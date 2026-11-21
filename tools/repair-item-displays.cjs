// Build item displays from the exact current block and moving-part resources.
const fs=require('fs'),path=require('path'),sharp=require('sharp');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const {turn}=require('./create-turning-booster-pump.cjs');
const root=path.resolve(__dirname,'../src/main/resources/assets/ccd');
const save=(name,data)=>fs.writeFileSync(path.join(root,'models/item/'+name+'.json'),JSON.stringify(data,null,2)+'\n');
const x90=[[1,0,0],[0,0,-1],[0,1,0]];
function rotateAtomizerBody(source){
 const e=structuredClone(source),[x,y,z]=source.from,[X,Y,Z]=source.to;
 // Preserve signed bounds: the imported inside-out cuboid provides inward faces.
 e.from=[x,z,16-Y];e.to=[X,Z,16-y];e.faces={};
 const corners=(a,b,s)=>{const[x,y,z]=a,[X,Y,Z]=b;return {
  north:[[X,Y,z],[X,y,z],[x,y,z],[x,Y,z]],south:[[x,Y,Z],[x,y,Z],[X,y,Z],[X,Y,Z]],
  west:[[x,Y,z],[x,y,z],[x,y,Z],[x,Y,Z]],east:[[X,Y,Z],[X,y,Z],[X,y,z],[X,Y,z]],
  up:[[x,Y,z],[x,Y,Z],[X,Y,Z],[X,Y,z]],down:[[x,y,Z],[x,y,z],[X,y,z],[X,y,Z]]}[s];};
 const sides={north:'down',south:'up',up:'north',down:'south',east:'east',west:'west'};
 for(const[side,face]of Object.entries(source.faces)){
  const target=sides[side],points=corners(source.from,source.to,side).map(([x,y,z])=>[x,z,16-y]);
  const first=corners(e.from,e.to,target)[0];
  const shift=points.findIndex(p=>p.every((v,i)=>Math.abs(v-first[i])<1e-5));
  if(shift<0)throw Error('Atomizer signed UV mismatch');
  e.faces[target]={...face,rotation:((face.rotation||0)+shift*90)%360};
 }
 delete e.rotation;return e;
}
function combine(parts){
 const elements=[],textures={};
 for(const[index,{id,basis} ]of parts.entries()){
  const m=model(id),prefix='part'+index+'_';
  for(const[k,v]of Object.entries(m.textures||{}))textures[prefix+k]=v.startsWith('#')?'#'+prefix+v.slice(1):v;
  for(const source of m.elements){const e=basis?rotateAtomizerBody(source):structuredClone(source);
   for(const f of Object.values(e.faces)){if(f.texture.startsWith('#'))f.texture='#'+prefix+f.texture.slice(1);delete f.cullface;}
   elements.push(e);
  }
 }
 return {parent:'create:block/block',render_type:'minecraft:cutout_mipped',ambientocclusion:false,textures,elements};
}
async function main(){
 const atomizer=combine([{id:'ccd:block/fluid_atomizer',basis:x90},{id:'ccd:block/fluid_atomizer/propeller'},{id:'create:block/shaft_half'}]);
 atomizer.display={gui:{rotation:[30,225,0],translation:[0,0,0],scale:[.625,.625,.625]}};
 save('fluid_atomizer',atomizer);
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
