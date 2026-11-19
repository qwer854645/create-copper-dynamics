const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const out=path.resolve(__dirname,'../outputs/portable-transmitter');fs.mkdirSync(out,{recursive:true});
 const faces=await mesh('ccd:block/fluid_transmitter'),panels=[];
 for(const [i,view]of [[0,[26,20,-34]],[1,[-26,20,34]],[2,[32,28,8]]])
  panels.push({input:await render(faces,440,420,view,{center:[8,8,8],scale:20}),left:i*440,top:45});
 const label=Buffer.from('<svg width="1320" height="510" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="660" y="30" font-size="24">流体传输器 · 移动流体接口造型试稿</text><text x="660" y="490" font-size="16">复用原生接口与贴图 · 固定双端 · 不含运行时过滤图标</text></g></svg>');
 await sharp({create:{width:1320,height:510,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:label,left:0,top:0}]).png().toFile(path.join(out,'preview.png'));
} main().catch(e=>{console.error(e);process.exitCode=1;});
