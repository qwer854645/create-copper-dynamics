// One copper body and one native rotating gear. No generated textures.
const fs=require('fs'),path=require('path'),sharp=require('sharp');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const {turn}=require('./create-turning-booster-pump.cjs');
const root=path.resolve(__dirname,'../src/main/resources');
function save(rel,d){const p=path.join(root,rel);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(d,null,2)+'\n');}
const textures={copper:'create:block/copper_underside',shaft:'ccd:block/booster_pump/shaft_face',pipe:'create:block/pipes',gear:'create:block/pump',particle:'create:block/copper_underside'};
function box(name,from,to){return{name,from,to,faces:Object.fromEntries(['north','south','east','west','up','down'].map(s=>[s,{texture:'#copper',uv:[2,2,14,14]}]))};}
async function main(){
 // Central vertical slot exposes the native wheel at the top and bottom of the copper shell.
 const body=[box('left_copper_housing',[2,2,2],[6.25,14,14]),box('right_copper_housing',[9.75,2,2],[14,14,14])];
  // Trial: extend both pipe necks by three pixels and move their end rims outward.
  for(const side of ['north','south'])for(const part of ['rim_connector','rim'])for(const original of model(`create:block/fluid_pipe/${part}/${side}`).elements){
   const e=structuredClone(original);e.name='native_'+part+'_'+side;
   const offset=side==='north'?-3:3;
   if(part==='rim'){e.from[2]+=offset;e.to[2]+=offset;}
   else if(side==='north')e.from[2]+=offset;else e.to[2]+=offset;
   for(const f of Object.values(e.faces)){f.texture='#pipe';delete f.cullface;}body.push(e);
  }
 const basis=[[0,0,-1],[0,1,0],[1,0,0]],gear=model('create:block/mechanical_pump/cog').elements.map(original=>{
  const e=turn({...original,rotation:undefined},basis);
  for(const p of [e.from,e.to])for(let i=0;i<3;i++)p[i]=8+(p[i]-8)*(i===0?.3:1);
  if(original.rotation?.angle)e.rotation={angle:original.rotation.angle,axis:'x',origin:[8,8,8]};
  for(const f of Object.values(e.faces))f.texture='#gear';return e;
 });
  const head=model('create:block/gauge/speedometer/head'),dial=model('create:block/gauge/dial');
  textures.gauge='create:block/gauge';
  const needles=[];
  for(const side of [1,-1])for(const [source,list]of [[head,body],[dial,needles]])for(const original of source.elements){
   let e=structuredClone(original);delete e.rotation;
   for(const p of [e.from,e.to])p[0]-=1.9;
   for(const f of Object.values(e.faces))f.texture='#gauge';
   if(side<0)e=turn(e,[[-1,0,0],[0,1,0],[0,0,-1]]);
   e.name=source===head?'signal_gauge_head':'signal_gauge_needle';list.push(e);
  }
  const wrap=elements=>({parent:'create:block/block',render_type:'minecraft:cutout_mipped',textures,elements});
 save('assets/ccd/models/block/frequency_pump/body.json',wrap(body));save('assets/ccd/models/block/frequency_pump/cog.json',wrap(gear));
 const shaft=model('create:block/shaft_half'),halves=[];
 for(const b of [basis,[[0,0,1],[0,1,0],[-1,0,0]]])for(const original of shaft.elements){const e=turn(original,b);for(const f of Object.values(e.faces))f.texture=f.texture==='#0'?'#shaft_side':'#shaft_end';halves.push(e);}
  save('assets/ccd/models/item/reciprocating_fluid_pump.json',wrap([...body,...gear,...needles]));
 const variants={};for(const[facing,y]of Object.entries({north:0,east:90,south:180,west:270,up:0,down:0}))variants['facing='+facing]={model:'ccd:block/frequency_pump/body',...(y?{y}:{})};
 save('assets/ccd/blockstates/reciprocating_fluid_pump.json',{variants});
 for(const locale of ['zh_cn','en_us']){const rel=`assets/ccd/lang/${locale}.json`,d=JSON.parse(fs.readFileSync(path.join(root,rel),'utf8'));d['block.ccd.reciprocating_fluid_pump']=locale==='zh_cn'?'调频动力泵':'Variable Flow Mechanical Pump';save(rel,d);}
 fs.mkdirSync('outputs/frequency-pump',{recursive:true});const full=await mesh('ccd:item/reciprocating_fluid_pump'),panels=[];
 for(const[i,view]of [[0,[28,28,-34]],[1,[-28,22,34]]])panels.push({input:await render(full,550,520,view,{center:[8,8,8],scale:25}),left:i*550,top:0});
 await sharp({create:{width:1100,height:520,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile('outputs/frequency-pump/preview.png');
}main().catch(e=>{console.error(e);process.exitCode=1;});
