// Eight-sided copper vessel body study. No supply mechanics are changed.
// Usage: node tools/create-copper-water-supply.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd');
const textures=path.join(assets,'textures/block/copper_water_supply'),output=path.join(root,'outputs/copper-water-supply');
for(const dir of [textures,output])fs.mkdirSync(dir,{recursive:true});
const save=(p,d)=>fs.writeFileSync(p,JSON.stringify(d,null,2)+'\n');
const copper=[[63,32,22],[96,46,31],[123,61,41],[144,73,49],[167,90,64],[194,107,76],[214,123,91],[227,130,108],[239,156,128]];
const iron=[[53,52,56],[79,79,79],[103,97,97],[130,132,124],[159,163,153]];
const clusters=[
 '5667777766665554','6667777666655544','6777776666555443','7777766665554433',
 '7776666555544433','6766665555544333','6666655555444332','6666555554444332',
 '6655555444443332','6555554444433332','6555544444433322','5555444554433322',
 '5554445555433222','5444455554433221','4444555544332211','3333444333222111'];
const paint=(data,x,y,rgb)=>data.set([...rgb,255],(y*16+x)*4);
const round=v=>Math.round(v*1e6)/1e6;
const face=(texture,uv)=>({texture:'#'+texture,uv});
function box(name,from,to,texture){
 const [x,y,z]=from,[X,Y,Z]=to,f=uv=>face(texture,uv);
 return {name,from,to,faces:{north:f([16-X,16-Y,16-x,16-y]),south:f([x,16-Y,X,16-y]),
  east:f([16-Z,16-Y,16-z,16-y]),west:f([z,16-Y,Z,16-y]),up:f([x,z,X,Z]),down:f([x,z,X,Z])}};
}
function octagon(name,r,t,y0,y1,texture,closed=false){
 const elements=[],a=8-t,b=8+t,lo=8-r,hi=8+r;
 function plane(suffix,from,to,side,width,angle=0){
  const element={name:name+'_'+suffix,from:from.map(round),to:to.map(round),
   faces:{[side]:face(texture,[0,16-y1,round(width),16-y0])}};
  if(angle)element.rotation={origin:[8,8,8],axis:'y',angle};
  elements.push(element);
 }
 plane('north',[a,y0,lo],[b,y1,lo],'north',2*t);
 plane('south',[a,y0,hi],[b,y1,hi],'south',2*t);
 plane('west',[lo,y0,a],[lo,y1,b],'west',2*t);
 plane('east',[hi,y0,a],[hi,y1,b],'east',2*t);
 const h=(r-t)/Math.sqrt(2),d=(r+t)/Math.sqrt(2);
 for(const angle of [-45,45]){
  plane('north_diagonal_'+angle,[8-h,y0,8-d],[8+h,y1,8-d],'north',2*h,angle);
  plane('south_diagonal_'+angle,[8-h,y0,8+d],[8+h,y1,8+d],'south',2*h,angle);
 }
 if(closed){
  // Cross and four diagonal infills exactly fill the cap. Constant UVs make
  // overlapping coplanar infills identical, including their shading.
  function cap(suffix,from,to,angle=0){
   const element={name:name+'_cap_'+suffix,from:from.map(round),to:to.map(round),
    faces:{up:face('flat',[0,0,1,1]),down:face('flat',[0,0,1,1])}};
   if(angle)element.rotation={origin:[8,8,8],axis:'y',angle};
   elements.push(element);
  }
  cap('cross_x',[lo,y0,a],[hi,y1,b]);cap('cross_z',[a,y0,lo],[b,y1,hi]);
  for(const angle of [-45,45]){
   cap('north_'+angle,[8-h,y0,8-d],[8+h,y1,8-d+h],angle);
   cap('south_'+angle,[8-h,y0,8+d-h],[8+h,y1,8+d],angle);
  }
 }
 return elements;
}
async function main(){
 if(clusters.length!==16||clusters.some(row=>row.length!==16))throw Error('Invalid pixel cluster rows');
 const body=Buffer.alloc(1024),cap=Buffer.alloc(1024),band=Buffer.alloc(1024),flat=Buffer.alloc(1024);
 for(let y=0;y<16;y++)for(let x=0;x<16;x++){
  const shade=Number(clusters[y][x]);
  paint(body,x,y,copper[shade]);
  let top=Math.min(7,shade+1);
  if(x===0||y===0)top=6;else if(x===15||y===15)top=2;
  paint(cap,x,y,copper[top]);
  paint(band,x,y,iron[y===1||y===14?3:y===0||y===15?1:2]);
  paint(flat,x,y,iron[2]);
 }
 for(const [name,data]of Object.entries({body,cap,band,flat}))
  await sharp(data,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,name+'.png'));
 const elements=[
  ...octagon('lower_iron_foot',7.5,4.5,0,1.5,'band',true),
  ...octagon('copper_vessel',7,4,1.5,14,'body'),
  ...octagon('upper_iron_collar',7.5,4.5,14,15,'band',true),
  box('fixed_copper_crown',[3.5,15,3.5],[12.5,16,12.5],'cap')];
 save(path.join(assets,'models/block/copper_water_supply.json'),{parent:'minecraft:block/block',ambientocclusion:false,textures:{
  body:'ccd:block/copper_water_supply/body',cap:'ccd:block/copper_water_supply/cap',
  band:'ccd:block/copper_water_supply/band',flat:'ccd:block/copper_water_supply/flat',
  particle:'ccd:block/copper_water_supply/body'},elements});
 save(path.join(assets,'blockstates/copper_water_supply.json'),{variants:{'':{model:'ccd:block/copper_water_supply'}}});
 const faces=await mesh('ccd:block/copper_water_supply'),panels=[];
 for(const [i,name,view]of [[0,'front',[26,20,34]],[1,'rear',[-26,20,-34]],[2,'top',[14,42,22]]]){
  const buffer=await render(faces,500,510,view,{center:[8,8,8],scale:20});
  await sharp(buffer).png().toFile(path.join(output,name+'.png'));panels.push({input:buffer,left:i*500,top:95});
 }
 const labels=Buffer.from(`<svg width="1500" height="700" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="750" y="40" font-size="29">铜供水箱 · 八棱罐身试版</text><text x="750" y="78" font-size="16">切角铜罐身 · 深灰束口 · 低矮底座 · 固定铜顶板</text><text x="250" y="647" font-size="19">正面</text><text x="750" y="647" font-size="19">背面</text><text x="1250" y="647" font-size="19">顶面</text><text x="750" y="683" font-size="14" fill="#677070">实际模型离线预览 / 本次确认主体轮廓，接口暂未加入 / 未构建或启动游戏</text></g></svg>`);
 await sharp({create:{width:1500,height:700,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 const swatches=[];for(const [i,name]of ['body','cap','band','flat'].entries())swatches.push({
  input:await sharp(path.join(textures,name+'.png')).resize(192,192,{kernel:'nearest'}).png().toBuffer(),left:i*208,top:0});
 await sharp({create:{width:816,height:192,channels:4,background:'#e5e8e2'}}).composite(swatches).png().toFile(path.join(output,'textures.png'));
 save(path.join(output,'model.meta.json'),{stage:'octagonal body study',textureSize:[16,16],
  geometry:elements.map(({name,from,to,rotation})=>({name,from,to,rotation})),
  style:'CCD copper pixel clusters; eight-sided vessel, iron collars and low foot',
  connectionPlan:'show native fluid interface only on faces touching valid containers; deferred until body approval',
  behavior:'existing Java supply, cache, rate, redstone and full-block collision unchanged',
  artwork:'CCD original geometry and pixel textures; palette follows existing CCD booster-pump study'});
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),elements:elements.length}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
