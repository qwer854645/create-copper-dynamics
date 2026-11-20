// Read-only dial state comparison.
const {mesh,render}=require('./preview-fluid-packager.cjs');
const sharp=require('sharp'),fs=require('node:fs');
async function main(){
 fs.mkdirSync('outputs/three-way-dial',{recursive:true});const panels=[];
 for(const [i,state]of ['unpowered','powered'].entries()){
  const faces=[...await mesh('ccd:block/three_way_valve/body'),...await mesh('ccd:block/three_way_valve/dial_'+state)];
  const image=await render(faces,500,440,[24,32,34],{center:[8,8,8],scale:23});
  panels.push({input:image,left:i*500,top:35});
 }
 const labels=Buffer.from('<svg width="1000" height="530" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="500" y="28" font-size="23">三通阀 · 红石状态仪表盘</text><text x="250" y="505" font-size="18">无信号 · 绿色</text><text x="750" y="505" font-size="18">有信号 · 红色</text></g></svg>');
 await sharp({create:{width:1000,height:530,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile('outputs/three-way-dial/preview.png');
}main().catch(e=>{console.error(e);process.exitCode=1});
