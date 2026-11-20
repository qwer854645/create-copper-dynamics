// Reuse existing artwork without writing any texture. Bake the 24 port orientations.
const fs=require('node:fs'),path=require('node:path');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const sharp=require('sharp');
const root=path.resolve(__dirname,'..'),resources=path.join(root,'src/main/resources');
const save=(rel,data)=>{const p=path.join(resources,rel);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(data,null,2)+'\n');};
const vectors={east:[1,0,0],west:[-1,0,0],up:[0,1,0],down:[0,-1,0],south:[0,0,1],north:[0,0,-1]};
const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
const equal=(a,b)=>a.every((v,i)=>Math.abs(v-b[i])<1e-5);
const name=v=>Object.keys(vectors).find(k=>equal(vectors[k],v));
const vertices=(a,b,s)=>{const[x,y,z]=a,[X,Y,Z]=b;return {
 north:[[X,Y,z],[X,y,z],[x,y,z],[x,Y,z]],south:[[x,Y,Z],[x,y,Z],[X,y,Z],[X,Y,Z]],
 west:[[x,Y,z],[x,y,z],[x,y,Z],[x,Y,Z]],east:[[X,Y,Z],[X,y,Z],[X,y,z],[X,Y,z]],
 up:[[x,Y,z],[x,Y,Z],[X,Y,Z],[X,Y,z]],down:[[x,y,Z],[x,y,z],[X,y,z],[X,y,Z]]}[s];};
function turn(element,basis){
 const vector=v=>[0,1,2].map(i=>basis.reduce((sum,col,j)=>sum+col[i]*v[j],0));
 const point=p=>vector(p.map(n=>n-8)).map(n=>n+8);
 const ends=[point(element.from),point(element.to)],out={...structuredClone(element),faces:{}};
 out.from=[0,1,2].map(i=>Math.min(...ends.map(p=>p[i])));out.to=[0,1,2].map(i=>Math.max(...ends.map(p=>p[i])));
 for(const [side,face]of Object.entries(element.faces)){
  const target=name(vector(vectors[side])),points=vertices(element.from,element.to,side).map(point);
  const first=vertices(out.from,out.to,target)[0],shift=points.findIndex(p=>equal(p,first));
  if(shift<0)throw Error('UV corner mismatch');
  out.faces[target]={...face,rotation:((face.rotation||0)+shift*90)%360};
  if(face.cullface)out.faces[target].cullface=name(vector(vectors[face.cullface]));
 }
 return out;
}
async function main(){
 const base=model('ccd:block/booster_pump'),housing=structuredClone(base.elements[0]);
 for(const side of Object.keys(housing.faces))housing.faces[side].texture=['south','down'].includes(side)?'#shaft_face':'#body';
 const elements=[housing];
 for(const side of ['up','north'])for(const part of ['rim_connector','rim']){
  for(const original of model(`create:block/fluid_pipe/${part}/${side}`).elements){
   const e=structuredClone(original);e.name=`native_${part}_${side}`;
   for(const face of Object.values(e.faces)){face.texture='#pipe';delete face.cullface;}
   elements.push(e);
  }
 }
 for(const original of base.elements.filter(e=>/^native_pump_mark_.*_(east|west)$/.test(e.name))){
  elements.push(original.name.includes('inlet')?turn(original,[[1,0,0],[0,0,1],[0,-1,0]]):structuredClone(original));
 }
 const shaft=model('create:block/shaft_half');
 const nativeShaft=shaft.elements.map(e=>{const q=structuredClone(e);for(const f of Object.values(q.faces))f.texture=f.texture==='#0'?'#shaft_side':'#shaft_end';return q;});
 // Native half shaft faces south. Second shaft rotates south to down.
 const itemShafts=[...nativeShaft,...nativeShaft.map(e=>turn(e,[[1,0,0],[0,0,1],[0,-1,0]]))];
 const variants={};
 for(const [facing,front]of Object.entries(vectors)){
  let branch=['up','down'].includes(facing)?vectors.north:vectors.up;
  for(let roll=0;roll<4;roll++){
   const z=branch.map(v=>-v),basis=[cross(front,z),front,z],id=`turning_booster_pump/${facing}_${roll}`;
   for(const reversed of [false,true]){
    const oriented=elements.map(e=>turn(e,basis));
    if(reversed)for(const e of oriented.filter(e=>e.name.startsWith('native_pump_mark_')))
     for(const face of Object.values(e.faces))face.rotation=((face.rotation||0)+180)%360;
    const stateId=id+(reversed?'_reversed':'');
    save(`assets/ccd/models/block/${stateId}.json`,{...base,elements:oriented});
    variants[`facing=${facing},roll=${roll},reversed=${reversed}`]={model:`ccd:block/${stateId}`};
   }
   // Direction.getClockWise rotates about the positive coordinate axis.
   const axis=front.findIndex(v=>v!==0),[x,y,z0]=branch;
   branch=axis===0?[x,-z0,y]:axis===1?[z0,y,-x]:[-y,x,z0];
  }
 }
 save('assets/ccd/blockstates/turning_booster_pump.json',{variants});
 save('assets/ccd/models/item/turning_booster_pump.json',{...base,textures:{...base.textures,shaft_side:shaft.textures['0'],shaft_end:shaft.textures['1']},elements:[...elements,...itemShafts]});
 save('data/ccd/recipe/turning_booster_pump.json',{type:'minecraft:crafting_shapeless',category:'redstone',ingredients:[{item:'ccd:booster_pump'}],result:{id:'ccd:turning_booster_pump',count:1}});
 save('data/ccd/recipe/booster_pump_from_turning.json',{type:'minecraft:crafting_shapeless',category:'redstone',ingredients:[{item:'ccd:turning_booster_pump'}],result:{id:'ccd:booster_pump',count:1}});
 save('data/ccd/loot_table/blocks/turning_booster_pump.json',{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:'ccd:turning_booster_pump'}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
 for(const tag of ['data/create/tags/block/wrench_pickup.json','data/minecraft/tags/block/mineable/pickaxe.json']){
  const data=JSON.parse(fs.readFileSync(path.join(resources,tag),'utf8'));if(!data.values.includes('ccd:turning_booster_pump'))data.values.push('ccd:turning_booster_pump');save(tag,data);
 }
 for(const locale of ['zh_cn','en_us']){
  const rel=`assets/ccd/lang/${locale}.json`,data=JSON.parse(fs.readFileSync(path.join(resources,rel),'utf8'));
  data['block.ccd.turning_booster_pump']=locale==='zh_cn'?'转向增压泵':'Turning Booster Pump';
  data['block.ccd.turning_booster_pump.tooltip.summary']=locale==='zh_cn'?'将流体管路转过_直角_，保持增压泵的输送能力。':'Turns fluid flow through a _Right Angle_, retaining the Booster Pump’s performance.';
  save(rel,data);
 }
 const output=path.join(root,'outputs/turning-booster-pump');fs.mkdirSync(output,{recursive:true});
 const full=await mesh('ccd:item/turning_booster_pump'),panels=[];
 for(const [i,view]of [[0,[26,23,-34]],[1,[-26,23,34]],[2,[26,-23,34]]])panels.push({input:await render(full,500,480,view,{center:[8,8,8],scale:23}),left:i*500,top:55});
 const text=Buffer.from('<svg width="1500" height="585" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="750" y="35" font-size="26">转向增压泵 · 直角双管口 / 同平面双传动轴</text><text x="250" y="565" font-size="18">两个流体口</text><text x="750" y="565" font-size="18">封闭侧面</text><text x="1250" y="565" font-size="18">两个传动轴接口</text></g></svg>');
 await sharp({create:{width:1500,height:585,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:text,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 console.log('Generated 24 orientations and preview; existing textures unchanged.');
}
main().catch(e=>{console.error(e);process.exitCode=1;});
