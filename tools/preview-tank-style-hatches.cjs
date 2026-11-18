// Read-only render of actual current models and new textures.
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const out=path.resolve(__dirname,'../outputs/tank-style-hatches');fs.mkdirSync(out,{recursive:true});
 const panels=[];
 for(const [row,id]of ['ccd:block/fluid_hatch_closed','ccd:block/smart_fluid_hatch_closed'].entries()){
  const faces=await mesh(id);
  for(const [col,view]of [[0,[0,8,40]],[1,[26,18,34]],[2,[26,18,-34]]])
   panels.push({input:await render(faces,420,380,view,{center:[8,8,4],scale:17}),left:col*420,top:55+row*420});
 }
 const labels=Buffer.from('<svg width="1260" height="930" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" fill="#303535" text-anchor="middle"><text x="630" y="32" font-size="24">流体舱口 · 储罐与黄铜材质试稿</text><text x="630" y="450" font-size="18">普通版 · 储罐铜框与铜盖板</text><text x="630" y="875" font-size="18">智能版 · 黄铜框与黄铜盖板</text><text x="630" y="910" font-size="14">正面 / 侧面 / 容器侧 · 离线预览不含运行时过滤图标</text></g></svg>');
 await sharp({create:{width:1260,height:930,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(out,'preview.png'));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
