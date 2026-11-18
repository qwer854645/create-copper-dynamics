// Kitchen sink: shallow copper bowl, flat rim and compact copper tap. Pixel artwork uses CCD's approved palette.
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
 const [x,y,z]=from,[X,Y,Z]=to,f=uv=>({texture:'#'+texture,uv});
 return {name,from,to,faces:{north:f([X,16-Y,x,16-y]),south:f([x,16-Y,X,16-y]),
  east:f([Z,16-Y,z,16-y]),west:f([z,16-Y,Z,16-y]),up:f([x,z,X,Z]),down:f([x,Z,X,z])}};
}
async function main(){
 const {data,info}=await sharp(path.join(assets,'textures/block/faucet/body.png')).ensureAlpha().raw().toBuffer({resolveWithObject:true});
 if(info.width!==16||info.height!==16)throw Error('Unexpected copper source texture');
 const outer=Buffer.from(data),inner=Buffer.from(data),rim=Buffer.from(data);
 for(let y=0;y<16;y++)for(let x=0;x<16;x++){
  const i=(y*16+x)*4,index=colors.findIndex(c=>c.every((v,n)=>v===data[i+n]));
  if(index<0)throw Error('Unexpected CCD copper palette');
  paint(inner,x,y,colors[Math.max(1,index-1)]);
  if(y===8)paint(outer,x,y,colors[x<7?6:5]);
  if(y===9)paint(outer,x,y,colors[x<10?3:2]);
  if(y===15)paint(outer,x,y,colors[1]);
  if(x===0||y===0)paint(rim,x,y,colors[6]);
  if(x===15||y===15)paint(rim,x,y,colors[2]);
 }
 const floor=Buffer.from(inner);
 for(let y=7;y<10;y++)for(let x=7;x<10;x++)paint(floor,x,y,x===8?[40,43,44]:[133,145,144]);
 for(const [name,pixels]of Object.entries({outer,inner,rim,floor}))
  await sharp(pixels,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,name+'.png'));
 const elements=[box('bowl_floor',[0,0,0],[16,2,16]),box('back_deck',[0,2,0],[16,8,3]),
  box('front_wall',[0,2,14],[16,8,16]),box('left_wall',[0,2,3],[2,8,14]),box('right_wall',[14,2,3],[16,8,14]),
  box('tap_foot',[6,8,0],[10,9,4],'rim'),box('tap_stem',[7,9,1],[9,15,3]),
  box('tap_spout',[7,13,3],[9,15,7]),box('tap_nozzle',[7,11,6],[9,13,8],'metal')];
 elements[0].faces.up.texture='#floor';
 for(const e of elements.slice(1,5))e.faces.up.texture='#rim';
 for(const [index,side]of [[1,'south'],[2,'north'],[3,'east'],[4,'west']])elements[index].faces[side].texture='#inner';
 elements[8].faces.down.texture='#hole';
 const model={parent:'minecraft:block/block',textures:{particle:'ccd:block/copper_water_basin/outer',
  outer:'ccd:block/copper_water_basin/outer',inner:'ccd:block/copper_water_basin/inner',floor:'ccd:block/copper_water_basin/floor',
  rim:'ccd:block/copper_water_basin/rim',metal:'ccd:block/fluid_arm/metal',hole:'ccd:block/fluid_arm/core'},elements};
 save(path.join(assets,'models/block/copper_water_basin.json'),model);
 const variants={};for(const [facing,y]of [['south',0],['west',90],['north',180],['east',270]])
  variants['facing='+facing]={model:'ccd:block/copper_water_basin',...(y?{y}:{})};
 save(path.join(assets,'blockstates/copper_water_basin.json'),{variants});
 const shell=await mesh('ccd:block/copper_water_basin'),waterData=Buffer.alloc(16*16*4);
 // Offline illustration only; the game keeps Create's actual biome-tinted and animated water.
 for(let y=0;y<16;y++)for(let x=0;x<16;x++){
  const stripe=(y===3&&x>=2&&x<=7)||(y===9&&x>=9&&x<=13)||(x===4&&y>=11&&y<=13);
  paint(waterData,x,y,stripe?[78,136,195]:[47,108,177]);
 }
 const surface={vertices:[[1.125,6,1.125],[1.125,6,14.875],[14.875,6,14.875],[14.875,6,1.125]],n:[0,1,0],
  uv:[[0,0],[0,16],[16,16],[16,0]],pixels:{data:waterData,info:{width:16,height:16,channels:4}}};
 const panels=[];
 for(const [i,faces,name,view]of [[0,[...shell,surface],'front',[25,24,34]],[1,shell,'empty',[25,30,34]],
  [2,[...shell,surface],'rear',[-26,24,-34]]]){
  const buffer=await render(faces,500,510,view,{center:[8,7,8],scale:21});
  await sharp(buffer).png().toFile(path.join(output,name+'.png'));panels.push({input:buffer,left:i*500,top:95});
 }
 const labels=Buffer.from(`<svg width="1500" height="700" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="750" y="40" font-size="29">铜水槽 · 厨房水槽式样</text><text x="750" y="78" font-size="16">浅槽体 · 平整槽沿 · 铜龙头 · 灰色短出水嘴</text><text x="250" y="647" font-size="19">正面 · 水面示意</text><text x="750" y="647" font-size="19">空槽结构与排水口细节</text><text x="1250" y="647" font-size="19">背面</text><text x="750" y="683" font-size="14" fill="#677070">实际模型离线预览 / 空槽仅用于查看结构；游戏内始终装水并沿用原生水面渲染 / 未启动游戏</text></g></svg>`);
 await sharp({create:{width:1500,height:700,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 save(path.join(output,'model.meta.json'),{textureSize:[16,16],geometry:elements.map(({name,from,to})=>({name,from,to})),
  front:'south',facing:'four horizontal directions',rimHeight:8,nativeWaterScaleY:.5,fullWaterSurfaceY:6,
  waterRendering:'native FluidTankRenderer inside a scaled pose; actual texture, tint and animation preserved',
  inventoryAndInteraction:'unchanged infinite fixed water; ordinary right click fills, sneak disposes',
  artwork:'CCD original pixel textures derived from existing CCD faucet palette; no upstream artwork copied'});
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),elements:elements.length}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
