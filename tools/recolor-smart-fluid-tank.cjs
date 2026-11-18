// Pixel-art palette change only: native tank geometry, UVs, glass alpha and CT sheet layout stay intact.
// Usage: node tools/recolor-smart-fluid-tank.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render,model:loadModel}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),ref=path.resolve(process.argv[2]),assets=path.join(root,'src/main/resources/assets/ccd');
const textures=path.join(assets,'textures/block/smart_fluid_tank'),models=path.join(assets,'models/block/smart_fluid_tank'),output=path.join(root,'outputs/smart-fluid-tank');
for(const dir of [textures,models,output])fs.mkdirSync(dir,{recursive:true});
const save=(p,d)=>fs.writeFileSync(p,JSON.stringify(d,null,2)+'\n');
const brass=[[114,71,49],[164,107,71],[175,121,79],[199,151,88],[215,170,94],[228,183,99],[247,203,108],[251,220,125],[255,235,140]];
const thresholds=[35,55,75,90,108,128,150,180];
const names=['fluid_tank','fluid_tank_top','fluid_tank_connected','fluid_tank_top_connected','fluid_tank_inner','fluid_tank_inner_connected','fluid_tank_window','fluid_tank_window_single'];
const moved=(faces,dy)=>faces.map(f=>({...f,vertices:f.vertices.map(v=>[v[0],v[1]+dy,v[2]])}));
function liquid(from,to,rgb){
 const [x,y,z]=from,[X,Y,Z]=to,data=Buffer.alloc(16*16*4);
 for(let v=0;v<16;v++)for(let u=0;u<16;u++)data.set([...rgb.map(c=>c+(u<7&&v<5?12:0)),255],(v*16+u)*4);
 const pixels={data,info:{width:16,height:16,channels:4}},uv=[[0,0],[0,16],[16,16],[16,0]];
 return [
  {vertices:[[x,Y,Z],[x,y,Z],[X,y,Z],[X,Y,Z]],n:[0,0,1]},
  {vertices:[[X,Y,Z],[X,y,Z],[X,y,z],[X,Y,z]],n:[1,0,0]},
  {vertices:[[x,Y,z],[x,Y,Z],[X,Y,Z],[X,Y,z]],n:[0,1,0]},
 ].map(f=>({...f,pixels,uv}));
}
async function main(){
 const changes=[];
 for(const name of names){
  const {data,info}=await sharp(path.join(ref,'assets/create/textures/block',name+'.png')).ensureAlpha().raw().toBuffer({resolveWithObject:true});
  const updated=Buffer.from(data);let count=0;
  for(let i=0;i<data.length;i+=4){
   const [r,g,b,a]=data.subarray(i,i+4);if(!a||r<g||g<b||r-b<20||g-b<8)continue;
   const luminance=.2126*r+.7152*g+.0722*b;let index=thresholds.findIndex(t=>luminance<t);if(index<0)index=8;
   updated.set(brass[index],i);count++;
  }
  for(let i=3;i<data.length;i+=4)if(updated[i]!==data[i])throw Error('Native alpha changed');
  await sharp(updated,{raw:{width:info.width,height:info.height,channels:4}}).png().toFile(path.join(textures,name+'.png'));
  changes.push({name,width:info.width,height:info.height,recoloredPixels:count});
 }
 function variant(name){
  const parent='create:block/fluid_tank/'+name,native=loadModel(parent),overrides={};
  for(const [key,value]of Object.entries(native.textures)){
   const short=value.replace('create:block/','');if(names.includes(short))overrides[key]='ccd:block/smart_fluid_tank/'+short;
  }
  overrides.particle='ccd:block/smart_fluid_tank/fluid_tank';
  return {parent,render_type:'minecraft:cutout_mipped',textures:overrides};
 }
 for(const name of fs.readdirSync(models).filter(n=>n.endsWith('.json')))save(path.join(models,name),variant(name.slice(0,-5)));
 save(path.join(assets,'models/block/smart_fluid_tank.json'),{parent:'ccd:block/smart_fluid_tank/block_single_window'});
 const original=await mesh('create:block/fluid_tank/block_single_window'),single=await mesh('ccd:block/smart_fluid_tank/block_single_window');
 const tower=[];
 for(const [i,name]of ['block_bottom_window','block_middle_window','block_top_window'].entries())tower.push(...moved(await mesh('ccd:block/smart_fluid_tank/'+name),16*i));
 const filled=[...tower,...liquid([1.12,4,1.12],[14.88,15,14.88],[48,106,181]),
  ...liquid([1.12,15,1.12],[14.88,29,14.88],[201,137,44]),...liquid([1.12,29,1.12],[14.88,39,14.88],[141,79,159])];
 const panels=[];
 for(const [i,faces,name,center,scale]of [[0,original,'original',[8,8,8],18.5],[1,single,'single',[8,8,8],18.5],[2,filled,'tower',[8,24,8],9]]){
  const buffer=await render(faces,480,540,[26,18,34],{center,scale});
  await sharp(buffer).png().toFile(path.join(output,name+'.png'));panels.push({input:buffer,left:i*480,top:95});
 }
 const labels=Buffer.from(`<svg width="1440" height="730" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="720" y="40" font-size="29">智能储罐 · 黄铜框架与原生观察窗</text><text x="720" y="78" font-size="16">外框、顶盖与内壁统一黄铜 · 保留原生玻璃与多方块结构</text><text x="240" y="667" font-size="19">Create 原生储罐</text><text x="720" y="667" font-size="19">CCD 智能储罐</text><text x="1200" y="667" font-size="19">三格高 · 多流体分层示意</text><text x="720" y="710" font-size="14" fill="#677070">实际模型离线预览 / 右侧颜色仅为分层示意，游戏内使用实际流体纹理 / 未构建或启动游戏</text></g></svg>`);
 await sharp({create:{width:1440,height:730,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 save(path.join(output,'model.meta.json'),{source:'Create 6.0.10-280',textures:changes,geometry:'24 native parents unchanged',innerAndGlass:'warm inner pixels recolored to brass; non-warm glass pixels unchanged',
  connectedTextures:'native FluidTankCTBehaviour, CTModel and RECTANGLE shifts; only same controller connects',fluidRendering:'existing multi-layer renderer unchanged',artwork:'Create derived palette variants, not original CCD upstream artwork'});
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),textures:changes}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
