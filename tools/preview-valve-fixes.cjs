// Read-only preview of the current generated inventory models.
const fs=require('fs'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const out='outputs/valve-fixes';fs.mkdirSync(out,{recursive:true});
 const panels=[];
 for(const [i,id]of ['redstone_fluid_pump','three_way_valve'].entries())
  panels.push({input:await render(await mesh('ccd:item/'+id),480,480,[26,27,-34],{center:[8,8,8],scale:21}),left:i*480,top:0});
 await sharp({create:{width:960,height:480,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile(out+'/preview.png');
}
main().catch(e=>{console.error(e);process.exitCode=1;});
