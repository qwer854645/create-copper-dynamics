// Native Create hatch silhouette with copper material and an angled native pipe coupling.
// Usage: node tools/create-fluid-hatch.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render,model:loadModel}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd');
const ref=path.resolve(process.argv[2]),textures=path.join(assets,'textures/block/fluid_hatch'),output=path.join(root,'outputs/fluid-hatch');
for(const dir of [textures,output])fs.mkdirSync(dir,{recursive:true});
const save=(file,data)=>fs.writeFileSync(file,JSON.stringify(data,null,2)+'\n');
const copper=[[63,32,22],[96,46,31],[123,61,41],[144,73,49],[167,90,64],[194,107,76],[214,123,91],[227,130,108],[239,156,128]];
const thresholds=[42,58,75,90,108,130,155,180];
const source=name=>path.join(ref,'assets/create/textures/block',name+'.png');
const target=[8,11.4,5.9],translation=target.map((v,i)=>v-[8,8,16][i]);
const imported=[];
function port(){
 const elements=[];
 for(const part of ['rim_connector','rim']){
  const id='create:block/fluid_pipe/'+part+'/south';imported.push(id);
  for(const original of loadModel(id).elements){
   const e=JSON.parse(JSON.stringify(original));e.name='native_pipe_'+part;
   e.from=e.from.map((v,i)=>v+translation[i]);e.to=e.to.map((v,i)=>v+translation[i]);
   e.rotation={axis:'x',angle:-45,origin:target};
   for(const [side,face]of Object.entries(e.faces)){
    face.texture=part==='rim'&&side==='south'?'#port':'#pipe';
    // This mouth is inside the hatch block, rather than on its neighbour boundary.
    delete face.cullface;
   }
   elements.push(e);
  }
 }
 return elements;
}
async function main(){
 const native=await sharp(source('hatch')).ensureAlpha().raw().toBuffer({resolveWithObject:true});
 if(native.info.width!==32||native.info.height!==32)throw Error('Unexpected native hatch atlas');
 const frame=Buffer.from(native.data);let recolored=0;
 for(let y=0;y<32;y++)for(let x=0;x<32;x++){
  const i=(y*32+x)*4,[r,g,b,a]=native.data.subarray(i,i+4);
  // Keep the deep recesses and the small original lower latch hardware neutral.
  if(!a||(x>=7&&x<=10&&y>=20&&y<=23))continue;
  const value=.2126*r+.7152*g+.0722*b;if(value<42)continue;
  let index=thresholds.findIndex(t=>value<t);if(index<0)index=8;
  frame.set(copper[index],i);recolored++;
 }
 for(let i=3;i<frame.length;i+=4)if(frame[i]!==native.data[i])throw Error('Native hatch alpha changed');
 await sharp(frame,{raw:{width:32,height:32,channels:4}}).png().toFile(path.join(textures,'frame.png'));
 const pipe=await sharp(source('pipes')).ensureAlpha().raw().toBuffer({resolveWithObject:true});
 if(pipe.info.width!==32||pipe.info.height!==32)throw Error('Unexpected native pipe atlas');
 const cap=Buffer.from(pipe.data);let shutterPixels=0;
 for(let y=22;y<32;y++)for(let x=22;x<32;x++){
  const i=(y*32+x)*4;
  // Only the native dark bore changes; the copper rim stays byte-identical.
  if(pipe.data[i]!==61||pipe.data[i+1]!==22||pipe.data[i+2]!==30)continue;
  const u=x-22,v=y-22;
  const rgb=v===4?[53,52,56]:v<4?[159,163,153]:u<5?[103,108,105]:[79,84,83];
  cap.set(rgb,i);shutterPixels++;
 }
 if(shutterPixels!==32)throw Error('Unexpected native pipe bore mask');
 await sharp(cap,{raw:{width:32,height:32,channels:4}}).png().toFile(path.join(textures,'pipe_cap.png'));
 const nativeModel=loadModel('create:block/item_hatch/block_closed');
 const elements=JSON.parse(JSON.stringify(nativeModel.elements));
 const portElements=port();
 const closed={parent:'create:block/item_hatch/block_closed',render_type:'minecraft:cutout_mipped',textures:{
  '0':'ccd:block/fluid_hatch/frame','1':'ccd:block/faucet/body',pipe:'create:block/pipes',
  port:'ccd:block/fluid_hatch/pipe_cap',particle:'ccd:block/fluid_hatch/frame'},elements:[...elements,...portElements]};
 save(path.join(assets,'models/block/fluid_hatch_closed.json'),closed);
 save(path.join(assets,'models/block/fluid_hatch_open.json'),{parent:'ccd:block/fluid_hatch_closed',textures:{port:'create:block/pipes'}});
 // Keep the smart hatch on its previous native geometry until its own art pass.
 save(path.join(assets,'models/block/smart_fluid_hatch_closed.json'),{parent:'create:block/item_hatch/block_closed',
  render_type:'minecraft:cutout_mipped',textures:{'0':'create:block/brass_casing',particle:'create:block/brass_casing'}});
 const panels=[];
 for(const [i,id,name,view]of [
  [0,'create:block/item_hatch/block_closed','original',[24,20,34]],
  [1,'ccd:block/fluid_hatch_closed','closed',[24,20,34]],
  [2,'ccd:block/fluid_hatch_open','open',[24,20,34]],
  [3,'ccd:block/fluid_hatch_open','side',[-34,16,24]],
 ]){
  const buffer=await render(await mesh(id),480,510,view,{center:[8,8,4],scale:25});
  await sharp(buffer).png().toFile(path.join(output,name+'.png'));
  panels.push({input:buffer,left:i*480,top:95});
 }
 const labels=Buffer.from(`<svg width="1920" height="700" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="960" y="40" font-size="29">流体舱口 · 铜框与斜面管口</text><text x="960" y="78" font-size="16">沿用原生舱口轮廓 · 复用原生管口 · 灰色内盖 / 深色开口</text><text x="240" y="643" font-size="19">Create 物品舱口</text><text x="720" y="643" font-size="19">CCD · 关闭</text><text x="1200" y="643" font-size="19">CCD · 交互开启</text><text x="1680" y="643" font-size="19">侧面</text><text x="960" y="683" font-size="14" fill="#677070">实际模型离线预览 / 开合沿用原生贴图切换；流体代理和定时复位逻辑不变 / 未构建或启动游戏</text></g></svg>`);
 await sharp({create:{width:1920,height:700,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 const swatches=[];
 for(const [i,name]of ['frame','pipe_cap'].entries())swatches.push({input:await sharp(path.join(textures,name+'.png')).resize(320,320,{kernel:'nearest'}).png().toBuffer(),left:i*336,top:0});
 await sharp({create:{width:656,height:320,channels:4,background:'#e5e8e2'}}).composite(swatches).png().toFile(path.join(output,'textures.png'));
 save(path.join(output,'model.meta.json'),{source:'Create 6.0.10-280',nativeHatchElements:elements.length,nativeParts:imported,
  textures:{frame:{size:[32,32],recoloredPixels:recolored,alpha:'unchanged'},pipeCap:{size:[32,32],changedBorePixels:shutterPixels}},
  portFrontCenter:target,portRotation:{axis:'x',angle:-45},nativePortUVs:'unchanged',nativePortCullFaces:'removed for internal angled mount',
  openState:'native timed interaction feedback; does not gate the fluid capability',
  preserved:['placement','four horizontal rotations','waterlogging','collision','fluid proxy','container interaction','timed closing','smart hatch appearance'],
  artwork:'Create hatch/pipe derived textures and geometry; existing CCD copper fascia; upstream artwork remains owned by Create'});
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),elements:closed.elements.length,recoloredPixels:recolored,shutterPixels}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
