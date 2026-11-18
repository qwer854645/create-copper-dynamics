// Reuse the copper faucet geometry, Create's brass palette and its native smart-pipe filter face.
// Usage: node tools/create-smart-faucet.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render,model:loadModel}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd');
const textures=path.join(assets,'textures/block/smart_faucet'),output=path.join(root,'outputs/smart-faucet');
for(const dir of [textures,output])fs.mkdirSync(dir,{recursive:true});
const copper=[[96,46,31],[144,73,49],[154,80,56],[167,90,64],[178,98,71],[194,107,76],[214,123,91],[227,130,108]];
const brass=[[114,71,49],[158,105,71],[175,121,79],[191,142,85],[206,160,90],[228,183,99],[247,203,108],[255,235,140]];
const palette=new Map(copper.map((color,i)=>[color.join(','),brass[i]]));
const nativeFilter=loadModel('create:block/smart_fluid_pipe/block').elements[2];
// Expand by 0.1 around the nominal six-unit panel to avoid coplanar collar/neck faces.
const panel={name:'front_filter_panel',from:[4.9,4.9,9.8],to:[11.1,11.1,11.2],faces:{}};
// Reuse the native filter bezel's face UVs, adapted from its north-facing plate to a front-facing mount.
for(const [face,source]of Object.entries(nativeFilter.faces)){
 const target=face==='north'?'south':face==='south'?'north':face;
 panel.faces[target]={...source,texture:'#filter'};
}
const save=(file,m)=>fs.writeFileSync(file,JSON.stringify(m,null,2)+'\n');
async function main(){
 const {data,info}=await sharp(path.join(assets,'textures/block/faucet/body.png')).ensureAlpha().raw().toBuffer({resolveWithObject:true});
 const recolored=Buffer.from(data);
 for(let i=0;i<data.length;i+=4){
  const color=palette.get([...data.subarray(i,i+3)].join(','));if(!color)throw Error('Unexpected copper palette pixel');
  recolored.set(color,i);
 }
 await sharp(recolored,{raw:{width:info.width,height:info.height,channels:4}}).png().toFile(path.join(textures,'body.png'));
 for(const state of ['closed','open'])save(path.join(assets,'models/block/smart_faucet_'+state+'.json'),{
  parent:'ccd:block/faucet_'+state,textures:{body:'ccd:block/smart_faucet/body',base:'ccd:block/smart_faucet/body',particle:'ccd:block/smart_faucet/body'}});
 save(path.join(assets,'models/block/smart_faucet_filter.json'),{parent:'minecraft:block/block',render_type:'minecraft:cutout_mipped',
  textures:{filter:'create:block/smart_pipe_2'},elements:[panel]});
 const baseStates=JSON.parse(fs.readFileSync(path.join(assets,'blockstates/faucet.json'),'utf8')).variants;
 const multipart=Object.entries(baseStates).map(([condition,apply])=>({
  when:Object.fromEntries(condition.split(',').map(part=>part.split('='))),
  apply:{...apply,model:apply.model.replace('ccd:block/faucet_','ccd:block/smart_faucet_')},
 }));
 for(const [facing,y]of [['south',0],['north',180],['east',270],['west',90]])multipart.push({when:{facing},
  apply:{model:'ccd:block/smart_faucet_filter',...(y?{y}:{})}});
 save(path.join(assets,'blockstates/smart_faucet.json'),{multipart});
 const closed=loadModel('ccd:block/smart_faucet_closed');
 save(path.join(assets,'models/item/smart_faucet.json'),{parent:'ccd:block/smart_faucet_closed',render_type:'minecraft:cutout_mipped',
  textures:{filter:'create:block/smart_pipe_2'},display:{gui:{rotation:[30,45,0],scale:[.625,.625,.625]}},elements:[...closed.elements,panel]});
 const panels=[];
 for(const [i,state,view]of [[0,'closed',[24,18,34]],[1,'open',[24,18,34]],[2,'open',[-24,18,-34]]]){
  const faces=[...await mesh('ccd:block/smart_faucet_'+state),...await mesh('ccd:block/smart_faucet_filter')];
  const buffer=await render(faces,500,480,view,{center:[8,7.5,7],scale:28});
  await sharp(buffer).toFile(path.join(output,['closed','open','rear'][i]+'.png'));
  panels.push({input:buffer,left:i*500,top:100});
 }
 const labels=Buffer.from(`<svg width="1500" height="660" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="750" y="42" font-size="29">智能龙头 · 黄铜弯管与原生过滤面板</text><text x="750" y="78" font-size="16">沿用铜龙头结构 · 正面配置过滤 · 原生背部管口与分液池喷嘴</text><text x="250" y="611" font-size="18">关闭</text><text x="750" y="611" font-size="18">开启</text><text x="1250" y="611" font-size="18">背部接口</text><text x="750" y="648" font-size="14" fill="#677070">实际模型离线渲染 / 过滤未设置，游戏内原生渲染器会在面板中央显示过滤图标</text></g></svg>`);
 await sharp({create:{width:1500,height:660,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 await sharp(path.join(textures,'body.png')).resize(256,256,{kernel:'nearest'}).png().toFile(path.join(output,'texture.png'));
 save(path.join(output,'model.meta.json'),{originalArtwork:'brass recoloring of CCD faucet body only',textureSize:[16,16],paletteReference:'Create brass_block',
  sharedGeometry:['ccd:block/faucet_closed','ccd:block/faucet_open'],filterTexture:'create:block/smart_pipe_2',
  filterBox:[panel.from,panel.to],filterFaceCenter:[8,8,11.2],filterInteractionCenter:[8,8,11.25],filterScale:.375,
  filterSides:'front only, per user choice',nozzleAndPipeUnchanged:true,processingLogicUnchanged:true});
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),textureSize:[info.width,info.height],filter:'front only'}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
