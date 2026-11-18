// Copper-framed Conduit Box. Art uses integer pixels; native conduit textures are referenced.
// Usage: node tools/create-copper-water-supply.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd');
const reference=path.resolve(process.argv[2]);
const textures=path.join(assets,'textures/block/copper_water_supply'),output=path.join(root,'outputs/copper-water-supply');
for(const dir of [textures,output])fs.mkdirSync(dir,{recursive:true});
const save=(p,d)=>fs.writeFileSync(p,JSON.stringify(d,null,2)+'\n');
const copper=[[63,32,22],[96,46,31],[123,61,41],[144,73,49],[167,90,64],[194,107,76],[214,123,91],[227,130,108],[239,156,128]];
const clusters=[
 '5667777766665554','6667777666655544','6777776666555443','7777766665554433',
 '7776666555544433','6766665555544333','6666655555444332','6666555554444332',
 '6655555444443332','6555554444433332','6555544444433322','5555444554433322',
 '5554445555433222','5444455554433221','4444555544332211','3333444333222111'];
const paint=(data,x,y,rgb)=>data.set([...rgb,255],(y*16+x)*4);
const face=(texture,uv)=>({texture:'#'+texture,uv});
function box(name,from,to,texture){
 const [x,y,z]=from,[X,Y,Z]=to,f=uv=>face(texture,uv);
 return {name,from,to,faces:{north:f([16-X,16-Y,16-x,16-y]),south:f([x,16-Y,X,16-y]),
  east:f([16-Z,16-Y,16-z,16-y]),west:f([z,16-Y,Z,16-y]),up:f([x,z,X,Z]),down:f([x,z,X,Z])}};
}
// CubeListBuilder texOffs(0,0), addBox(-size/2,...), normalized to JSON UV units.
function nativeCube(name,size,texture,width,height,scale=1){
 const half=size*scale/2,e=box(name,[8-half,8-half,8-half],[8+half,8+half,8+half],texture);
 const s=size,u=n=>n/width*16,v=n=>n/height*16;
 const coords={west:[0,s,s,2*s],north:[s,s,2*s,2*s],east:[2*s,s,3*s,2*s],south:[3*s,s,4*s,2*s],
  down:[s,0,2*s,s],up:[2*s,s,3*s,0]};
 for(const [side,[x,y,X,Y]]of Object.entries(coords))e.faces[side].uv=[u(x),v(y),u(X),v(Y)];
 return e;
}
async function main(){
 if(clusters.length!==16||clusters.some(row=>row.length!==16))throw Error('Invalid pixel cluster rows');
 const body=Buffer.alloc(1024),cap=Buffer.alloc(1024),glass=Buffer.alloc(1024);
 for(let y=0;y<16;y++)for(let x=0;x<16;x++){
  let shade=Number(clusters[y][x]);
  if(x===0||y===0)shade=5;else if(x===15||y===15)shade=1;
  paint(body,x,y,copper[shade]);
  let top=Math.min(7,Number(clusters[y][x])+1);
  if(x===0||y===0)top=6;else if(x===15||y===15)top=2;
  if((x===3||y===3||x===12||y===12)&&x>=3&&x<=12&&y>=3&&y<=12)top=3;
  paint(cap,x,y,copper[top]);
 }
 // Clear centres with a few stepped reflection marks, leaving the conduit readable.
 for(const [x,y]of [[3,4],[3,5],[4,3],[4,4],[5,3],[10,12],[11,11],[12,10]])
  paint(glass,x,y,[187,221,219]);
 for(let i=2;i<=13;i++)for(const [x,y]of [[i,2],[i,13],[2,i],[13,i]])paint(glass,x,y,[115,164,166]);
 for(const [name,data]of Object.entries({body,cap,glass}))
  await sharp(data,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,name+'.png'));
 const bottom=box('copper_base',[0,0,0],[16,2,16],'body');bottom.faces.up.texture='#cap';
 const top=box('copper_top',[0,14,0],[16,16,16],'body');top.faces.up.texture='#cap';
 const elements=[bottom,top];
 for(const x of [0,14])for(const z of [0,14])elements.push(box('copper_post_'+x+'_'+z,[x,2,z],[x+2,14,z+2],'body'));
 elements.push(box('north_glass',[2,2,.75],[14,14,1],'glass'),box('south_glass',[2,2,15],[14,14,15.25],'glass'),
  box('west_glass',[.75,2,2],[1,14,14],'glass'),box('east_glass',[15,2,2],[15.25,14,14],'glass'));
 const material={body:'ccd:block/copper_water_supply/body',cap:'ccd:block/copper_water_supply/cap',
  glass:'ccd:block/copper_water_supply/glass',particle:'ccd:block/copper_water_supply/body'};
 const base={parent:'minecraft:block/block',render_type:'minecraft:cutout',ambientocclusion:false,textures:material,elements};
 save(path.join(assets,'models/block/copper_water_supply.json'),base);
 save(path.join(assets,'blockstates/copper_water_supply.json'),{variants:{'':{model:'ccd:block/copper_water_supply'}}});
 const cage=nativeCube('native_active_cage',8,'cage',32,16,.55);
 cage.rotation={origin:[8,8,8],axis:'y',angle:22.5};
 const eyes=[];
 for(const side of ['north','south','west','east']){
  const e=box('native_eye_'+side,[6.5333,6.5333,6.5333],[9.4667,9.4667,9.4667],'eye');
  e.faces={[side]:face('eye',[0,0,8,8])};eyes.push(e);
 }
 const coreMaterial={...material,cage:'minecraft:entity/conduit/cage',eye:'minecraft:entity/conduit/open_eye'};
 save(path.join(assets,'models/item/copper_water_supply.json'),{...base,textures:coreMaterial,elements:[...elements,cage,...eyes]});

 // The world renderer delegates to ConduitRenderer. Preview shows a frozen native animation frame.
 const shellFaces=await mesh('ccd:block/copper_water_supply'),itemFaces=await mesh('ccd:item/copper_water_supply');
 const cageFaces=itemFaces.slice(shellFaces.length,shellFaces.length+6),eyeFaces=itemFaces.slice(shellFaces.length+6);
 const wind=nativeCube('native_wind_preview',16,'wind',64,32,.55);
 const scratch=path.join(assets,'models/block/copper_water_supply_wind_preview.json');
 let windFaces;
 try{
  save(scratch,{parent:'minecraft:block/block',textures:{wind:'minecraft:entity/conduit/wind'},elements:[wind]});
  windFaces=await mesh('ccd:block/copper_water_supply_wind_preview');
 }finally{fs.rmSync(scratch,{force:true});}
 const windFrame=await sharp(path.join(reference,'assets/minecraft/textures/entity/conduit/wind.png'))
  .extract({left:0,top:32*8,width:64,height:32}).ensureAlpha().raw().toBuffer({resolveWithObject:true});
 for(const f of windFaces)f.pixels=windFrame;
 const panels=[];
 for(const [i,name,view]of [[0,'front',[16,12,42]],[1,'rear',[-16,12,-42]],[2,'top',[14,38,22]]]){
  // One eye is billboarded toward the same camera as the native world renderer.
  const unit=view.map(v=>v/Math.hypot(...view)),right=[unit[2],0,-unit[0]];
  const len=Math.hypot(...right);for(let k=0;k<3;k++)right[k]/=len;
  const up=[unit[1]*right[2],unit[2]*right[0]-unit[0]*right[2],-unit[1]*right[0]];
  const eye={...eyeFaces[0],n:unit,uv:[[0,0],[0,8],[8,8],[8,0]],
   vertices:[[-1,1],[-1,-1],[1,-1],[1,1]].map(([x,y])=>[0,1,2].map(k=>8+1.4667*(right[k]*x+up[k]*y)))};
  const buffer=await render([...shellFaces,...cageFaces,...windFaces,eye],500,510,view,{center:[8,8,8],scale:20});
  await sharp(buffer).png().toFile(path.join(output,name+'.png'));panels.push({input:buffer,left:i*500,top:95});
 }
 const labels=Buffer.from(`<svg width="1500" height="700" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="750" y="40" font-size="29">潮涌箱 · 铜框与四面玻璃</text><text x="750" y="78" font-size="16">铜底座与顶框 · 通透观察窗 · 中央悬浮的原版激活核心</text><text x="250" y="647" font-size="19">正面</text><text x="750" y="647" font-size="19">背面</text><text x="1250" y="647" font-size="19">顶面</text><text x="750" y="683" font-size="14" fill="#677070">离线预览为原版核心的定格示意 / 游戏内复用原版旋转、水流和眼睛渲染 / 未构建或启动客户端</text></g></svg>`);
 await sharp({create:{width:1500,height:700,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 const swatches=[];for(const [i,name]of ['body','cap','glass'].entries())swatches.push({
  input:await sharp(path.join(textures,name+'.png')).resize(192,192,{kernel:'nearest'}).png().toBuffer(),left:i*208,top:0});
 await sharp({create:{width:608,height:192,channels:4,background:'#e5e8e2'}}).composite(swatches).png().toFile(path.join(output,'textures.png'));
 save(path.join(output,'model.meta.json'),{stage:'Conduit Box',textureSize:[16,16],frameElements:6,glassElements:4,
  geometry:elements.map(({name,from,to})=>({name,from,to})),coreScale:.55,
  core:'native ConduitRenderer, active and open eye, presentation only; static native cage and four eyes in item model',
  behavior:'supply, cache, rate, redstone and collision unchanged; adjacent-container interfaces deferred',
  artwork:'CCD original copper/glass pixels and frame; native conduit geometry, animation and textures belong to Mojang'});
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),frameElements:elements.length}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
