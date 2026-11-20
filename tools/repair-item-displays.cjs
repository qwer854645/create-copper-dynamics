// Build item displays from the exact current block and moving-part resources.
const fs=require('fs'),path=require('path'),sharp=require('sharp');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const {turn}=require('./create-turning-booster-pump.cjs');
const root=path.resolve(__dirname,'../src/main/resources/assets/ccd');
const save=(name,data)=>fs.writeFileSync(path.join(root,'models/item/'+name+'.json'),JSON.stringify(data,null,2)+'\n');
const x90=[[1,0,0],[0,0,-1],[0,1,0]];
function combine(parts){
 const elements=[],textures={};
 for(const[index,{id,basis} ]of parts.entries()){
  const m=model(id),prefix='part'+index+'_';
  for(const[k,v]of Object.entries(m.textures||{}))textures[prefix+k]=v.startsWith('#')?'#'+prefix+v.slice(1):v;
  for(const source of m.elements){const e=basis?turn(source,basis):structuredClone(source);
   for(const f of Object.values(e.faces)){if(f.texture.startsWith('#'))f.texture='#'+prefix+f.texture.slice(1);delete f.cullface;}
   elements.push(e);
  }
 }
 return {parent:'create:block/block',render_type:'minecraft:cutout_mipped',ambientocclusion:false,textures,elements};
}
async function main(){
 save('fluid_atomizer',combine([{id:'ccd:block/fluid_atomizer',basis:x90},{id:'ccd:block/fluid_atomizer/propeller'},{id:'create:block/shaft_half'}]));
 const valve=combine([{id:'ccd:block/redstone_fluid_valve/block_horizontal_open'},{id:'ccd:block/redstone_fluid_valve/dial_open'}]);
 // A single static pointer pair matches the runtime unpowered/open position.
 const pointer=model('create:block/fluid_valve/pointer');
 for(const[k,v]of Object.entries(pointer.textures||{}))valve.textures['pointer_'+k]=v;
 for(const source of pointer.elements){const e=turn(source,[[0,0,1],[0,1,0],[-1,0,0]]);
  for(const coord of ['from','to'])e[coord][1]=8+(e[coord][1]-8)*(6.02/5.5);
  for(const f of Object.values(e.faces))if(f.texture.startsWith('#'))f.texture='#pointer_'+f.texture.slice(1);
  valve.elements.push(e);
 }
 save('redstone_fluid_valve',valve);
 const out='outputs/item-display-repair';fs.mkdirSync(out,{recursive:true});
 const panels=[];
 for(const[i,id]of ['redstone_fluid_valve','fluid_atomizer'].entries())panels.push({input:await render(await mesh('ccd:item/'+id),550,520,[26,24,-34],{center:[8,8,8],scale:24}),left:i*550,top:0});
 await sharp({create:{width:1100,height:520,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile(out+'/preview.png');
}main().catch(e=>{console.error(e);process.exitCode=1;});
