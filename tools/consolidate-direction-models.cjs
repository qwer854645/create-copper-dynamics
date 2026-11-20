// Consolidate existing artwork losslessly. Never write textures.
const fs=require('fs'),path=require('path');
const {turn,cross,vectors}=require('./create-turning-booster-pump.cjs');
const root=path.resolve(__dirname,'../src/main/resources/assets/ccd');
const read=p=>JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));
const save=(p,d)=>fs.writeFileSync(path.join(root,p),JSON.stringify(d,null,2)+'\n');
const eq=(a,b)=>a.every((n,i)=>Math.abs(n-b[i])<1e-5);
function rotate(v,axis,degrees){
 const a=degrees*Math.PI/180,c=Math.cos(a),s=Math.sin(a),[x,y,z]=v;
 return axis==='x'?[x,y*c-z*s,y*s+z*c]:[x*c+z*s,y,-x*s+z*c];
}
const rotations=[];
for(const x of [0,90,180,270])for(const y of [0,90,180,270]){
 const basis=[[1,0,0],[0,1,0],[0,0,1]].map(v=>rotate(rotate(v,'x',-x),'y',-y));
 rotations.push({basis,...(x?{x}:{}),...(y?{y}:{})});
}
function placement(base,target,model){
 const found=rotations.find(r=>base.every((v,j)=>{
  const actual=[0,1,2].map(i=>r.basis.reduce((sum,col,k)=>sum+col[i]*v[k],0));
  return eq(actual,target[j]);
 }));
 if(!found){
  const extra=[[0,1,0],[-1,0,0],[0,0,1]];
  const transformed=base.map(v=>[0,1,2].map(i=>extra.reduce((sum,col,k)=>sum+col[i]*v[k],0)));
  const alternative=rotations.find(r=>transformed.every((v,j)=>eq([0,1,2].map(i=>r.basis.reduce((sum,col,k)=>sum+col[i]*v[k],0)),target[j])));
  if(!alternative)throw Error('No native rotation: '+model);
  const source=read('models/block/'+model+'.json');
  save('models/block/'+model+'_cross.json',{...source,elements:source.elements.map(e=>turn(e,extra))});
  const {basis,...rotation}=alternative;return {model:'ccd:block/'+model+'_cross',...rotation};
 }
 const {basis,...rotation}=found;return {model:'ccd:block/'+model,...rotation};
}
function clock(v,axis){const[x,y,z]=v;return axis===0?[x,z,-y]:axis===1?[-z,y,x]:[y,-x,z];}
for(const name of ['turning_booster_pump','bidirectional_fluid_pump']){
 const canonical=read(fs.existsSync(path.join(root,`models/block/${name}/up_0.json`))?`models/block/${name}/up_0.json`:`models/block/${name}.json`);
 const reversed=read(fs.existsSync(path.join(root,`models/block/${name}/up_0_reversed.json`))?`models/block/${name}/up_0_reversed.json`:`models/block/${name}_reversed.json`);
 save(`models/block/${name}.json`,canonical);
 save(`models/block/${name}_reversed.json`,reversed);
 const variants={};
 const base=name==='turning_booster_pump'?[vectors.up,vectors.north]:[vectors.up,vectors.south];
 for(const [facing,front]of Object.entries(vectors)){
  const axis=front.findIndex(n=>n);
  let secondary=name==='turning_booster_pump'?(axis===1?vectors.north:vectors.up):(axis===2?vectors.east:vectors.south);
  for(let roll=0;roll<4;roll++){
   for(const reverse of [false,true])variants[`facing=${facing},roll=${roll},reversed=${reverse}`]=placement(base,[front,secondary],name+(reverse?'_reversed':''));
   secondary=clock(secondary,axis);
  }
 }
 save(`blockstates/${name}.json`,{variants});
}
const states=read('blockstates/three_way_valve.json');
const dialBase=[vectors.east,vectors.up,vectors.south];
for(const part of states.multipart){
 if(!part.apply.model.includes('/dial_'))continue;
 const front=vectors[part.when.facing],axis=front.findIndex(n=>n);let branch=axis===1?vectors.north:vectors.up;
 for(let i=0;i<Number(part.when.roll);i++)branch=clock(branch,axis);
 const peers=states.multipart.filter(p=>p.when?.facing===part.when.facing&&p.when?.roll===part.when.roll&&p.apply.model.includes('/dial_'));
 let normal=cross(front,branch);if(peers.indexOf(part)===1)normal=normal.map(n=>-n);
 const z=front.map(n=>-n),basis=[cross(normal,z),normal,z];
 part.apply=placement(dialBase,basis,'three_way_valve/dial_unpowered');
}
save('blockstates/three_way_valve.json',states);
// Replace byte-for-byte equivalent item geometry with parent inheritance.
let inherited=0;
for(const file of fs.readdirSync(path.join(root,'models/item'))){
 if(!file.endsWith('.json'))continue;
 const rel='models/item/'+file,item=read(rel);
 if(!item.elements||!item.parent?.startsWith('ccd:block/'))continue;
 let parentPath='models/'+item.parent.slice(4)+'.json';
 if(!fs.existsSync(path.join(root,parentPath)))continue;
 const parent=read(parentPath);
 if(JSON.stringify(item.elements)!==JSON.stringify(parent.elements))continue;
 delete item.elements;
 if(item.textures)for(const key of Object.keys(item.textures))if(item.textures[key]===parent.textures?.[key])delete item.textures[key];
 if(item.textures&&!Object.keys(item.textures).length)delete item.textures;
 save(rel,item);inherited++;
}
console.log('Consolidated 96 pump direction models and 48 dial direction models; item inheritance:',inherited);
for(const directory of ['turning_booster_pump','bidirectional_fluid_pump','three_way_valve']){
 const dir=path.join(root,'models/block',directory);
 for(const file of fs.readdirSync(dir))if(/^(north|south|east|west|up|down)_[0-3](_reversed)?\.json$/.test(file)
  ||/^dial_(north|south|east|west|up|down)_[0-3]_[01]\.json$/.test(file))fs.unlinkSync(path.join(dir,file));
}
