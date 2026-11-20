const {mesh,render}=require('./preview-fluid-packager.cjs');
const fs=require('node:fs'),sharp=require('sharp');
async function main(){
 fs.mkdirSync('outputs/redstone-fluid-pump',{recursive:true});
 const item=await mesh('ccd:item/redstone_fluid_pump'),pointer=await mesh('ccd:block/redstone_fluid_pump/pointer');
 const body=item.slice(0,item.length-pointer.length-12);
 const flip=pointer.map(f=>({...f,vertices:f.vertices.map(p=>[16-p[0],p[1],16-p[2]]),n:[-f.n[0],f.n[1],-f.n[2]]}));
 const panels=[];
 const shafts=item.slice(-12);
 for(const [i,needle]of [pointer,flip].entries())panels.push({input:await render([...body,...needle,...shafts],500,450,[26,28,-34],{center:[8,8,8],scale:21}),left:i*500,top:45});
 panels.push({input:await render([...body,...shafts],500,450,[-26,-28,34],{center:[8,8,8],scale:21}),left:1000,top:45});
 const text=Buffer.from('<svg width="1500" height="550" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="750" y="30" font-size="24">红石双向动力泵 · 单面半长指针 / 物品双轴</text><text x="250" y="525" font-size="18">无信号</text><text x="750" y="525" font-size="18">有信号 · 反向泵送</text><text x="1250" y="525" font-size="18">背面封闭</text></g></svg>');
 await sharp({create:{width:1500,height:550,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:text,left:0,top:0}]).png().toFile('outputs/redstone-fluid-pump/preview.png');
}main().catch(e=>{console.error(e);process.exitCode=1});
