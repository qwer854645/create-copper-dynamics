// Render current runtime models and textures without regenerating any game assets.
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..');
function rotate(v,angle,point){
 const a=angle*Math.PI/180,c=Math.cos(a),s=Math.sin(a),p=v.map(n=>n-(point?8:0));
 return [p[0],p[1]*c-p[2]*s,p[1]*s+p[2]*c].map(n=>n+(point?8:0));
}
const turn=(faces,angle)=>faces.map(f=>({...f,vertices:f.vertices.map(v=>rotate(v,angle,true)),n:rotate(f.n,angle,false)}));
async function main(){
 const panels=[];
 for(const [row,id,name]of [[0,'ccd:item/booster_pump','增压泵'],[1,'ccd:block/fluid_transmitter','流体传输器']]){
  const faces=await mesh(id),horizontal=row===0?turn(faces,90):faces,vertical=row===0?faces:turn(faces,-90);
  for(const [col,parts,view]of [[0,horizontal,[26,20,-34]],[1,horizontal,[-26,20,34]],[2,vertical,[26,20,-34]]]){
   const buffer=await render(parts,480,420,view,{center:[8,8,8],scale:21});
   panels.push({input:buffer,left:col*480,top:row*500+65});
  }
 }
 const labels=Buffer.from(`<svg width="1440" height="1040" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" fill="#303535" text-anchor="middle"><text x="720" y="35" font-size="25">增压泵 · 最新对称铜壳</text><text x="720" y="535" font-size="25">流体传输器 · 最新对称铜壳</text>${[0,1].map(row=>['水平正面','水平反面','竖直摆放'].map((s,i)=>`<text x="${240+i*480}" y="${480+row*500}" font-size="18">${s}</text>`).join('')).join('')}<text x="720" y="1025" font-size="15" fill="#677070">实际资源离线渲染 · 原生轴孔、过滤面板及方向箭头保留 · 非游戏截图</text></g></svg>`);
 const out=path.join(root,'outputs/symmetric-housings');fs.mkdirSync(out,{recursive:true});
 await sharp({create:{width:1440,height:1040,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(out,'preview.png'));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
