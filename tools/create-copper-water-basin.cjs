// Plain copper cabinet basin, bright iron rim and a half-size instance of CCD's copper faucet.
// Usage: node tools/create-copper-water-basin.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render,model:loadModel}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd');
const textures=path.join(assets,'textures/block/copper_water_basin'),output=path.join(root,'outputs/copper-water-basin');
for(const dir of [textures,output])fs.mkdirSync(dir,{recursive:true});
const save=(file,data)=>fs.writeFileSync(file,JSON.stringify(data,null,2)+'\n');
const colors=[[96,46,31],[144,73,49],[154,80,56],[167,90,64],[178,98,71],[194,107,76],[214,123,91],[227,130,108]];
const paint=(data,x,y,rgb)=>data.set([...rgb,255],(y*16+x)*4);
function box(name,from,to,texture='outer'){
 const [x,y,z]=from,[X,Y,Z]=to,f=(u,v)=>({texture:'#'+texture,uv:[0,0,u,v]});
 return {name,from,to,faces:{north:f(X-x,Y-y),south:f(X-x,Y-y),east:f(Z-z,Y-y),west:f(Z-z,Y-y),up:f(X-x,Z-z),down:f(X-x,Z-z)}};
}
async function main(){
 // New hand-authored clusters in the approved booster-pump style. No external texture input.
 const clusters=[
  '66777777776654','66666777665543','65556666555433','65555666554433',
  '65555555544433','65555554444333','65555444445543','65544444555543',
  '55444445555433','55444455544333','54445554443332','44455544433322',
  '33344433322211','12222111111100'];
 if(clusters.length!==14||clusters.some(row=>row.length!==14))throw Error('Invalid pixel-art panel');
 const outer=Buffer.alloc(1024),inner=Buffer.alloc(1024),rim=Buffer.alloc(1024);
 // Neutral greys observed in the vanilla iron-ingot sprite; all pixel layout is hand drawn here.
 const iron=[[53,53,53],[94,94,94],[114,114,114],[130,130,130],[168,168,168],[216,216,216],[255,255,255]];
 const silverRim=[145,168,184,208,224,240,255].map(value=>[value,value,value]);
 for(let z=0;z<16;z++)for(let x=0;x<16;x++){
  const px=Math.max(0,Math.min(13,x-1)),py=Math.max(0,Math.min(13,z-1)),cluster=Number(clusters[py][px]);
  let copper=cluster;
  if(z===0)copper=x<10?6:4;
  else if(z===15)copper=x<6?2:1;
  else if(x===0)copper=z<9?5:3;
  else if(x===15)copper=z<6?3:1;
  paint(outer,x,z,colors[copper]);
  paint(inner,x,z,iron[Math.max(1,Math.min(4,cluster-2))]);
  // One continuous ring atlas: bright upper/left bevel, neutral silver flat, darker lower/right bevel.
  let silver=5;
  if(z===0)silver=x===0||x===15?4:x<12?6:5;
  else if(z===15)silver=x<10?4:3;
  else if(x===0)silver=z<10?5:4;
  else if(x===15)silver=z<5?4:3;
  else if(z===1&&x>=2&&x<14)silver=x>=4&&x<9?6:4;
  else if(z===14&&x>=2&&x<14)silver=x<9?5:4;
  else if(x===1&&z>=2&&z<14)silver=z<8?4:5;
  else if(x===14&&z>=2&&z<14)silver=z<9?4:3;
  paint(rim,x,z,silverRim[silver]);
 }
 // An inset reveal under the rim, rather than an extra decorative frame.
 for(let x=0;x<16;x++){paint(inner,x,1,iron[1]);paint(inner,x,2,iron[3]);}
 const floor=Buffer.from(inner);
 for(let z=6;z<10;z++)for(let x=6;x<10;x++)paint(floor,x,z,(x===7||x===8)&&(z===7||z===8)?iron[0]:iron[4]);
 for(const [name,pixels]of Object.entries({outer,inner,rim,floor}))
  await sharp(pixels,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,name+'.png'));
 const elements=[
  box('recessed_plinth',[1,0,1],[15,1,15],'inner'),
  box('plain_cabinet_and_bowl_floor',[0,1,0],[16,9,16]),
  box('back_wall',[0,9,0],[16,15,2]),box('front_wall',[0,9,14],[16,15,16]),
  box('left_wall',[0,9,2],[2,15,14]),box('right_wall',[14,9,2],[16,15,14]),
  box('counter_back',[0,15,0],[16,16,2],'rim'),box('counter_front',[0,15,14],[16,16,16],'rim'),
  box('counter_left',[0,15,2],[2,16,14],'rim'),box('counter_right',[14,15,2],[16,16,14],'rim'),
  box('bowl_step_back',[2,9,2],[14,11,3],'inner'),box('bowl_step_front',[2,9,13],[14,11,14],'inner'),
  box('bowl_step_left',[2,9,3],[3,11,13],'inner'),box('bowl_step_right',[13,9,3],[14,11,13],'inner')];
 const worldUV=e=>({
  north:[16-e.to[0],16-e.to[1],16-e.from[0],16-e.from[1]],
  south:[e.from[0],16-e.to[1],e.to[0],16-e.from[1]],
  east:[16-e.to[2],16-e.to[1],16-e.from[2],16-e.from[1]],
  west:[e.from[2],16-e.to[1],e.to[2],16-e.from[1]],
  up:[e.from[0],e.from[2],e.to[0],e.to[2]],down:[e.from[0],e.from[2],e.to[0],e.to[2]]});
 for(const e of elements){
  const uv=worldUV(e);for(const [side,f]of Object.entries(e.faces))f.uv=uv[side];
 }
 elements[1].faces.up.texture='#floor';
 for(const [index,side]of [[2,'south'],[3,'north'],[4,'east'],[5,'west']])elements[index].faces[side].texture='#inner';
 for(const e of elements.filter(e=>e.name.startsWith('counter_'))){
  for(const side of ['north','south'])e.faces[side].uv=[e.from[0],0,e.to[0],1];
  for(const side of ['east','west'])e.faces[side].uv=[e.from[2],15,e.to[2],16];
 }
 const tapSource='ccd:block/faucet_open',tap=loadModel(tapSource),tapScale=.5,tapOffset=[4,17,1.5];
 const tapTextures={};
 for(const [key,value]of Object.entries(tap.textures))if(key!=='particle')tapTextures['tap_'+key]=value;
 const transform=p=>p.map((v,i)=>v*tapScale+tapOffset[i]);
 const mount=box('tap_mount',[6.5,16,0],[9.5,16.5,2],'rim');
 const riser=box('tap_riser',[7,16.5,.25],[9,21.5,1.75],'tap_body');
 elements.push(mount,riser);
 for(const original of tap.elements){
  const e=JSON.parse(JSON.stringify(original));
  e.name='scaled_faucet_'+(e.name||elements.length);
  e.from=transform(e.from);e.to=transform(e.to);
  if(e.rotation)e.rotation.origin=transform(e.rotation.origin);
  for(const f of Object.values(e.faces)){
   f.texture=f.texture.replace(/^#/,'#tap_');
   // A miniature pipe mouth is inside the sink block, not on its block boundary.
   delete f.cullface;
  }
  elements.push(e);
 }
 const model={parent:'minecraft:block/block',render_type:'minecraft:cutout_mipped',textures:{particle:'ccd:block/copper_water_basin/outer',
  outer:'ccd:block/copper_water_basin/outer',inner:'ccd:block/copper_water_basin/inner',
  floor:'ccd:block/copper_water_basin/floor',rim:'ccd:block/copper_water_basin/rim',...tapTextures},elements};
 save(path.join(assets,'models/block/copper_water_basin.json'),model);
 save(path.join(assets,'models/item/copper_water_basin.json'),{parent:'ccd:block/copper_water_basin',
  display:{gui:{rotation:[25,225,0],translation:[0,-1,0],scale:[.55,.55,.55]}}});
 const variants={};for(const [facing,y]of [['south',0],['west',90],['north',180],['east',270]])
  variants['facing='+facing]={model:'ccd:block/copper_water_basin',...(y?{y}:{})};
 save(path.join(assets,'blockstates/copper_water_basin.json'),{variants});
 const shell=await mesh('ccd:block/copper_water_basin'),waterData=Buffer.alloc(16*16*4);
 // Offline illustration only; the game keeps Create's actual biome-tinted and animated water.
 for(let y=0;y<16;y++)for(let x=0;x<16;x++){
  const stripe=(y===3&&x>=2&&x<=7)||(y===9&&x>=9&&x<=13)||(x===4&&y>=11&&y<=13);
  paint(waterData,x,y,stripe?[78,136,195]:[47,108,177]);
 }
 const surface={vertices:[[1.125,14,1.125],[1.125,14,14.875],[14.875,14,14.875],[14.875,14,1.125]],n:[0,1,0],
  uv:[[0,0],[0,16],[16,16],[16,0]],pixels:{data:waterData,info:{width:16,height:16,channels:4}}};
 const panels=[];
 for(const [i,faces,name,view]of [[0,[...shell,surface],'front',[25,24,34]],[1,shell,'empty',[25,30,34]],
  [2,[...shell,surface],'rear',[-26,24,-34]]]){
  const buffer=await render(faces,500,510,view,{center:[8,11,8],scale:18});
  await sharp(buffer).png().toFile(path.join(output,name+'.png'));panels.push({input:buffer,left:i*500,top:95});
 }
 const labels=Buffer.from(`<svg width="1500" height="700" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="750" y="40" font-size="29">铜水槽 · 半尺寸铜龙头</text><text x="750" y="78" font-size="16">保留素面柜体与明亮银边 · 原有铜龙头整体缩小一半 · 铜立管安装</text><text x="250" y="647" font-size="19">正面 · 水面示意</text><text x="750" y="647" font-size="19">空槽结构</text><text x="1250" y="647" font-size="19">背面</text><text x="750" y="683" font-size="14" fill="#677070">实际模型离线预览 / 缩小龙头沿用原有模型与 UV，不新增开关操作 / 未启动游戏</text></g></svg>`);
 await sharp({create:{width:1500,height:700,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 save(path.join(output,'model.meta.json'),{textureSize:[16,16],geometry:elements.map(({name,from,to})=>({name,from,to})),
  front:'south',facing:'four horizontal directions',rimHeight:16,nativeWaterScaleY:.625,nativeWaterOffsetY:6.5,fullWaterSurfaceY:14,
  opening:[[2,2],[14,14]],bowlStepHeight:[9,11],countertopUV:'one continuous 16x16 X/Z layout, four strips use absolute UVs',
  faucet:{source:tapSource,scale:tapScale,offset:tapOffset,geometry:'all source elements uniformly scaled; UVs unchanged; embedded mouth cullfaces removed',state:'fixed open appearance'},
  waterRendering:'native FluidTankRenderer inside a scaled pose; actual texture, tint and animation preserved',
  inventoryAndInteraction:'unchanged infinite fixed water; ordinary right click fills, sneak disposes',
  reference:'new user-directed structure; vanilla iron-ingot palette/bevel inspiration only',
  artwork:'CCD sink artwork plus existing CCD faucet; native pipe mouth and spout art retain Create ownership',
  pixelStyle:'plain copper clusters without seams or doors; neutral silver-white iron-ingot-style bevel; four original 16x16 textures'});
 const swatches=[];
 for(const [i,name]of ['outer','rim','inner','floor'].entries())swatches.push({
  input:await sharp(path.join(textures,name+'.png')).resize(192,192,{kernel:'nearest'}).png().toBuffer(),left:(i%2)*208,top:Math.floor(i/2)*208});
 await sharp({create:{width:400,height:400,channels:4,background:'#e5e8e2'}}).composite(swatches).png().toFile(path.join(output,'textures.png'));
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),elements:elements.length}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
