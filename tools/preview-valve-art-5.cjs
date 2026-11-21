// Read-only preview of the supplied signed geometry.
const fs=require('fs'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const out='outputs/valve-art-5-reapply';fs.mkdirSync(out,{recursive:true});
 const panels=[];
 for(const[i,id]of ['three_way_valve','redstone_fluid_pump'].entries())
  panels.push({input:await render(await mesh('ccd:item/'+id),600,550,[26,28,-34],{center:[8,8,8],scale:25}),left:i*600,top:0});
 await sharp({create:{width:1200,height:550,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile(out+'/preview.png');
}main().catch(e=>{console.error(e);process.exitCode=1;});
