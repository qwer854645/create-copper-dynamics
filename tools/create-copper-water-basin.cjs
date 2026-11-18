// Modern cabinet sink: original CCD geometry and pixels, informed by Cooking for Blockheads proportions.
// Usage: node tools/create-copper-water-basin.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
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
 const rimClusters=[
  '66777665556665','66666655556654','65555555555543','65554445555543',
  '65544445555433','65444455554433','65444555544333','65555554443343',
  '65555444433343','55554444333443','55444443334433','44433333333322',
  '33322222222211','12211111111000'];
 if(rimClusters.some(row=>row.length!==14))throw Error('Invalid copper rim pixels');
 const outer=Buffer.alloc(1024),front=Buffer.alloc(1024),inner=Buffer.alloc(1024),rim=Buffer.alloc(1024),metal=Buffer.alloc(1024);
 const steel=[[53,52,56],[79,79,79],[103,97,97],[130,132,124],[159,163,153],[190,193,175]];
 for(let y=0;y<16;y++)for(let x=0;x<16;x++){
  const px=Math.max(0,Math.min(13,x-1)),py=Math.max(0,Math.min(13,y-1));
  const cluster=Number(clusters[py][px]);
  let copper=cluster;
  if(y===0)copper=x<10?6:4;
  else if(y===15)copper=x<6?2:0;
  else if(x===0)copper=y<9?5:3;
  else if(x===15)copper=y<6?3:1;
  paint(outer,x,y,colors[copper]);
  // Flat folded sheet-metal doors, with a single seam rather than inset furniture moulding.
  paint(front,x,y,colors[x===8?0:x===9?Math.min(copper+1,6):copper]);
  const iron=Math.max(1,Math.min(5,cluster-1));
  paint(inner,x,y,steel[Math.max(1,iron-1)]);
  paint(metal,x,y,steel[iron]);
  // Copper folded countertop, drawn separately from the cabinet and iron bowl.
  let rimColor=Number(rimClusters[py][px]);
  if(y===0)rimColor=x<11?6:5;
  else if(y===15)rimColor=x<6?2:1;
  else if(x===0)rimColor=y<10?6:4;
  else if(x===15)rimColor=y<5?3:1;
  paint(rim,x,y,colors[rimColor]);
 }
 // Short connected wear marks at folded edges; no evenly spaced dots or rivets.
 for(const [x,y]of [[2,2],[3,2],[4,2],[3,3],[12,11],[12,12],[11,12]])paint(front,x,y,colors[y<5?7:2]);
 const floor=Buffer.from(inner);
 for(let y=6;y<10;y++)for(let x=6;x<10;x++)paint(floor,x,y,(x===7||x===8)&&(y===7||y===8)?steel[0]:steel[4]);
 for(const [name,pixels]of Object.entries({outer,front,inner,rim,floor,metal}))
  await sharp(pixels,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,name+'.png'));
 const elements=[
  box('recessed_plinth',[1,0,1],[15,1,15],'metal'),
  box('cabinet_and_bowl_floor',[1,1,1],[15,9,15]),
  box('back_wall',[0,1,0],[16,15,3]),box('front_cabinet',[0,1,13],[16,15,16]),
  box('left_wall',[0,1,3],[3,15,13]),box('right_wall',[13,1,3],[16,15,13]),
  box('counter_back',[0,15,0],[16,16,3],'rim'),box('counter_front',[0,15,13],[16,16,16],'rim'),
  box('counter_left',[0,15,3],[3,16,13],'rim'),box('counter_right',[13,15,3],[16,16,13],'rim'),
  box('tap_foot',[6.5,16,0.5],[9.5,17,3.5],'metal'),
  box('tap_stem',[7.5,17,1.5],[8.5,21,2.5],'metal'),
  box('tap_arch',[7.5,20,2.5],[8.5,21,7],'metal'),
  box('tap_nozzle',[7.5,18.5,6],[8.5,20,7],'metal'),
  box('mixer_lever',[9,17,1],[11,17.75,2],'metal'),
  box('left_cabinet_handle',[6,7,15.75],[6.75,10,16.25],'metal'),
  box('right_cabinet_handle',[9.25,7,15.75],[10,10,16.25],'metal')];
 elements[1].faces.up.texture='#floor';
 elements[3].faces.south={texture:'#front',uv:[0,1,16,15]};
 for(const [index,side]of [[2,'south'],[3,'north'],[4,'east'],[5,'west']])elements[index].faces[side].texture='#inner';
 elements[13].faces.down.texture='#hole';
 // Sample the textured interior of the iron atlas on the small tap and handle faces.
 for(const e of elements.slice(10))for(const f of Object.values(e.faces))if(f.texture==='#metal')
  f.uv=f.uv.map((v,i)=>v+(i%2===0?4:3));
 const model={parent:'minecraft:block/block',textures:{particle:'ccd:block/copper_water_basin/outer',
  outer:'ccd:block/copper_water_basin/outer',front:'ccd:block/copper_water_basin/front',inner:'ccd:block/copper_water_basin/inner',
  floor:'ccd:block/copper_water_basin/floor',rim:'ccd:block/copper_water_basin/rim',metal:'ccd:block/copper_water_basin/metal',hole:'ccd:block/fluid_arm/core'},elements};
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
  const buffer=await render(faces,500,510,view,{center:[8,10,8],scale:18});
  await sharp(buffer).png().toFile(path.join(output,name+'.png'));panels.push({input:buffer,left:i*500,top:95});
 }
 const labels=Buffer.from(`<svg width="1500" height="700" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="750" y="40" font-size="29">铜水槽 · 铜色折边台面</text><text x="750" y="78" font-size="16">柜体与台面统一铜色 · 保留灰色槽盆与细龙头 · Create 风格明暗像素簇</text><text x="250" y="647" font-size="19">正面 · 水面示意</text><text x="750" y="647" font-size="19">空槽结构与金属内壁</text><text x="1250" y="647" font-size="19">背面</text><text x="750" y="683" font-size="14" fill="#677070">实际模型离线预览 / 本次仅重绘顶部台面贴图 / 未启动游戏</text></g></svg>`);
 await sharp({create:{width:1500,height:700,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 save(path.join(output,'model.meta.json'),{textureSize:[16,16],geometry:elements.map(({name,from,to})=>({name,from,to})),
  front:'south',facing:'four horizontal directions',rimHeight:16,nativeWaterScaleY:.625,nativeWaterOffsetY:6.5,fullWaterSurfaceY:14,
  waterRendering:'native FluidTankRenderer inside a scaled pose; actual texture, tint and animation preserved',
  inventoryAndInteraction:'unchanged infinite fixed water; ordinary right click fills, sneak disposes',
  reference:'https://github.com/TwelveIterations/CookingForBlockheads/blob/26.3/common/src/main/resources/assets/cookingforblockheads/models/block/sink.json',
  artwork:'CCD original geometry and pixels; external reference applies to structure only, not textures',
  pixelStyle:'approved CCD booster pump: copper cabinet and separately drawn folded copper countertop; iron bowl and tap; original 16x16 clusters'});
 const swatches=[];
 for(const [i,name]of ['outer','front','inner','rim','floor','metal'].entries())swatches.push({
  input:await sharp(path.join(textures,name+'.png')).resize(192,192,{kernel:'nearest'}).png().toBuffer(),left:(i%3)*208,top:Math.floor(i/3)*208});
 await sharp({create:{width:608,height:400,channels:4,background:'#e5e8e2'}}).composite(swatches).png().toFile(path.join(output,'textures.png'));
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),elements:elements.length}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
