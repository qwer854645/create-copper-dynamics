// Thin CCD copper mounting plate with a straight, native Create pipe coupling.
// Usage: node tools/create-fluid-hatch.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render,model:loadModel}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd');
const ref=path.resolve(process.argv[2]),textures=path.join(assets,'textures/block/fluid_hatch'),output=path.join(root,'outputs/fluid-hatch');
for(const dir of [textures,output])fs.mkdirSync(dir,{recursive:true});
const save=(file,data)=>fs.writeFileSync(file,JSON.stringify(data,null,2)+'\n');
const target=[8,8,4],translation=[0,0,-12];
const imported=[];
function port(){
 const elements=[];
 for(const part of ['rim_connector','rim']){
  const id='create:block/fluid_pipe/'+part+'/south';imported.push(id);
  for(const original of loadModel(id).elements){
   const e=JSON.parse(JSON.stringify(original));e.name='native_pipe_'+part;
   e.from=e.from.map((v,i)=>v+translation[i]);e.to=e.to.map((v,i)=>v+translation[i]);
   if(e.rotation)e.rotation.origin=e.rotation.origin.map((v,i)=>v+translation[i]);
   for(const [side,face]of Object.entries(e.faces)){
    face.texture=part==='rim'&&side==='south'?'#port':'#pipe';
    // The mouth sits inside the block: never cull it against the next block.
    delete face.cullface;
   }
   elements.push(e);
  }
 }
 return elements;
}
async function main(){
 // Reuse the established 16x16 CCD pixel clusters; a one-pixel folded edge defines the plate.
 const {data,info}=await sharp(path.join(assets,'textures/block/faucet/body.png')).ensureAlpha().raw().toBuffer({resolveWithObject:true});
 if(info.width!==16||info.height!==16)throw Error('Unexpected CCD copper texture');
 const plate=Buffer.from(data);
 for(let y=2;y<14;y++)for(let x=2;x<14;x++){
  if(x===2||y===2)plate.set([214,123,91,255],(y*16+x)*4);
  if(x===13||y===13)plate.set([123,61,41,255],(y*16+x)*4);
 }
 await sharp(plate,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,'plate.png'));
 const face=uv=>({texture:'#plate',uv});
 const mountingPlate={name:'thin_copper_plate',from:[2,2,0],to:[14,14,2],faces:{
  north:face([14,2,2,14]),south:face([2,2,14,14]),
  east:face([2,2,4,14]),west:face([2,2,4,14]),
  up:face([2,2,14,4]),down:face([2,2,14,4])}};
 const closed={parent:'create:block/item_hatch/block_closed',render_type:'minecraft:cutout_mipped',textures:{
  plate:'ccd:block/fluid_hatch/plate',pipe:'create:block/pipes',
  port:'create:block/pipes',particle:'ccd:block/fluid_hatch/plate'},elements:[mountingPlate,...port()]};
 save(path.join(assets,'models/block/fluid_hatch_closed.json'),closed);
 for(const prefix of ['', 'smart_']){
  const variants={};
  for(const [facing,y]of [['north',0],['east',90],['south',180],['west',270]])
   variants['facing='+facing]={model:'ccd:block/'+prefix+'fluid_hatch_closed',...(y?{y}:{})};
  save(path.join(assets,'blockstates/'+prefix+'fluid_hatch.json'),{variants});
 }
 const panels=[];
 for(const [i,id,name,view]of [
  [0,'create:block/item_hatch/block_closed','original',[24,20,34]],
  [1,'ccd:block/fluid_hatch_closed','front',[24,20,34]],
  [2,'ccd:block/fluid_hatch_closed','side',[-34,16,24]],
 ]){
  const buffer=await render(await mesh(id),480,510,view,{center:[8,8,4],scale:25});
  await sharp(buffer).png().toFile(path.join(output,name+'.png'));
  panels.push({input:buffer,left:i*480,top:95});
 }
 const labels=Buffer.from(`<svg width="1440" height="700" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="720" y="40" font-size="29">流体舱口 · 薄铜板与直向管口</text><text x="720" y="78" font-size="16">12×12 薄铜板 · 原生流体管口 · 无斜面、无盖板</text><text x="240" y="643" font-size="19">Create 物品舱口</text><text x="720" y="643" font-size="19">CCD · 正面</text><text x="1200" y="643" font-size="19">CCD · 侧面</text><text x="720" y="683" font-size="14" fill="#677070">实际模型离线预览 / 交互保留音效，不切换模型或安排复位 tick / 未构建或启动游戏</text></g></svg>`);
 await sharp({create:{width:1440,height:700,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 await sharp(path.join(textures,'plate.png')).resize(320,320,{kernel:'nearest'}).png().toFile(path.join(output,'textures.png'));
 save(path.join(output,'model.meta.json'),{source:'Create 6.0.10-280',copperPlate:{from:[2,2,0],to:[14,14,2]},nativeParts:imported,
  textures:{plate:{size:[16,16],source:'CCD faucet/body with one-pixel folded edges'}},
  portFrontCenter:target,portRotation:'none',nativePortUVs:'unchanged',nativePortCullFaces:'removed for internal straight mount',
  openState:'inherited property unused; all states use one fixed model; no interaction state changes or scheduled reset',
  preserved:['placement','four horizontal rotations','waterlogging','fluid proxy','container interaction','smart hatch geometry and filter'],
  artwork:'CCD plate geometry and pixel texture; native Create pipe geometry, UVs, textures and item display transforms'});
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),elements:closed.elements.length}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
