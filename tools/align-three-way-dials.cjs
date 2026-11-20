// Orient native dial artwork from the actual straight/branch axes, without editing textures.
const fs=require('fs'),path=require('path');
const {turn,cross,vectors}=require('./create-turning-booster-pump.cjs');
const root=path.resolve(__dirname,'../src/main/resources/assets/ccd');
const save=(p,d)=>fs.writeFileSync(path.join(root,p),JSON.stringify(d,null,2)+'\n');
const states=JSON.parse(fs.readFileSync(path.join(root,'blockstates/three_way_valve.json')));
states.multipart=states.multipart.filter(p=>!p.apply.model.includes('/dial_'));
const base=JSON.parse(fs.readFileSync(path.join(root,'models/block/three_way_valve/dial_unpowered.json')));
for(const [facing,front]of Object.entries(vectors)){
 const axis=front.findIndex(n=>n),z=front.map(n=>-n);
 let branch=axis===1?vectors.north:vectors.up;
 for(let roll=0;roll<4;roll++){
  const normal=cross(front,branch);
  for(const [index,y]of [normal,normal.map(n=>-n)].entries()){
   const basis=[cross(y,z),y,z],id=`three_way_valve/dial_${facing}_${roll}_${index}`;
   save(`models/block/${id}.json`,{...base,elements:base.elements.map(e=>turn(e,basis))});
   states.multipart.push({when:{facing,roll:String(roll)},apply:{model:'ccd:block/'+id}});
  }
  // Keep physical branch rims aligned with the runtime ROLL convention too.
  const branchName=Object.keys(vectors).find(k=>vectors[k].every((n,i)=>n===branch[i]));
  for(const p of states.multipart)if(p.when?.facing===facing&&p.when?.roll===String(roll)&&p.apply.model.includes('/closed_'))
   p.apply.model='ccd:block/three_way_valve/closed_'+branchName;
  const[x,y,bz]=branch;branch=axis===0?[x,bz,-y]:axis===1?[-bz,y,x]:[y,-x,bz];
 }
}
save('blockstates/three_way_valve.json',states);
