// Bellows pump using existing Create materials; no textures are generated.
const fs=require('fs'),path=require('path'),sharp=require('sharp');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const {turn}=require('./create-turning-booster-pump.cjs');
const root=path.resolve(__dirname,'../src/main/resources');
function save(rel,data){const p=path.join(root,rel);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(data,null,2)+'\n');}
const textures={copper:'create:block/copper_casing',rubber:'create:block/belt',brass:'create:block/brass_block',shaft:'ccd:block/booster_pump/shaft_face',pipe:'create:block/pipes'};
function box(name,from,to,texture){return {name,from,to,faces:Object.fromEntries(['north','south','east','west','up','down'].map(s=>[s,{texture:'#'+texture,uv:[0,0,16,16]}]))};}
const wrap=elements=>({parent:'minecraft:block/block',textures:{...textures,particle:textures.copper},elements});
async function main(){
 const base=[box('copper_base',[2,2,2],[14,10,14],'copper'),box('foot',[1,0,1],[15,2,15],'copper')];
 for(const side of ['east','west'])base[0].faces[side]={texture:'#shaft',uv:[2,6,14,14]};
 for(const side of ['north','south'])for(const kind of ['rim_connector','rim'])for(const original of model(`create:block/fluid_pipe/${kind}/${side}`).elements){
  const e=structuredClone(original);e.name='native_'+kind+'_'+side;
  for(const f of Object.values(e.faces)){f.texture='#pipe';delete f.cullface;}base.push(e);
 }
 const bellows=[box('dark_bellows_core',[3,10,3],[13,14,13],'rubber')];
 for(let i=0;i<4;i++)bellows.push(box('bellows_fold_'+i,[2,10+i,2],[14,10.5+i,14],'rubber'));
 const plate=[box('brass_pressure_plate',[2,14,2],[14,15,14],'brass')];
 const prefix='assets/ccd/models/block/reciprocating_fluid_pump';
 save(prefix+'/body.json',wrap(base));save(prefix+'/bellows.json',wrap(bellows));save(prefix+'/plate.json',wrap(plate));
 const shaft=model('create:block/shaft_half');
 const halves=[];
 for(const basis of [[[0,0,-1],[0,1,0],[1,0,0]],[[0,0,1],[0,1,0],[-1,0,0]]])for(const element of shaft.elements){
  const e=turn(element,basis);for(const f of Object.values(e.faces))f.texture=f.texture==='#0'?'#shaft_side':'#shaft_end';halves.push(e);
 }
 save('assets/ccd/models/item/reciprocating_fluid_pump.json',{...wrap([...base,...bellows,...plate,...halves]),parent:'create:block/block',textures:{...textures,shaft_side:shaft.textures['0'],shaft_end:shaft.textures['1']}});
 const multipart=[];
 for(const[facing,y]of Object.entries({north:0,east:90,south:180,west:270,up:0,down:0}))multipart.push({when:{facing},apply:{model:'ccd:block/reciprocating_fluid_pump/body',...(y?{y}:{})}});
 save('assets/ccd/blockstates/reciprocating_fluid_pump.json',{multipart});
 save('data/ccd/recipe/reciprocating_fluid_pump.json',{type:'minecraft:crafting_shapeless',ingredients:[{item:'create:mechanical_pump'},{item:'create:copper_casing'},{item:'create:belt_connector'},{item:'create:electron_tube'}],result:{id:'ccd:reciprocating_fluid_pump',count:1}});
 save('data/ccd/loot_table/blocks/reciprocating_fluid_pump.json',{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:'ccd:reciprocating_fluid_pump'}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
 for(const rel of ['data/create/tags/block/wrench_pickup.json','data/minecraft/tags/block/mineable/pickaxe.json']){
  const data=JSON.parse(fs.readFileSync(path.join(root,rel),'utf8'));if(!data.values.includes('ccd:reciprocating_fluid_pump'))data.values.push('ccd:reciprocating_fluid_pump');save(rel,data);
 }
 for(const locale of ['zh_cn','en_us']){
  const rel=`assets/ccd/lang/${locale}.json`,data=JSON.parse(fs.readFileSync(path.join(root,rel),'utf8'));
  data['block.ccd.reciprocating_fluid_pump']=locale==='zh_cn'?'复进流体泵':'Reciprocating Fluid Pump';
  data['ccd.reciprocating.flow']=locale==='zh_cn'?'目标流速：%s mB/t':'Target flow: %s mB/t';
  data['ccd.reciprocating.signal']=locale==='zh_cn'?'红石强度：%s / 15':'Redstone strength: %s / 15';
  data['block.ccd.reciprocating_fluid_pump.tooltip.summary']=locale==='zh_cn'?'使用旋转动力泵送流体，_红石信号强度_按比例设定流量。':'Pumps fluids using rotational power. _Redstone Signal Strength_ proportionally sets the flow rate.';
  save(rel,data);
 }
 const output='outputs/reciprocating-fluid-pump';fs.mkdirSync(output,{recursive:true});
 const full=await mesh('ccd:item/reciprocating_fluid_pump');
 await sharp(await render(full,700,650,[26,26,-34],{center:[8,8,8],scale:28})).png().toFile(output+'/preview.png');
}main().catch(e=>{console.error(e);process.exitCode=1;});
