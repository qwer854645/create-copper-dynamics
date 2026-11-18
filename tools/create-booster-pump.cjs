// Original 16x16 pixel artwork, referencing Create's pump and copper material palette.
// Usage: node tools/create-booster-pump.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render,model:loadModel}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd');
const textures=path.join(assets,'textures/block/booster_pump'),output=path.join(root,'outputs/booster-pump');
for(const dir of [textures,output])fs.mkdirSync(dir,{recursive:true});
const copper=[[70,32,24],[144,73,49],[154,80,56],[167,90,64],[178,98,71],[194,107,76],[200,116,86],[214,123,91],[227,130,108]];
const image=fn=>{const data=Buffer.alloc(1024);for(let y=0;y<16;y++)for(let x=0;x<16;x++)data.set([...fn(x,y),255],(y*16+x)*4);return data;};
const paint=(data,x,y,color)=>data.set([...color,255],(y*16+x)*4);
// Twelve-by-twelve copper shell. Seams, reinforcement and wear are pixel clusters, not noise.
const panel=[
 '277778776642',
 '765666655543',
 '765556655433',
 '665555544433',
 '655555444443',
 '655544444543',
 '655444455553',
 '554444555543',
 '554455555433',
 '454555544433',
 '343333333221',
 '122211111110',
];
const body=image(()=>copper[5]);
for(let y=0;y<12;y++)for(let x=0;x<12;x++)paint(body,x+2,y+2,copper[Number(panel[y][x])]);
// The ring opening reveals the recessed, dark pump hub behind it.
for(let y=6;y<10;y++)for(let x=6;x<10;x++)paint(body,x,y,copper[x===6||y===6?1:0]);
const elements=[];
function box(name,from,to,texture){
 const [x,y,z]=from,[X,Y,Z]=to,face=(uv)=>({uv,texture:'#'+texture});
 elements.push({name,from,to,faces:{
  north:face([X,16-Y,x,16-y]),south:face([x,16-Y,X,16-y]),
  east:face([Z,16-Y,z,16-y]),west:face([z,16-Y,Z,16-y]),
  up:face([x,z,X,Z]),down:face([x,Z,X,z]),
 }});
}
box('copper_housing',[2,2,2],[14,14,14],'body');
for(const face of ['north','south','east','west'])elements[0].faces[face].texture='#shaft_face';
// Import the native pipe mouths verbatim, including their UV rotation and culling.
// No hand-drawn substitute, no CCD copy of the upstream pipe texture.
const nativeParts=[];
for(const side of ['up','down'])for(const part of ['rim_connector','rim']){
 const id='create:block/fluid_pipe/'+part+'/'+side,native=loadModel(id);
 nativeParts.push(id);
 for(const [i,original]of native.elements.entries()){
  const element=JSON.parse(JSON.stringify(original));element.name='native_'+part+'_'+side+'_'+i;
  for(const spec of Object.values(element.faces))spec.texture='#pipe';
  elements.push(element);
 }
}
// Use the original pump end-band artwork and exact face UV/rotation instead of a custom glyph.
const nativePump=loadModel('create:block/mechanical_pump/block');
for(const [end,y,Y]of [['inlet',2,4],['outlet',12,14]])for(const side of ['north','south','east','west']){
 const [from,to]={north:[[2,y,1.99],[14,Y,1.99]],south:[[2,y,14.01],[14,Y,14.01]],
  west:[[1.99,y,2],[1.99,Y,14]],east:[[14.01,y,2],[14.01,Y,14]]}[side];
 elements.push({name:'native_pump_mark_'+end+'_'+side,from,to,
  faces:{[side]:{...nativePump.elements[0].faces[side],texture:'#native_pump'}}});
}
const model={parent:'minecraft:block/block',render_type:'minecraft:cutout_mipped',
 textures:{body:'ccd:block/booster_pump/body',shaft_face:'ccd:block/booster_pump/shaft_face',pipe:'create:block/pipes',
  native_pump:'create:block/pump'},elements};
model.textures.particle=model.textures.body;
const rotate=(v,axis,angle,point)=>{
 const a=angle*Math.PI/180,c=Math.cos(a),s=Math.sin(a),p=v.map(n=>n-(point?8:0)),q=[...p];
 const [i,j]=axis==='x'?[1,2]:[2,0];q[i]=p[i]*c-p[j]*s;q[j]=p[i]*s+p[j]*c;
 return q.map(n=>n+(point?8:0));
};
const transform=(faces,axis,angle)=>faces.map(f=>({...f,vertices:f.vertices.map(v=>rotate(v,axis,angle,true)),n:rotate(f.n,axis,angle,false)}));
const faceVertices=(from,to,side)=>{
 const [x,y,z]=from,[X,Y,Z]=to;
 return {north:[[X,Y,z],[X,y,z],[x,y,z],[x,Y,z]],south:[[x,Y,Z],[x,y,Z],[X,y,Z],[X,Y,Z]],
  west:[[x,Y,z],[x,y,z],[x,y,Z],[x,Y,Z]],east:[[X,Y,Z],[X,y,Z],[X,y,z],[X,Y,z]],
  up:[[x,Y,z],[x,Y,Z],[X,Y,Z],[X,Y,z]],down:[[x,y,Z],[x,y,z],[X,y,z],[X,y,Z]]}[side];
};
function inventoryShaft(source,yaw){
 const e=structuredClone(source),normals={north:[0,0,-1],south:[0,0,1],east:[1,0,0],west:[-1,0,0],up:[0,1,0],down:[0,-1,0]};
 const turn=p=>rotate(p,'y',yaw,true).map(v=>Math.round(v*1e6)/1e6);
 const corners=[turn(source.from),turn(source.to)];
 e.from=[0,1,2].map(k=>Math.min(...corners.map(p=>p[k])));e.to=[0,1,2].map(k=>Math.max(...corners.map(p=>p[k])));
 e.name='inventory_shaft_'+yaw;e.faces={};delete e.rotation;
 for(const [side,f]of Object.entries(source.faces)){
  const n=rotate(normals[side],'y',yaw,false).map(Math.round);
  const target=Object.keys(normals).find(key=>normals[key].every((v,k)=>v===n[k]));
  const vertices=faceVertices(source.from,source.to,side).map(turn),first=faceVertices(e.from,e.to,target)[0];
  const shift=vertices.findIndex(p=>p.every((v,k)=>Math.abs(v-first[k])<1e-5));
  if(shift<0)throw Error('Unmatched native shaft UV corner');
  e.faces[target]={...f,texture:f.texture==='#0'?'#shaft_side':'#shaft_end',rotation:((f.rotation||0)+shift*90)%360};
 }
 return e;
}
async function main(){
 // Create's gearbox/encased-shaft aperture is an opaque dark circular recess, not an alpha cutout.
 // Preserve the native hole and wall pixels; replace the old casing's brown corner pixels with copper.
 const reference=path.resolve(process.argv[2]);
 const {data:opening,info}=await sharp(path.join(reference,'assets/create/textures/block/gearbox.png'))
  .ensureAlpha().raw().toBuffer({resolveWithObject:true});
 if(info.width!==16||info.height!==16)throw Error('Unexpected native gearbox texture size');
 const shaftFace=Buffer.from(body);
 let casingPixelsReplaced=0;
 for(let y=4;y<12;y++)for(let x=4;x<12;x++){
  const i=(y*16+x)*4;
  if(opening[i]===112&&opening[i+1]===82&&opening[i+2]===46){casingPixelsReplaced++;continue;}
  opening.copy(shaftFace,i,i,i+4);
 }
 if(casingPixelsReplaced!==4)throw Error('Unexpected native gearbox casing pixels');
 await sharp(body,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,'body.png'));
  await sharp(shaftFace,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,'shaft_face.png'));
  require('node:child_process').execFileSync('py',[path.join(root,'tools/symmetrize-fluid-housings.py'),'booster_pump'],{stdio:'inherit'});
 fs.writeFileSync(path.join(assets,'models/block/booster_pump.json'),JSON.stringify(model,null,2)+'\n');
 const nativeShaft=loadModel('create:block/shaft_half');
 const item={parent:'ccd:block/booster_pump',textures:{shaft_side:nativeShaft.textures['0'],shaft_end:nativeShaft.textures['1']},
  elements:[...elements,...[0,90,180,270].flatMap(yaw=>nativeShaft.elements.map(e=>inventoryShaft(e,yaw)))]};
 fs.writeFileSync(path.join(assets,'models/item/booster_pump.json'),JSON.stringify(item,null,2)+'\n');
 const full=await mesh('ccd:item/booster_pump');
 const horizontal=transform(full,'x',90),panels=[];
 for(const [i,faces,view]of [[0,horizontal,[26,20,-34]],[1,horizontal,[-26,20,34]],[2,full,[26,20,-34]]]){
  const buffer=await render(faces,500,480,view,{center:[8,8,8],scale:23});
  await sharp(buffer).toFile(path.join(output,['horizontal-front','horizontal-rear','vertical'][i]+'.png'));
  panels.push({input:buffer,left:i*500,top:100});
 }
 const labels=Buffer.from(`<svg width="1500" height="660" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei, sans-serif" text-anchor="middle" fill="#303535"><text x="750" y="42" font-size="29">增压泵 · 原版机械泵方向标记</text><text x="750" y="78" font-size="16">直接使用原版两端贴图与 UV · 四侧共八处 · 原生轴孔、管口与半轴</text><text x="250" y="612" font-size="18">水平管路 · 正面</text><text x="750" y="612" font-size="18">水平管路 · 背面</text><text x="1250" y="612" font-size="18">竖直管路</text><text x="750" y="644" font-size="14" fill="#677070">实际模型离线渲染 / 方向标记引用 create:block/pump，不再绘制替代箭头</text></g></svg>`);
 await sharp({create:{width:1500,height:660,channels:4,background:'#e5e8e2'}})
  .composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 await sharp({create:{width:528,height:256,channels:4,background:'#e5e8e2'}}).composite([
  {input:await sharp(path.join(textures,'body.png')).resize(256,256,{kernel:'nearest'}).png().toBuffer(),left:0,top:0},
  {input:await sharp(path.join(textures,'shaft_face.png')).resize(256,256,{kernel:'nearest'}).png().toBuffer(),left:272,top:0},
 ]).png().toFile(path.join(output,'textures.png'));
 fs.writeFileSync(path.join(output,'model.meta.json'),JSON.stringify({originalArtwork:'copper housing pixels only; shaft-face center belongs to Create',textureSize:[16,16],
  paletteReference:['Create pump','Create copper_casing'],elements:elements.length,
  fluidAxis:'y before blockstate rotation',nativePipeModels:nativeParts,nativePipeTexture:'create:block/pipes',
  nativeShaftModel:'create:block/shaft_half',shaftOpeningTexture:'Create gearbox.png crop [4,4,12,12], brown casing corners replaced by copper',
  shaftOpeningCasingPixelsReplaced:casingPixelsReplaced,
  shaftOpeningFaces:4,customBearings:0,rivets:0,itemShafts:4,shaftFaceSize:[16,16],
  arrows:{perFace:2,faces:4,direction:'local +Y, follows FACING and native wrench reversal',
   sourceModel:'create:block/mechanical_pump/block',sourceTexture:'create:block/pump',
   sourceUV:[8,0,6.5,6],sourceRotation:270,stampSize:[12,2],planeOffset:.01}},null,2)+'\n');
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),elements:elements.length,textures:2}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
