// Render current assets only; do not run the resource generator.
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const faces=await mesh('ccd:block/fluid_transmitter');
 const turn=p=>[p[0],p[2],16-p[1]];
 const vertical=faces.map(f=>({...f,vertices:f.vertices.map(turn),n:[f.n[0],f.n[2],-f.n[1]]}));
 const out=path.resolve(__dirname,'../outputs/current-transmitter');fs.mkdirSync(out,{recursive:true});
 const panels=[];
 for(const [i,parts,view,name]of [[0,faces,[26,20,-34],'input'],[1,faces,[-26,20,34],'output'],[2,vertical,[26,20,-34],'vertical']]){
  const image=await render(parts,500,460,view,{center:[8,8,8],scale:23});
  await sharp(image).png().toFile(path.join(out,name+'.png'));panels.push({input:image,left:i*500,top:65});
 }
 const text=Buffer.from(`<svg width="1500" height="590" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" fill="#303535" text-anchor="middle"><text x="750" y="35" font-size="26">流体传输器 · 当前资源</text><text x="250" y="552" font-size="18">输入侧</text><text x="750" y="552" font-size="18">输出侧</text><text x="1250" y="552" font-size="18">竖直摆放</text><text x="750" y="580" font-size="14">实际模型离线预览 · 未覆盖贴图 · 不含运行时过滤图标</text></g></svg>`);
 await sharp({create:{width:1500,height:590,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:text,left:0,top:0}]).png().toFile(path.join(out,'preview.png'));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
