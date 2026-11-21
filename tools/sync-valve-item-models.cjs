// Match inventory displays to actual block parts, preserving signed cuboid bounds.
const fs=require('fs'),path=require('path');
const {model}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'../src/main/resources/assets/ccd');
// The rear dial has the opposite handedness: mirror artwork, not signed geometry.
const dial=model('ccd:block/three_way_valve/dial_unpowered');
const mirrored=structuredClone(dial);
for(const e of mirrored.elements)for(const side of ['up','down']){
 const f=e.faces[side];if(!f)continue;
 const axis=((f.rotation||0)/90)%2===0?0:1;
 [f.uv[axis],f.uv[axis+2]]=[f.uv[axis+2],f.uv[axis]];
}
const mirroredPath=path.join(root,'models/block/three_way_valve/dial_mirrored.json');
if(process.argv.includes('--check')){
 if(JSON.stringify(JSON.parse(fs.readFileSync(mirroredPath,'utf8')))!==JSON.stringify(mirrored))throw Error('Mirrored dial mismatch');
}else fs.writeFileSync(mirroredPath,JSON.stringify(mirrored,null,2)+'\n');
const normals={east:[1,0,0],west:[-1,0,0],up:[0,1,0],down:[0,-1,0],south:[0,0,1],north:[0,0,-1]};
const vertices=(a,b,s)=>{const[x,y,z]=a,[X,Y,Z]=b;return{north:[[X,Y,z],[X,y,z],[x,y,z],[x,Y,z]],south:[[x,Y,Z],[x,y,Z],[X,y,Z],[X,Y,Z]],west:[[x,Y,z],[x,y,z],[x,y,Z],[x,Y,Z]],east:[[X,Y,Z],[X,y,Z],[X,y,z],[X,Y,z]],up:[[x,Y,z],[x,Y,Z],[X,Y,Z],[X,Y,z]],down:[[x,y,Z],[x,y,z],[X,y,z],[X,y,Z]]}[s];};
const eq=(a,b)=>a.every((v,i)=>Math.abs(v-b[i])<1e-6);
function transform(source,basis){
 const vector=v=>[0,1,2].map(i=>basis.reduce((s,col,j)=>s+col[i]*v[j],0));
 const point=v=>vector(v.map(n=>n-8)).map(n=>n+8);
 const e=structuredClone(source);delete e.rotation;e.faces={};e.from=[];e.to=[];
 for(let i=0;i<3;i++){
  const j=basis.findIndex(col=>col[i]!==0),sign=basis[j][i];
  e.from[i]=8+sign*((sign>0?source.from[j]:source.to[j])-8);
  e.to[i]=8+sign*((sign>0?source.to[j]:source.from[j])-8);
 }
 for(const[side,f]of Object.entries(source.faces)){
  const target=Object.keys(normals).find(k=>eq(normals[k],vector(normals[side])));
  const points=vertices(source.from,source.to,side).map(point),first=vertices(e.from,e.to,target)[0];
  const shift=points.findIndex(v=>eq(v,first));if(shift<0)throw Error('UV mismatch');
  e.faces[target]={...f,rotation:((f.rotation||0)+shift*90)%360};delete e.faces[target].cullface;
 }
 return e;
}
function resolveTexture(m,texture){for(let i=0;texture.startsWith('#')&&i<30;i++)texture=m.textures[texture.slice(1)];return texture;}
function append(elements,id,basis){const m=model(id);for(const source of m.elements){
 const e=basis?transform(source,basis):structuredClone(source);
 for(const f of Object.values(e.faces)){f.texture=resolveTexture(m,f.texture);delete f.cullface;}
 elements.push(e);
}}
function expected(name){const elements=[];
 if(name==='redstone_fluid_pump'){
  append(elements,'ccd:block/redstone_fluid_pump/block');append(elements,'ccd:block/redstone_fluid_pump/pointer');
  const old=model('ccd:item/'+name);
  for(const e of old.elements.filter(e=>e.name?.startsWith('inventory_shaft_'))){const copy=structuredClone(e);for(const f of Object.values(copy.faces))f.texture=resolveTexture(old,f.texture);elements.push(copy);}
 }else{
  for(const part of ['body','common_north','selected_south','closed_up'])append(elements,'ccd:block/three_way_valve/'+part);
  // SOUTH/roll=0 default: local -Z points south, local Y points to each free face.
   for(const [index,basis]of [[[0,1,0],[1,0,0],[0,0,-1]],[[0,-1,0],[-1,0,0],[0,0,-1]]].entries())
    for(const part of [index===0?'dial_mirrored':'dial_unpowered','pointer'])append(elements,'ccd:block/three_way_valve/'+part,basis);
 }
 return elements;
}
for(const name of ['three_way_valve','redstone_fluid_pump']){
 const p=path.join(root,'models/item/'+name+'.json'),elements=expected(name);
 const textures={particle:'create:block/copper_underside'};
 for(const e of elements)for(const f of Object.values(e.faces)){
  const address=f.texture;
  let key=Object.keys(textures).find(k=>textures[k]===address);
  if(!key){key='surface_'+Object.keys(textures).length;textures[key]=address;}
  f.texture='#'+key;
 }
 const result={parent:'create:block/block',render_type:'minecraft:cutout_mipped',ambientocclusion:false,textures,elements};
 if(process.argv.includes('--check')){
  const current=JSON.parse(fs.readFileSync(p,'utf8'));
  if(JSON.stringify(current)!==JSON.stringify(result))throw Error('Inventory mismatch '+name);
 }else fs.writeFileSync(p,JSON.stringify(result,null,2)+'\n');
 console.log(name+': inventory matches block body and resting dynamic parts');
}
