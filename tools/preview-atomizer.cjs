// Offline actual-model preview; blue liquid is illustrative only.
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..');
async function main(){
 const shell=await mesh('ccd:block/fluid_atomizer'),rotor=await mesh('ccd:block/fluid_atomizer/propeller');
 // Native rotor default -Z is oriented to the block model's +Y outlet.
 const turn=p=>[p[0],16-p[2],p[1]];
 const faces=[...shell,...rotor.map(f=>({...f,vertices:f.vertices.map(turn),n:[f.n[0],-f.n[2],f.n[1]]}))];
 const liquid={data:Buffer.alloc(1024),info:{width:16,height:16,channels:4}};
 for(let i=0;i<256;i++)liquid.data.set([48,108,176,255],i*4);
 const l=2.24,h=13.76,y=2.24,Y=9.5,z=2.24,Z=8;
 const fluid=[{vertices:[[l,Y,z],[l,Y,Z],[h,Y,Z],[h,Y,z]],n:[0,1,0]},
 {vertices:[[h,Y,z],[h,y,z],[l,y,z],[l,Y,z]],n:[0,0,-1]},
 {vertices:[[h,Y,Z],[h,y,Z],[h,y,z],[h,Y,z]],n:[1,0,0]}].map(f=>({...f,uv:[[0,0],[0,16],[16,16],[16,0]],pixels:liquid}));
 const orient=p=>[p[0],p[2],16-p[1]];
 const horizontal=parts=>parts.map(f=>({...f,vertices:f.vertices.map(orient),n:[f.n[0],f.n[2],-f.n[1]]}));
 const panels=[];
 for(const [i,parts,view]of [[0,horizontal(faces),[25,20,-34]],[1,[...horizontal(faces),...fluid],[25,20,-34]],[2,horizontal(faces),[-25,20,34]]]){
  panels.push({input:await render(parts,460,460,view,{center:[8,8,8],scale:20}),left:i*460,top:60});
 }
 const labels=Buffer.from(`<svg width="1380" height="580" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" fill="#303535" text-anchor="middle"><text x="690" y="32" font-size="25">雾化器 · 转子后移与前部储液展示</text><text x="230" y="550" font-size="18">空罐 · 正面</text><text x="690" y="550" font-size="18">前部储液 · 蓝色示意</text><text x="1150" y="550" font-size="18">铜色背板与原生轴孔</text></g></svg>`);
 const out=path.join(root,'outputs/fluid-atomizer');fs.mkdirSync(out,{recursive:true});
 await sharp({create:{width:1380,height:580,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(out,'preview.png'));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
