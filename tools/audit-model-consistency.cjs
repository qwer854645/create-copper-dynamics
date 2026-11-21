// Resolve every local model/blockstate, validate rotations, and assert pump state orientations.
const fs=require('fs'),path=require('path');
const {model}=require('./preview-fluid-packager.cjs');
const {turn,vectors}=require('./create-turning-booster-pump.cjs');
const root=path.resolve(__dirname,'../src/main/resources/assets/ccd');
const read=p=>JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));
const save=(p,d)=>fs.writeFileSync(path.join(root,p),JSON.stringify(d,null,2)+'\n');
function rotate(v,x,y){let [a,b,c]=v;let t=-x*Math.PI/180;[b,c]=[b*Math.cos(t)-c*Math.sin(t),b*Math.sin(t)+c*Math.cos(t)];t=-y*Math.PI/180;return[a*Math.cos(t)+c*Math.sin(t),b,-a*Math.sin(t)+c*Math.cos(t)];}
function basis(spec){return[[1,0,0],[0,1,0],[0,0,1]].map(v=>rotate(v,spec.x||0,spec.y||0));}
function vec(v,b){return[0,1,2].map(i=>Math.round(b.reduce((s,col,k)=>s+col[i]*v[k],0)));}
function clock(v,axis){const[x,y,z]=v;return axis===0?[x,z,-y]:axis===1?[-z,y,x]:[y,-x,z];}
const equal=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
if(process.argv.includes('--repair')){
 for(const name of ['turning_booster_pump','bidirectional_fluid_pump'])for(const suffix of ['','_reversed']){
  const source=read(`models/block/${name}${suffix}.json`);
  const b=[[0,1,0],[-1,0,0],[0,0,1]];
  save(`models/block/${name}${suffix}_cross.json`,{...source,elements:source.elements.map(e=>turn(e,b))});
 }
 const p='blockstates/redstone_fluid_pump.json',states=read(p);
 for(const[key,spec]of Object.entries(states.variants))if(key==='facing=up')spec.x=270;else if(key==='facing=down')spec.x=90;
 save(p,states);
 for(const name of ['turning_booster_pump','bidirectional_fluid_pump','redstone_fluid_pump']){
  const itemPath=`models/item/${name}.json`,item=read(itemPath);
  const source=model(name==='redstone_fluid_pump'?'ccd:block/redstone_fluid_pump/block':'ccd:block/'+name);
  const extras=item.elements.filter(e=>e.name?.startsWith('inventory_shaft_'));
  const pointer=name==='redstone_fluid_pump'?model('ccd:block/redstone_fluid_pump/pointer').elements:[];
  item.elements=[...source.elements,...pointer,...extras];item.textures={...item.textures,...source.textures};save(itemPath,item);
 }
 const itemPath='models/item/three_way_valve.json',item=read(itemPath);
 const dials=item.elements.filter(e=>e.name?.startsWith('dial_item_'));
 const parts=['body','common_north','selected_south','closed_up'].map(n=>model('ccd:block/three_way_valve/'+n));
 item.elements=[...parts.flatMap(m=>m.elements),...dials];
 for(const m of parts)Object.assign(item.textures,m.textures);
 save(itemPath,item);
}
let models=0,placements=0;
function walk(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(dir,e.name)):[path.join(dir,e.name)]);}
for(const p of walk(path.join(root,'models')).filter(p=>p.endsWith('.json'))){
 const raw=JSON.parse(fs.readFileSync(p));
 // NeoForge bucket loader models are not ordinary baked cuboid models.
 if(raw.loader||raw.parent?.startsWith('neoforge:'))continue;
 const id='ccd:'+path.relative(path.join(root,'models'),p).replaceAll('\\','/').slice(0,-5),m=model(id);models++;
 for(const e of m.elements||[]){
  if(e.rotation&&(!('angle'in e.rotation)||![-45,-22.5,0,22.5,45].includes(e.rotation.angle)))throw Error('Invalid element rotation '+id);
   for(const f of Object.values(e.faces)){
    if(!f.texture?.startsWith('#'))throw Error('Face texture must use #variable '+id);
   let tex=f.texture;for(let i=0;tex?.startsWith('#')&&i<30;i++)tex=m.textures[tex.slice(1)];
   if(!tex||tex.startsWith('#'))throw Error('Missing texture '+id);
  }
 }
}
for(const p of walk(path.join(root,'blockstates')).filter(p=>p.endsWith('.json'))){
 const s=JSON.parse(fs.readFileSync(p));const specs=s.variants?Object.values(s.variants):s.multipart.map(p=>p.apply);
 for(const entry of specs)for(const spec of Array.isArray(entry)?entry:[entry]){
  for(const axis of ['x','y'])if(spec[axis]!==undefined&&![0,90,180,270].includes(spec[axis]))throw Error('Invalid blockstate '+p);
  model(spec.model);placements++;
 }
}
for(const name of ['turning_booster_pump','bidirectional_fluid_pump']){
 const states=read(`blockstates/${name}.json`).variants;
 for(const[key,spec]of Object.entries(states)){
  const state=Object.fromEntries(key.split(',').map(v=>v.split('='))),front=vectors[state.facing],axis=front.findIndex(n=>n);
  let other=name==='turning_booster_pump'?(axis===1?vectors.north:vectors.up):(axis===2?vectors.east:vectors.south);
  for(let i=0;i<+state.roll;i++)other=clock(other,axis);
  const base=name==='turning_booster_pump'?[vectors.up,vectors.north]:[vectors.up,vectors.south];
  if(spec.model.endsWith('_cross'))for(let i=0;i<base.length;i++)base[i]=vec(base[i],[[0,1,0],[-1,0,0],[0,0,1]]);
  if(!equal(vec(base[0],basis(spec)),front)||!equal(vec(base[1],basis(spec)),other))throw Error('State orientation mismatch '+name+' '+key);
 }
 console.log(name+': 48 direction/reversal states aligned');
}
console.log(`${models} resolved models; ${placements} blockstate placements validated.`);
{
 const states=read('blockstates/vertical_turning_booster_pump.json').variants;
 for(const[key,spec]of Object.entries(states)){
  const state=Object.fromEntries(key.split(',').map(v=>v.split('='))),front=vectors[state.facing],axis=front.findIndex(n=>n);
  let branch=axis===1?vectors.north:vectors.up;
  for(let i=0;i<+state.roll;i++)branch=clock(branch,axis);
  let ports=[vectors.up,vectors.north],shaft=vectors.east;
  if(spec.model.endsWith('_cross')){const b=[[0,1,0],[-1,0,0],[0,0,1]];ports=ports.map(v=>vec(v,b));shaft=vec(shaft,b);}
  const b=basis(spec),normal=vec(shaft,b);
  if(!equal(vec(ports[0],b),front)||!equal(vec(ports[1],b),branch)||normal.reduce((s,n,i)=>s+n*front[i],0)!==0||normal.reduce((s,n,i)=>s+n*branch[i],0)!==0)
   throw Error('Vertical turning shaft/port mismatch '+key);
 }
 console.log('Vertical turning pump: 48 states keep shaft normal to both ports.');
}
for(const name of ['booster_pump','turning_booster_pump','bidirectional_fluid_pump','redstone_fluid_pump','buffer_pump']){
 const body=model(name==='redstone_fluid_pump'?'ccd:block/redstone_fluid_pump/block':'ccd:block/'+name);
 const item=model('ccd:item/'+name);
  const resolved=(elements,m)=>elements.map(e=>{
   const copy=structuredClone(e);
   for(const f of Object.values(copy.faces)){
    for(let i=0;f.texture.startsWith('#')&&i<30;i++)f.texture=m.textures[f.texture.slice(1)];
    delete f.cullface;
   }
   return copy;
  });
  if(!equal(resolved(item.elements.slice(0,body.elements.length),item),resolved(body.elements,body)))throw Error('Item body mismatch '+name);
}
console.log('Five pump item bodies match their block geometry and texture bindings.');
