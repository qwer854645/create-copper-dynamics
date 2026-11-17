// Pixel-art palette replacement restricted to Create's 8x8 frame UV island.
// Usage: node tools/recolor-fluid-gauge.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),reference=process.argv[2];
const assets=path.join(root,'src/main/resources/assets/ccd');
const textures=path.join(assets,'textures/block/fluid_gauge');
const models=path.join(assets,'models/block/fluid_gauge');
const output=path.join(root,'outputs/fluid-gauge-copper');
for(const dir of [textures,models,output])fs.mkdirSync(dir,{recursive:true});
const palette=new Map([
 ['68,50,42',[96,46,31]],['74,59,52',[123,61,41]],['96,75,64',[144,73,49]],
 ['191,142,85',[167,90,64]],['228,183,99',[178,98,71]],['247,203,108',[194,107,76]],
 ['251,220,125',[214,123,91]],['255,235,140',[227,130,108]],['255,249,191',[239,156,128]],
 ['65,65,80',[167,90,64]],['70,78,97',[194,107,76]],
 ['75,94,113',[214,123,91]],['86,107,129',[239,156,128]],
]);
async function main(){
 const changes=[];
 for(const name of ['factory_panel','factory_panel_packager_mode']){
  const source=path.join(reference,'assets/create/textures/block',name+'.png');
  const {data,info}=await sharp(source).ensureAlpha().raw().toBuffer({resolveWithObject:true});
  const updated=Buffer.from(data);let count=0;
  for(let y=0;y<8;y++)for(let x=0;x<8;x++){
   const i=(y*info.width+x)*4;if(!data[i+3])continue;
   const color=palette.get([...data.subarray(i,i+3)].join(','));
   if(!color)throw Error('Unexpected frame color at '+name+':'+x+','+y);
   updated.set(color,i);count++;
  }
  for(let i=0;i<data.length;i++){
   const pixel=Math.floor(i/4),x=pixel%info.width,y=Math.floor(pixel/info.width);
   if(i%4===3||x>=8||y>=8)if(data[i]!==updated[i])throw Error('Changed a non-frame pixel');
  }
  await sharp(updated,{raw:{width:info.width,height:info.height,channels:4}}).png().toFile(path.join(textures,name+'.png'));
  changes.push({file:name+'.png',width:info.width,height:info.height,recoloredPixels:count});
 }
 for(const [name,texture]of [['panel','factory_panel'],['panel_restocker','factory_panel_packager_mode']])
  fs.writeFileSync(path.join(models,name+'.json'),JSON.stringify({parent:'create:block/factory_gauge/'+name,
   textures:{'0':'ccd:block/fluid_gauge/'+texture,particle:'ccd:block/fluid_gauge/'+texture}},null,2)+'\n');
 fs.writeFileSync(path.join(assets,'models/item/fluid_gauge.json'),JSON.stringify({parent:'create:item/factory_gauge',
  textures:{'0':'ccd:block/fluid_gauge/factory_panel',particle:'ccd:block/fluid_gauge/factory_panel'}},null,2)+'\n');
 const panels=[];
 for(const [row,mode]of [[0,'panel'],[1,'panel_restocker']])for(const [column,namespace]of [[0,'create'],[1,'ccd']]){
  const id=namespace==='create'?'create:block/factory_gauge/'+mode:'ccd:block/fluid_gauge/'+mode;
  const buffer=await render(await mesh(id),500,330,[18,32,-20],{center:[4,1,4],scale:38});
  panels.push({input:buffer,left:column*500,top:100+row*340});
 }
 const labels=Buffer.from(`<svg width="1000" height="820" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei, sans-serif" text-anchor="middle" fill="#303535"><text x="250" y="40" font-size="26">Create 原版</text><text x="750" y="40" font-size="26">流体仪表 · 铜边框</text><text x="500" y="78" font-size="16">仅边框换色 · 上：普通模式　下：补货模式</text><text x="500" y="802" font-size="14">原生模型离线预览 / 表盘、背板、指示灯及透明度保留</text></g></svg>`);
 await sharp({create:{width:1000,height:820,channels:4,background:'#e5e8e2'}})
  .composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'comparison.png'));
 fs.writeFileSync(path.join(output,'palette.json'),JSON.stringify({source:'Create 6.0.10-280',
  region:[0,0,8,8],palette:Object.fromEntries(palette),textures:changes},null,2)+'\n');
 console.log(JSON.stringify({preview:path.join(output,'comparison.png'),textures:changes}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
