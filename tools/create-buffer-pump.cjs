// Native tank compressed to the bottom third; compact pump housing above it.
const fs=require('fs'),path=require('path'),sharp=require('sharp');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const {turn}=require('./create-turning-booster-pump.cjs');
const root=path.resolve(__dirname,'../src/main/resources');
function save(rel,data){const p=path.join(root,rel);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(data,null,2)+'\n');}
async function main(){
 const tank=model('create:block/fluid_tank/block_single_window'),base=structuredClone(tank);
 base.parent='create:block/block';base.render_type='minecraft:cutout_mipped';
 base.elements=base.elements.map(e=>{const v=structuredClone(e);v.name='compressed_tank';v.from[1]*=5/16;v.to[1]*=5/16;if(v.rotation)v.rotation.origin[1]*=5/16;for(const f of Object.values(v.faces))delete f.cullface;return v;});
 Object.assign(base.textures,{housing:'create:block/copper_underside',shaft:'ccd:block/booster_pump/shaft_face',pipe:'create:block/pipes'});
 const faces=Object.fromEntries(['north','south','east','west','up','down'].map(side=>[side,{texture:['east','west'].includes(side)?'#shaft':'#housing',uv:[2,2,14,11]}]));
 base.elements.push({name:'upper_half_pump',from:[2,5,2],to:[14,14,14],faces});
 for(const side of ['north','south'])for(const part of ['rim_connector','rim'])for(const original of model(`create:block/fluid_pipe/${part}/${side}`).elements){
  const e=structuredClone(original);e.name='native_pump_'+part+'_'+side;
  for(const f of Object.values(e.faces)){f.texture='#pipe';delete f.cullface;}base.elements.push(e);
 }
 save('assets/ccd/models/block/buffer_pump.json',base);
 const shaft=model('create:block/shaft_half'),shafts=[];
 for(const basis of [[[0,0,-1],[0,1,0],[1,0,0]],[[0,0,1],[0,1,0],[-1,0,0]]])for(const source of shaft.elements){const e=turn(source,basis);for(const f of Object.values(e.faces))f.texture=f.texture==='#0'?'#shaft_side':'#shaft_end';shafts.push(e);}
 save('assets/ccd/models/item/buffer_pump.json',{...base,textures:{...base.textures,shaft_side:shaft.textures['0'],shaft_end:shaft.textures['1']},elements:[...base.elements,...shafts]});
 const variants={};for(const[facing,y]of Object.entries({north:0,east:90,south:180,west:270,up:0,down:0}))variants['facing='+facing]={model:'ccd:block/buffer_pump',...(y?{y}:{})};
 save('assets/ccd/blockstates/buffer_pump.json',{variants});
 save('data/ccd/recipe/buffer_pump.json',{type:'minecraft:crafting_shapeless',ingredients:[{item:'create:mechanical_pump'},{item:'create:fluid_tank'}],result:{id:'ccd:buffer_pump',count:1}});
 save('data/ccd/loot_table/blocks/buffer_pump.json',{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:'ccd:buffer_pump'}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
 for(const rel of ['data/create/tags/block/wrench_pickup.json','data/minecraft/tags/block/mineable/pickaxe.json']){const data=JSON.parse(fs.readFileSync(path.join(root,rel)));if(!data.values.includes('ccd:buffer_pump'))data.values.push('ccd:buffer_pump');save(rel,data);}
 for(const locale of ['zh_cn','en_us']){const rel=`assets/ccd/lang/${locale}.json`,data=JSON.parse(fs.readFileSync(path.join(root,rel),'utf8'));data['block.ccd.buffer_pump']=locale==='zh_cn'?'缓存泵':'Buffer Pump';save(rel,data);}
 fs.mkdirSync('outputs/buffer-pump',{recursive:true});const full=await mesh('ccd:item/buffer_pump');
 const panels=[];for(const[i,view]of [[0,[26,24,-34]],[1,[-26,24,34]]])panels.push({input:await render(full,550,520,view,{center:[8,8,8],scale:24}),left:i*550,top:0});
 await sharp({create:{width:1100,height:520,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile('outputs/buffer-pump/preview.png');
}main().catch(e=>{console.error(e);process.exitCode=1;});
