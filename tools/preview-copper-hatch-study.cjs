// Render actual current hatch models without generating resources.
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const out=path.resolve(__dirname,'../outputs/copper-hatch-study');fs.mkdirSync(out,{recursive:true});
 const panels=[];
 for(const [row,id]of ['create:block/item_hatch/block_closed','ccd:block/fluid_hatch_closed'].entries()){
  const faces=await mesh(id);
  for(const [col,view]of [[0,[0,8,40]],[1,[26,18,34]],[2,[26,18,-34]]])
   panels.push({input:await render(faces,420,380,view,{center:[8,8,4],scale:17}),left:col*420,top:55+row*420});
 }
 const labels=Buffer.from('<svg width="1260" height="930" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" fill="#303535" text-anchor="middle"><text x="630" y="32" font-size="24">铜舱口试稿 · 连续铜框与深色内侧</text><text x="630" y="450" font-size="18">Create 原版物品舱口</text><text x="630" y="875" font-size="18">铜流体舱口 · 正面 / 侧面 / 容器侧</text><text x="630" y="910" font-size="14">参考经验舱口的材质处理 · 未导入其贴图 · 智能版保持原样</text></g></svg>');
 await sharp({create:{width:1260,height:930,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(out,'preview.png'));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
