// Copper collection bowl with a brass top lip and Create's native brass filter bezel.
// Usage: node tools/create-smart-collection-grate.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render,model:loadModel}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd');
const reference=path.resolve(process.argv[2]);
const textures=path.join(assets,'textures/block/smart_collection_grate'),output=path.join(root,'outputs/smart-collection-grate');
for(const dir of [textures,output])fs.mkdirSync(dir,{recursive:true});
const save=(p,d)=>fs.writeFileSync(p,JSON.stringify(d,null,2)+'\n');
const copper=[[70,32,24],[144,73,49],[154,80,56],[167,90,64],[178,98,71],[194,107,76],[200,116,86],[214,123,91],[227,130,108]];
const brass=[[70,46,30],[158,105,71],[175,121,79],[191,142,85],[206,160,90],[228,183,99],[238,193,103],[247,203,108],[255,235,140]];
const palette=new Map(copper.map((color,i)=>[color.join(','),brass[i]]));
const sourcePanel=loadModel('create:block/smart_fluid_pipe/block').elements[2];
const panel={name:'front_filter_panel',from:[5.5,2,14],to:[10.5,7,15.5],faces:{}};
const reversed={north:'south',south:'north',east:'west',west:'east',up:'up',down:'down'};
for(const [side,source]of Object.entries(sourcePanel.faces))panel.faces[reversed[side]]={...source,texture:'#filter'};
async function main(){
 for(const name of ['body','rim']){
  const {data,info}=await sharp(path.join(assets,'textures/block/copper_collection_grate',name+'.png'))
   .ensureAlpha().raw().toBuffer({resolveWithObject:true});
  const pixels=Buffer.from(data);
  for(let i=0;i<data.length;i+=4){
   const color=palette.get([...data.subarray(i,i+3)].join(','));if(!color)throw Error('Unexpected copper pixel');
   pixels.set(color,i);
  }
  await sharp(pixels,{raw:{width:info.width,height:info.height,channels:4}}).png().toFile(path.join(textures,name+'.png'));
 }
 const shared=loadModel('ccd:block/copper_collection_grate');
 const smartElements=structuredClone(shared.elements);
 for(const e of smartElements.filter(e=>e.name.startsWith('raised_copper_lip')))
  for(const f of Object.values(e.faces))if(f.texture==='#copper')f.texture='#brass';
 save(path.join(assets,'models/block/smart_collection_grate.json'),{parent:'ccd:block/copper_collection_grate',textures:{
  brass:'ccd:block/smart_collection_grate/body',rim:'ccd:block/smart_collection_grate/rim',
  particle:'ccd:block/copper_collection_grate/body'},elements:smartElements});
 save(path.join(assets,'models/block/smart_collection_grate_filter.json'),{parent:'minecraft:block/block',render_type:'minecraft:cutout_mipped',
  textures:{filter:'create:block/smart_pipe_2'},elements:[panel]});
 const multipart=[];
 for(const [facing,y]of [['south',0],['north',180],['east',270],['west',90]])
  for(const model of ['ccd:block/smart_collection_grate','ccd:block/smart_collection_grate_filter'])
   multipart.push({when:{facing},apply:{model,...(y?{y}:{})}});
 save(path.join(assets,'blockstates/smart_collection_grate.json'),{multipart});
 const body=loadModel('ccd:block/smart_collection_grate');
 save(path.join(assets,'models/item/smart_collection_grate.json'),{parent:'ccd:block/smart_collection_grate',
  textures:{filter:'create:block/smart_pipe_2'},elements:[...body.elements,panel]});
 const faces=[...await mesh('ccd:block/smart_collection_grate'),...await mesh('ccd:block/smart_collection_grate_filter')];
 const water={name:'preview_water',from:[3.0625,3.0625,3.0625],to:[12.9375,5.8125,12.9375],faces:{}};
 for(const [side,uv]of Object.entries({north:[3.0625,10.1875,12.9375,12.9375],south:[3.0625,10.1875,12.9375,12.9375],
  east:[3.0625,10.1875,12.9375,12.9375],west:[3.0625,10.1875,12.9375,12.9375],
  up:[3.0625,3.0625,12.9375,12.9375],down:[3.0625,3.0625,12.9375,12.9375]}))water.faces[side]={texture:'#water',uv};
 const scratch=path.join(assets,'models/block/smart_collection_grate_water_preview.json');let waterFaces;
 try{
  save(scratch,{parent:'minecraft:block/block',textures:{water:'minecraft:block/water_still'},elements:[water]});
  waterFaces=await mesh('ccd:block/smart_collection_grate_water_preview');
 }finally{fs.rmSync(scratch,{force:true});}
 const waterFrame=await sharp(path.join(reference,'assets/minecraft/textures/block/water_still.png'))
  .extract({left:0,top:0,width:16,height:16}).ensureAlpha().raw().toBuffer({resolveWithObject:true});
 const tint=[63,118,228];for(let i=0;i<waterFrame.data.length;i+=4)for(let c=0;c<3;c++)
  waterFrame.data[i+c]=Math.round(waterFrame.data[i+c]*tint[c]/255);
 for(const f of waterFaces)f.pixels=waterFrame;
 const panels=[];
 for(const [i,name,view,fluid]of [[0,'front',[20,15,38],false],[1,'water',[26,25,34],true],[2,'rear',[-26,25,-34],false]]){
  const buffer=await render(fluid?[...faces,...waterFaces]:faces,500,440,view,{center:[8,4,8],scale:20,alphaBlend:fluid});
  await sharp(buffer).png().toFile(path.join(output,name+'.png'));panels.push({input:buffer,left:i*500,top:90});
 }
 const labels=Buffer.from(`<svg width="1500" height="620" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="750" y="40" font-size="29">智能集液栅 · 铜槽体与黄铜边框</text><text x="750" y="77" font-size="16">铜壳为主 · 顶部边框和过滤槽保留黄铜 · 灰色栅条</text><text x="250" y="566" font-size="19">正面过滤面板</text><text x="750" y="566" font-size="19">存液示意</text><text x="1250" y="566" font-size="19">背面</text><text x="750" y="605" font-size="14" fill="#677070">实际模型离线预览 / 当前未设置过滤，游戏内由原生行为显示过滤图标 / 未构建或启动客户端</text></g></svg>`);
 await sharp({create:{width:1500,height:620,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 const comparison=[];
 for(const [i,part]of [await mesh('ccd:block/copper_collection_grate'),faces].entries())comparison.push({
  input:await render(part,500,440,[20,18,38],{center:[8,4,8],scale:20}),left:i*500,top:50});
 const comparisonLabels=Buffer.from(`<svg width="1000" height="550" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" fill="#303535" text-anchor="middle"><text x="500" y="35" font-size="24">集液栅 · 铜壳为主，黄铜区分智能版</text><text x="250" y="505" font-size="19">普通版 · 铜壳</text><text x="750" y="505" font-size="19">智能版 · 黄铜顶框与过滤槽</text><text x="500" y="538" font-size="13" fill="#677070">同角度实际模型离线预览 / 铜面沿用 CCD 增压泵原贴图布局，灰色栅条复用 Create 工业铁材质</text></g></svg>`);
 await sharp({create:{width:1000,height:550,channels:4,background:'#e5e8e2'}}).composite([...comparison,{input:comparisonLabels,left:0,top:0}]).png().toFile(path.join(output,'comparison.png'));
 await sharp(path.join(textures,'body.png')).resize(256,256,{kernel:'nearest'}).png().toFile(path.join(output,'texture.png'));
 save(path.join(output,'model.meta.json'),{textureSize:[16,16],base:'ccd:block/copper_collection_grate',panel:[panel.from,panel.to],
  materials:{body:'copper',topLip:'brass on all faces of the four lip elements',filter:'native brass bezel',bars:'native grey iron'},
  panelFaceCenter:[8,4.5,15.5],interactionCenter:[8,4.5,15.5625],filterScale:.25,
  filter:'native FilteringBehaviour.forFluids and ValueBoxTransform.Sided, front only',
  facingRotations:{south:0,north:180,east:270,west:90},capacity:2000,
  unchanged:['collection','search','redstone pause','native bottom pipe','fluid rendering'],
  artwork:'CCD brass recoloring of the approved booster-pump copper pixels; iron bars/liner, filter bezel UVs/texture and pipe belong to Create'});
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),filter:'front only',facings:4}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
