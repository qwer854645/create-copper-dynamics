// Read-only preview of current models; no artwork regeneration.
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const out=path.resolve(__dirname,'../outputs/brass-edge-tanks');fs.mkdirSync(out,{recursive:true});
 const single=await mesh('ccd:block/smart_fluid_tank/block_single_window');
 const tower=[];
 for(const [i,name]of ['block_bottom_window','block_middle_window','block_top_window'].entries()){
  const faces=await mesh('ccd:block/smart_fluid_tank/'+name);
  tower.push(...faces.map(f=>({...f,vertices:f.vertices.map(p=>[p[0],p[1]+i*16,p[2]])})));
 }
 const panels=[];
 for(const [i,faces,center,scale]of [[0,await mesh('create:block/fluid_tank/block_single_window'),[8,8,8],19],[1,single,[8,8,8],19],[2,tower,[8,24,8],10]])
  panels.push({input:await render(faces,440,550,[26,22,34],{center,scale}),left:i*440,top:50});
 const labels=Buffer.from('<svg width="1320" height="650" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="660" y="32" font-size="24">智能储罐 · 原版储罐与黄铜机壳包边</text><text x="220" y="615" font-size="18">Create 原版</text><text x="660" y="615" font-size="18">新版单格</text><text x="1100" y="615" font-size="18">三格模型组合</text><text x="660" y="642" font-size="13">离线模型预览 · 不模拟游戏运行时连接纹理；完整连接图集另附</text></g></svg>');
 await sharp({create:{width:1320,height:650,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(out,'preview.png'));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
