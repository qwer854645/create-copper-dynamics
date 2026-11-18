// CCD copper faucet with native Create pipe connection and spout nozzle components.
// Usage: node tools/create-faucet.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render,model:loadModel}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd');
const textures=path.join(assets,'textures/block/faucet'),output=path.join(root,'outputs/faucet');
for(const dir of [textures,output])fs.mkdirSync(dir,{recursive:true});
const copper=[[96,46,31],[144,73,49],[154,80,56],[167,90,64],[178,98,71],[194,107,76],[214,123,91],[227,130,108]];
const iron=[[53,52,56],[79,79,79],[103,97,97],[159,163,153]];
const image=fn=>{const data=Buffer.alloc(1024);for(let y=0;y<16;y++)for(let x=0;x<16;x++)data.set([...fn(x,y),255],(y*16+x)*4);return data;};
const patches=['166667776642','655666655543','655556655433','655555544433','655555444443','655544444543',
 '655444455553','554444555543','554455555433','454555544433','343333333221','122211111110'];
const body=image((x,y)=>x>=2&&x<14&&y>=2&&y<14?copper[Number(patches[y-2][x-2])]:copper[4]);
const handle=image((x,y)=>iron[x===4||y===4?3:x===11||y===11?0:x<6||y<6?2:1]);
function box(name,from,to,texture){
 const [x,y,z]=from,[X,Y,Z]=to,f=uv=>({texture:'#'+texture,uv});
 return {name,from,to,faces:{north:f([X,16-Y,x,16-y]),south:f([x,16-Y,X,16-y]),
  east:f([Z,16-Y,z,16-y]),west:f([z,16-Y,Z,16-y]),up:f([x,z,X,Z]),down:f([x,Z,X,z])}};
}
const nativeParts=[];
function native(id,texture,dy=0){
 nativeParts.push({id,translation:[0,dy,0]});
 return loadModel(id).elements.map((source,i)=>{
  const e=JSON.parse(JSON.stringify(source));e.name='native_'+id.split('/').slice(-2).join('_')+'_'+i;
  e.from[1]+=dy;e.to[1]+=dy;if(e.rotation)e.rotation.origin[1]+=dy;
  for(const spec of Object.values(e.faces))spec.texture='#'+texture;return e;
 });
}
const fixed=[
 ...native('create:block/fluid_pipe/rim_connector/north','pipe'),
 ...native('create:block/fluid_pipe/rim/north','pipe'),
 box('copper_neck',[6,7,3],[10,11,10],'body'),
 box('copper_elbow',[6,6,6],[10,8,10],'body'),
 ...native('create:block/spout/middle','nozzle',8),
 ...native('create:block/spout/bottom','nozzle',8),
 box('valve_stem',[7,11,7],[9,12,9],'body'),
];
// The native middle piece normally sits below the spout's upper module. Cap the exposed
// shoulder here using the same native nozzle face so the smaller faucet neck leaves no gap.
fixed.find(e=>e.name==='native_spout_middle_0').faces.up={texture:'#nozzle',uv:[5,5,11,11]};
function model(open){
 return {parent:'minecraft:block/block',render_type:'minecraft:cutout_mipped',textures:{
  body:'ccd:block/faucet/body',base:'ccd:block/faucet/body',handle:'ccd:block/faucet/handle',
  pipe:'create:block/pipes',nozzle:'create:block/spout_nozzle',particle:'ccd:block/faucet/body'},
  elements:[...fixed,open?box('handle_open',[4,12,7],[12,14,9],'handle'):box('handle_closed',[7,12,4],[9,14,12],'handle')]};
}
async function main(){
 for(const [name,data]of Object.entries({body,handle}))await sharp(data,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,name+'.png'));
 for(const [state,open]of [['open',true],['closed',false]])
  fs.writeFileSync(path.join(assets,'models/block/faucet_'+state+'.json'),JSON.stringify(model(open),null,2)+'\n');
 fs.writeFileSync(path.join(assets,'models/item/faucet.json'),JSON.stringify({parent:'ccd:block/faucet_closed',
  display:{gui:{rotation:[30,45,0],scale:[.625,.625,.625]}}},null,2)+'\n');
 const panels=[];
 for(const [i,state,view]of [[0,'closed',[24,18,34]],[1,'open',[24,18,34]],[2,'open',[24,-18,34]]]){
  const buffer=await render(await mesh('ccd:block/faucet_'+state),500,480,view,{center:[8,7.5,7],scale:28});
  await sharp(buffer).toFile(path.join(output,['closed','open','nozzle'][i]+'.png'));
  panels.push({input:buffer,left:i*500,top:100});
 }
 const labels=Buffer.from(`<svg width="1500" height="660" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="750" y="42" font-size="29">铜龙头 · 铜制弯管与原生喷嘴</text><text x="750" y="78" font-size="16">原生背部管口 · 深灰手柄 · 保留横纵开关状态</text><text x="250" y="611" font-size="18">关闭</text><text x="750" y="611" font-size="18">开启</text><text x="1250" y="611" font-size="18">出液嘴 · 下方视角</text><text x="750" y="648" font-size="14" fill="#677070">实际模型离线渲染 / 铜面与手柄为 16×16 像素贴图，管口及喷嘴使用 Create 原生资源</text></g></svg>`);
 await sharp({create:{width:1500,height:660,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 const layers=[];for(const [i,n]of ['body','handle'].entries())layers.push({input:await sharp(path.join(textures,n+'.png')).resize(256,256,{kernel:'nearest'}).png().toBuffer(),left:i*272,top:0});
 await sharp({create:{width:528,height:256,channels:4,background:'#e5e8e2'}}).composite(layers).png().toFile(path.join(output,'textures.png'));
 fs.writeFileSync(path.join(output,'model.meta.json'),JSON.stringify({originalArtwork:'copper neck/stem and handle texture only',
  textureSize:[16,16],nativeParts,elementsPerModel:fixed.length+1,
  outputCenter:[8,1,8],backConnection:'north',openHandleAxis:'x',closedHandleAxis:'z',rivets:0,
  smartFaucet:'inherits shared geometry; existing front filter offset remains outside neck',logicUnchanged:true},null,2)+'\n');
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),models:2,elementsPerModel:fixed.length+1,textures:2}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
