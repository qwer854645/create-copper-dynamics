const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const panels=[];
 for(const [col,id]of ['create:item/package/cardboard_12x12','ccd:item/native_package_copper_study','ccd:item/native_package_brass_study'].entries()){
  const faces=await mesh(id);
  for(const [row,view]of [[0,[24,18,-34]],[1,[-24,25,34]]]) panels.push({input:await render(faces,420,360,view,{center:[8,6,8],scale:20}),left:col*420,top:55+row*395});
 }
 const labels=Buffer.from(`<svg width="1260" height="870" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" fill="#303535" text-anchor="middle"><text x="630" y="30" font-size="24">原版包裹造型 · CCD 金属材质重绘</text>${['Create 原版纸包裹','铜款 · 流体元件铜面','黄铜款 · 动力臂黄铜'].map((s,i)=>`<text x="${210+i*420}" y="425" font-size="19">${s}</text>`).join('')}<text x="630" y="852" font-size="15">原模型与 UV 不变 · 金属像素簇、折边与封带 · 仅预览，未替换游戏载体</text></g></svg>`);
 const out=path.resolve(__dirname,'../outputs/native-metal-packages');fs.mkdirSync(out,{recursive:true});
 await sharp({create:{width:1260,height:870,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(out,'preview.png'));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
