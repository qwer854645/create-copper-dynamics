const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const faces=await mesh('ccd:item/fluid_package');
 const panels=[];
 for(const [i,view]of [[0,[24,16,-34]],[1,[0,8,-40]],[2,[-24,20,34]]]){
  panels.push({input:await render(faces,450,430,view,{center:[8,6,8],scale:24}),left:i*450,top:55});
 }
 const labels=Buffer.from(`<svg width="1350" height="550" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" fill="#303535" text-anchor="middle"><text x="675" y="32" font-size="26">铜包裹 · 方盒、折合接缝与铜封边</text><text x="225" y="512" font-size="18">正面斜视</text><text x="675" y="512" font-size="18">正面观察窗</text><text x="1125" y="512" font-size="18">背面与顶部折缝</text><text x="675" y="540" font-size="14">离线实际模型预览 · 窗内为未装液暗壁，游戏中动态显示实际流体</text></g></svg>`);
 const out=path.resolve(__dirname,'../outputs/copper-parcel');fs.mkdirSync(out,{recursive:true});
 await sharp({create:{width:1350,height:550,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(out,'preview.png'));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
