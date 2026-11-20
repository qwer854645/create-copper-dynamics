// Read-only preview: current models and textures, no resource generation.
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const out=path.resolve(__dirname,'../outputs/current-grates');fs.mkdirSync(out,{recursive:true});
 const copper=await mesh('ccd:block/copper_collection_grate');
 const smart=await mesh('ccd:block/smart_collection_grate');
 const panels=[];
 for(const [i,faces]of [copper,smart].entries()){
  const image=await render(faces,500,500,[26,24,38],{center:[8,8,8],scale:20});
  panels.push({input:image,left:i*500,top:50});
 }
 const labels=Buffer.from('<svg width="1000" height="600" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" fill="#303535" text-anchor="middle"><text x="500" y="35" font-size="24">集液栅 · 一格高</text><text x="250" y="575" font-size="20">铜集液栅</text><text x="750" y="575" font-size="20">智能集液栅</text></g></svg>');
 await sharp({create:{width:1000,height:600,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(out,'preview.png'));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
