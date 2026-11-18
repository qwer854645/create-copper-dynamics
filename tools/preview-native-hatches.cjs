// Read current model references; never regenerate artwork.
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const out=path.resolve(__dirname,'../outputs/native-hatches');fs.mkdirSync(out,{recursive:true});
 const panels=[];
 for(const [i,id]of ['ccd:block/fluid_hatch_closed','ccd:block/smart_fluid_hatch_closed'].entries()){
  const image=await render(await mesh(id),500,460,[26,18,-34],{center:[8,8,4],scale:20});
  panels.push({input:image,left:i*500,top:50});
 }
 const labels=Buffer.from('<svg width="1000" height="570" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" fill="#303535" text-anchor="middle"><text x="500" y="32" font-size="24">直接复用 Create 物品舱口模型与贴图</text><text x="250" y="535" font-size="20">流体舱口</text><text x="750" y="535" font-size="20">智能流体舱口</text><text x="500" y="560" font-size="14">当前外观相同 · 保留各自流体功能 · 离线预览</text></g></svg>');
 await sharp({create:{width:1000,height:570,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(out,'preview.png'));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
