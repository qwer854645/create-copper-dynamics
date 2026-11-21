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
  Object.assign(base.textures,{pump:'create:block/pump',pipe:'create:block/pipes',copper:'create:block/copper_underside'});
 const native=model('create:block/mechanical_pump/block');
 const tankElements=structuredClone(base.elements),headElements=[];
  for(const original of native.elements.filter(e=>e.from[1]>=8)){
  const e=structuredClone(original);e.name='native_upper_pump_end';
  for(const f of Object.values(e.faces))f.texture='#pump';base.elements.push(e);
  const inherited=structuredClone(original);headElements.push(inherited);
  }
  // Close the gap between the tank lid and upper pump end with a solid copper body.
  const housing={name:'copper_middle_housing',from:[2,10.6,2],to:[14,13,14],faces:{}};
  for(const side of ['north','east','south','west'])housing.faces[side]={uv:[2,3,14,11],texture:'#copper'};
  for(const side of ['up','down'])housing.faces[side]={uv:[2,2,14,14],texture:'#copper'};
  base.elements.unshift(housing);headElements.unshift(structuredClone(housing));
  const lower={...structuredClone(housing),name:'copper_lower_collar',from:[2,5,2],to:[14,5.4,14]};
  const hub={...structuredClone(housing),name:'copper_center_hub',from:[6.75,5.4,6.75],to:[9.25,10.6,9.25]};
  base.elements.unshift(lower,hub);headElements.unshift(structuredClone(lower),structuredClone(hub));
 // Native pump cog rotated from Z to Y, compressed into the upper two thirds.
 const cog=model('create:block/mechanical_pump/cog'),cogElements=[];
 for(const original of cog.elements){
  const e=turn({...original,rotation:undefined},[[1,0,0],[0,0,-1],[0,1,0]]);
  e.name='native_pump_cog';
  for(const p of [e.from,e.to])for(let i=0;i<3;i++)p[i]=8+(p[i]-8)*(i===1?.5:1);
  if(original.rotation?.angle)e.rotation={angle:original.rotation.angle,axis:'y',origin:[8,8,8]};
  for(const f of Object.values(e.faces)){f.texture='#pump';delete f.cullface;}cogElements.push(e);
 }
 for(const side of ['up'])for(const part of ['rim_connector','rim'])for(const original of model(`create:block/fluid_pipe/${part}/${side}`).elements){
  const e=structuredClone(original);e.name='native_pump_'+part+'_'+side;
  for(const f of Object.values(e.faces)){f.texture='#pipe';delete f.cullface;}base.elements.push(e);
 }
 save('assets/ccd/models/block/buffer_pump.json',base);
 save('assets/ccd/models/block/buffer_pump/tank.json',{...base,elements:tankElements});
  save('assets/ccd/models/block/buffer_pump/pump_head.json',{parent:'create:block/mechanical_pump/block',textures:{copper:'create:block/copper_underside'},elements:headElements});
 const mouthElements=base.elements.filter(e=>e.name?.startsWith('native_pump_rim'));
 save('assets/ccd/models/block/buffer_pump/mouth.json',{parent:'minecraft:block/block',textures:{pipe:'create:block/pipes'},elements:mouthElements});
 save('assets/ccd/models/block/buffer_pump/cog.json',{parent:'minecraft:block/block',textures:{pump:'create:block/pump'},elements:cogElements});
 const shaft=model('create:block/shaft_half'),shafts=[];
 for(const basis of [[[0,0,-1],[0,1,0],[1,0,0]],[[0,0,1],[0,1,0],[-1,0,0]]])for(const source of shaft.elements){const e=turn(source,basis);for(const f of Object.values(e.faces))f.texture=f.texture==='#0'?'#shaft_side':'#shaft_end';shafts.push(e);}
 save('assets/ccd/models/item/buffer_pump.json',{...base,elements:[...base.elements,...cogElements]});
 const multipart=[];for(const[facing,rotation]of Object.entries({up:{},down:{x:180},north:{x:90},south:{x:270},east:{x:90,y:90},west:{x:90,y:270}}))
  for(const part of ['tank','pump_head','mouth'])multipart.push({when:{facing},apply:{model:'ccd:block/buffer_pump/'+part,...rotation}});
 save('assets/ccd/blockstates/buffer_pump.json',{multipart});
 save('data/ccd/recipe/buffer_pump.json',{type:'minecraft:crafting_shapeless',ingredients:[{item:'create:mechanical_pump'},{item:'create:fluid_tank'}],result:{id:'ccd:buffer_pump',count:1}});
 save('data/ccd/loot_table/blocks/buffer_pump.json',{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:'ccd:buffer_pump'}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
 for(const rel of ['data/create/tags/block/wrench_pickup.json','data/minecraft/tags/block/mineable/pickaxe.json']){const data=JSON.parse(fs.readFileSync(path.join(root,rel)));if(!data.values.includes('ccd:buffer_pump'))data.values.push('ccd:buffer_pump');save(rel,data);}
 for(const locale of ['zh_cn','en_us']){const rel=`assets/ccd/lang/${locale}.json`,data=JSON.parse(fs.readFileSync(path.join(root,rel),'utf8'));data['block.ccd.buffer_pump']=locale==='zh_cn'?'缓存泵':'Buffer Pump';save(rel,data);}
 fs.mkdirSync('outputs/buffer-pump',{recursive:true});const full=await mesh('ccd:item/buffer_pump');
 const panels=[];for(const[i,view]of [[0,[26,24,-34]],[1,[-26,24,34]]])panels.push({input:await render(full,550,520,view,{center:[8,8,8],scale:24}),left:i*550,top:0});
 await sharp({create:{width:1100,height:520,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile('outputs/buffer-pump/preview.png');
}main().catch(e=>{console.error(e);process.exitCode=1;});
