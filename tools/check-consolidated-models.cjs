const fs=require('fs'),path=require('path');
const {turn}=require('./create-turning-booster-pump.cjs');
const root=path.resolve(__dirname,'../src/main/resources/assets/ccd');
const read=p=>JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));
const oldRead=p=>JSON.parse(require('child_process').execFileSync('C:/Users/Administrator/AppData/Local/GitHubDesktop/app-3.6.2/resources/app/git/cmd/git.exe',
 ['show','a857d40:src/main/resources/assets/ccd/'+p],{encoding:'utf8'}));
function rotate(v,axis,degrees){const a=degrees*Math.PI/180,c=Math.cos(a),s=Math.sin(a),[x,y,z]=v;return axis==='x'?[x,y*c-z*s,y*s+z*c]:[x*c+z*s,y,-x*s+z*c];}
function normalize(elements){return elements.map(e=>({from:e.from.map(n=>Math.round(n*1e5)),to:e.to.map(n=>Math.round(n*1e5)),faces:Object.keys(e.faces).sort().map(side=>[side,e.faces[side].texture,e.faces[side].uv, (e.faces[side].rotation||0)%360])}));}
let checked=0;
for(const name of ['turning_booster_pump','bidirectional_fluid_pump']){
 for(const [key,spec]of Object.entries(read('blockstates/'+name+'.json').variants)){
  const state=Object.fromEntries(key.split(',').map(p=>p.split('=')));
  const old=oldRead(`models/block/${name}/${state.facing}_${state.roll}${state.reversed==='true'?'_reversed':''}.json`);
  const base=read('models/'+spec.model.slice(4)+'.json');
  const basis=[[1,0,0],[0,1,0],[0,0,1]].map(v=>rotate(rotate(v,'x',-(spec.x||0)),'y',-(spec.y||0)));
  const actual=base.elements.map(e=>turn(e,basis));
  if(JSON.stringify(normalize(actual))!==JSON.stringify(normalize(old.elements)))throw Error('Geometry/UV mismatch '+name+' '+key);
  checked++;
 }
}
console.log('Exact geometry and UV match for '+checked+' pump states.');
const oldStates=oldRead('blockstates/three_way_valve.json').multipart.filter(p=>p.apply.model.includes('/dial_'));
const newStates=read('blockstates/three_way_valve.json').multipart.filter(p=>p.apply.model.includes('/dial_'));
for(let i=0;i<oldStates.length;i++){
 const old=oldRead('models/'+oldStates[i].apply.model.slice(4)+'.json');
 const spec=newStates[i].apply,base=read('models/'+spec.model.slice(4)+'.json');
 const basis=[[1,0,0],[0,1,0],[0,0,1]].map(v=>rotate(rotate(v,'x',-(spec.x||0)),'y',-(spec.y||0)));
 if(JSON.stringify(normalize(base.elements.map(e=>turn(e,basis))))!==JSON.stringify(normalize(old.elements)))throw Error('Dial mismatch '+i);
}
console.log('Exact geometry and UV match for '+oldStates.length+' dial placements.');
