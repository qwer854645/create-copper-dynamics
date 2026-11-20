// Read current imported geometry and artwork only.
const fs=require('fs'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const out='outputs/art-revision-4';fs.mkdirSync(out,{recursive:true});const panels=[];
 for(const[i,id]of ['bidirectional_fluid_pump','portable_fluid_pump','redstone_fluid_pump'].entries())
  panels.push({input:await render(await mesh('ccd:item/'+id),500,480,[26,27,-34],{center:[8,8,8],scale:22}),left:i*500,top:0});
 await sharp({create:{width:1500,height:480,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile(out+'/preview.png');
}main().catch(e=>{console.error(e);process.exitCode=1;});
