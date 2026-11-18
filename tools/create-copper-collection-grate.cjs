// CCD half-height collection bowl, original copper pixels and native Create bottom pipe mouth.
// Usage: node tools/create-copper-collection-grate.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render,model:loadModel}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd');
const reference=path.resolve(process.argv[2]);
const textures=path.join(assets,'textures/block/copper_collection_grate'),output=path.join(root,'outputs/copper-collection-grate');
for(const dir of [textures,output])fs.mkdirSync(dir,{recursive:true});
const save=(p,d)=>fs.writeFileSync(p,JSON.stringify(d,null,2)+'\n');
const copper=[[63,32,22],[96,46,31],[123,61,41],[144,73,49],[167,90,64],[194,107,76],[214,123,91],[227,130,108],[239,156,128]];
const metal=[[40,41,44],[53,52,56],[67,68,69],[79,79,79],[103,97,97],[130,132,124],[159,163,153]];
const clusters=[
 '5667777766665554','6667777666655544','6777776666555443','7777766665554433',
 '7776666555544433','6766665555544333','6666655555444332','6666555554444332',
 '6655555444443332','6555554444433332','6555544444433322','5555444554433322',
 '5554445555433222','5444455554433221','4444555544332211','3333444333222111'];
const paint=(pixels,x,y,rgb)=>pixels.set([...rgb,255],(y*16+x)*4);
const face=(texture,uv)=>({texture:'#'+texture,uv});
function box(name,from,to,texture){
 const [x,y,z]=from,[X,Y,Z]=to,f=uv=>face(texture,uv);
 return {name,from,to,faces:{north:f([16-X,16-Y,16-x,16-y]),south:f([x,16-Y,X,16-y]),
  east:f([16-Z,16-Y,16-z,16-y]),west:f([z,16-Y,Z,16-y]),up:f([x,z,X,Z]),down:f([x,z,X,Z])}};
}
function ring(name,o,O,i,I,y,Y,texture){
 return [box(name+'_north',[o,y,o],[O,Y,i],texture),box(name+'_south',[o,y,I],[O,Y,O],texture),
  box(name+'_west',[o,y,i],[i,Y,I],texture),box(name+'_east',[I,y,i],[O,Y,I],texture)];
}
async function main(){
 const body=Buffer.alloc(1024),inner=Buffer.alloc(1024),grate=Buffer.alloc(1024);
 for(let y=0;y<16;y++)for(let x=0;x<16;x++){
  let c=Number(clusters[y][x]);
  if(x===0||y===0)c=6;else if(x===15||y===15)c=1;
  paint(body,x,y,copper[c]);
  paint(inner,x,y,metal[Math.min(3,Math.floor(Number(clusters[y][x])/2))]);
  paint(grate,x,y,metal[Math.min(6,2+Math.floor(Number(clusters[y][x])/2))]);
 }
 for(const [name,data]of Object.entries({body,inner,grate}))
  await sharp(data,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,name+'.png'));
 const elements=[...ring('floor',2,14,6,10,2,3,'copper'),...ring('lower_bowl',2,14,3,13,3,5,'copper'),
  ...ring('upper_bowl',1,15,3,13,5,7,'copper'),...ring('raised_copper_lip',0,16,2,14,7,8,'copper')];
 // Inner metal liner shares the wall geometry; no overlapping decorative faces.
 for(const e of elements){
  if(e.name.startsWith('floor'))e.faces.up.texture='#inner';
  if(e.name.startsWith('lower_bowl')||e.name.startsWith('upper_bowl')){
   const side=e.name.split('_').at(-1),inside={north:'south',south:'north',west:'east',east:'west'}[side];
   e.faces[inside].texture='#inner';
  }
 }
 for(const x of [3,6,9,12])elements.push(box('recessed_grate_'+x,[x,6,3],[x+1,7,13],'grate'));
 const nativeParts=[];
 for(const part of ['rim_connector','rim']){
  const id='create:block/fluid_pipe/'+part+'/down';nativeParts.push(id);
  for(const [index,source]of loadModel(id).elements.entries()){
   const e=structuredClone(source);e.name='native_'+part+'_down_'+index;
   for(const f of Object.values(e.faces))f.texture='#pipe';elements.push(e);
  }
 }
 const model={parent:'minecraft:block/block',render_type:'minecraft:cutout_mipped',textures:{
  copper:'ccd:block/copper_collection_grate/body',inner:'ccd:block/copper_collection_grate/inner',
  grate:'ccd:block/copper_collection_grate/grate',pipe:'create:block/pipes',particle:'#copper'},elements};
 save(path.join(assets,'models/block/copper_collection_grate.json'),model);
 const faces=await mesh('ccd:block/copper_collection_grate');
 const water=box('preview_water',[3.0625,3.0625,3.0625],[12.9375,5.8125,12.9375],'water');
 const scratch=path.join(assets,'models/block/collection_grate_water_preview.json');let waterFaces;
 try{
  save(scratch,{parent:'minecraft:block/block',textures:{water:'minecraft:block/water_still'},elements:[water]});
  waterFaces=await mesh('ccd:block/collection_grate_water_preview');
 }finally{fs.rmSync(scratch,{force:true});}
 const waterFrame=await sharp(path.join(reference,'assets/minecraft/textures/block/water_still.png'))
  .extract({left:0,top:0,width:16,height:16}).ensureAlpha().raw().toBuffer({resolveWithObject:true});
 const tint=[63,118,228];for(let p=0;p<waterFrame.data.length;p+=4)for(let c=0;c<3;c++)
  waterFrame.data[p+c]=Math.round(waterFrame.data[p+c]*tint[c]/255);
 for(const f of waterFaces)f.pixels=waterFrame;
 const panels=[];
 for(const [i,name,view,fluid]of [[0,'empty',[26,25,34],false],[1,'water',[26,25,34],true],[2,'underside',[26,-20,34],false]]){
  const buffer=await render(fluid?[...faces,...waterFaces]:faces,500,440,view,{center:[8,4,8],scale:20,alphaBlend:fluid});
  await sharp(buffer).png().toFile(path.join(output,name+'.png'));panels.push({input:buffer,left:i*500,top:90});
 }
 const labels=Buffer.from(`<svg width="1500" height="620" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="750" y="40" font-size="29">铜集液栅 · 半格集液斗试版</text><text x="750" y="77" font-size="16">外扩铜框 · 下沉金属栅条 · 两级收口槽体 · 原生底部管口</text><text x="250" y="566" font-size="19">空槽与栅口</text><text x="750" y="566" font-size="19">存液示意</text><text x="1250" y="566" font-size="19">底部排液口</text><text x="750" y="605" font-size="14" fill="#677070">实际模型离线预览 / 液面随库存变化，预览为定格水纹 / 未构建或启动客户端</text></g></svg>`);
 await sharp({create:{width:1500,height:620,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 const swatches=[];for(const [i,name]of ['body','inner','grate'].entries())swatches.push({
  input:await sharp(path.join(textures,name+'.png')).resize(192,192,{kernel:'nearest'}).png().toBuffer(),left:i*208,top:0});
 await sharp({create:{width:608,height:192,channels:4,background:'#e5e8e2'}}).composite(swatches).png().toFile(path.join(output,'textures.png'));
 save(path.join(output,'model.meta.json'),{stage:'half-height bowl study',height:8,barTop:7,lipTop:8,
  innerBounds:[3,13],liquidBounds:{from:water.from,to:water.to},textureSize:[16,16],nativeParts,
  geometry:elements.map(({name,from,to})=>({name,from,to})),
  smart:'inherits geometry, brass override and updated top filter height; front panel styling deferred',
  behavior:'capacity, collection, search and draining unchanged; outline and fluid rendering follow new geometry',
  artwork:'CCD copper/liner/grate textures; native pipe geometry and texture belong to Create, preview water to Mojang'});
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),elements:elements.length}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
