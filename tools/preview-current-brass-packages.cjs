// Read current package resources only; never regenerate user artwork.
const fs=require('node:fs'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const output='outputs/current-brass-packages';fs.mkdirSync(output,{recursive:true});
 const panels=[];
 for(const [i,id]of ['composite_package','brass_package_10x8','brass_package_12x10','brass_package_10x12'].entries())
  panels.push({input:await render(await mesh('ccd:item/'+id),400,400,[26,22,-34],{center:[8,8,8],scale:24}),left:i*400,top:0});
 await sharp({create:{width:1600,height:400,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile(output+'/preview.png');
}main().catch(e=>{console.error(e);process.exitCode=1;});
