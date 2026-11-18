const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const sizes=['10x8','12x10','10x12','12x12'],panels=[];
 for(const [row,size]of sizes.entries()) for(const [col,material]of ['original','copper','brass'].entries()){
  const id=material==='original'?'create:item/package/cardboard_'+size:`ccd:item/native_package_${material}_${size}_study`;
  const faces=await mesh(id),height=Number(size.split('x')[1]);
  panels.push({input:await render(faces,400,350,[24,18,-34],{center:[8,height/2,8],scale:20}),left:col*400,top:85+row*385});
 }
 const labels=Buffer.from(`<svg width="1200" height="1655" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" fill="#303535" text-anchor="middle"><text x="600" y="30" font-size="24">Create 四种普通包裹 · 铜色与黄铜包边</text>${['Create 原版','铜色包裹','黄铜包裹'].map((s,i)=>`<text x="${200+i*400}" y="70" font-size="19">${s}</text>`).join('')}${sizes.map((s,i)=>`<text x="600" y="${448+i*385}" font-size="17">尺寸 ${s}</text>`).join('')}<text x="600" y="1640" font-size="14">原版几何、UV、眼睛和封带保留 · 试稿，不替换原版包裹或 CCD 载体</text></g></svg>`);
 const out=path.resolve(__dirname,'../outputs/native-metal-packages');fs.mkdirSync(out,{recursive:true});
 await sharp({create:{width:1200,height:1655,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(out,'preview.png'));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
