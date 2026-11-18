const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const panels=[];
 for(const [col,id]of ['create:item/package/cardboard_12x12','ccd:item/native_package_copper_study','ccd:item/native_package_brass_study'].entries()){
  const faces=await mesh(id);
  for(const [row,view]of [[0,[24,18,-34]],[1,[-24,25,34]]]) panels.push({input:await render(faces,420,360,view,{center:[8,6,8],scale:20}),left:col*420,top:55+row*395});
 }
 const labels=Buffer.from(`<svg width="1260" height="870" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" fill="#303535" text-anchor="middle"><text x="630" y="30" font-size="24">Create 原版包裹 · 仅换色试稿</text>${['原版纸包裹','铜色包裹','黄铜色包裹'].map((s,i)=>`<text x="${210+i*420}" y="425" font-size="19">${s}</text>`).join('')}<text x="630" y="852" font-size="15">原模型与 UV 不变 · 上排正面，下排背面 · 无观察窗 · 未替换游戏中的载体</text></g></svg>`);
 const out=path.resolve(__dirname,'../outputs/native-metal-packages');fs.mkdirSync(out,{recursive:true});
 await sharp({create:{width:1260,height:870,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(out,'preview.png'));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
