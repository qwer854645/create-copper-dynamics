// Preview current imported resources only; no texture/model generation.
const fs=require('fs'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const out='outputs/art-revision-3';fs.mkdirSync(out,{recursive:true});
 const panels=[];
 const pump=[...await mesh('ccd:block/redstone_fluid_pump/block'),...await mesh('ccd:block/redstone_fluid_pump/pointer')];
 const valve=[...await mesh('ccd:block/redstone_fluid_valve/block_horizontal_closed'),...await mesh('ccd:block/redstone_fluid_valve/dial_closed')];
 const bidirectional=await mesh('ccd:item/bidirectional_fluid_pump');
 for(const[i,faces]of [pump,valve,bidirectional].entries())panels.push({input:await render(faces,500,480,[26,28,-34],{center:[8,8,8],scale:22}),left:i*500,top:0});
 await sharp({create:{width:1500,height:480,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile(out+'/devices.png');
 const names=['fluid_tank','fluid_tank_inner','fluid_tank_top','fluid_tank_connected','fluid_tank_inner_connected','fluid_tank_top_connected','fluid_tank_window','fluid_tank_window_single'];
 const tiles=[];
 for(const[i,name]of names.entries())tiles.push({input:await sharp('src/main/resources/assets/ccd/textures/block/smart_fluid_tank/'+name+'.png').resize(256,256,{kernel:'nearest'}).png().toBuffer(),left:(i%4)*272,top:Math.floor(i/4)*272});
 await sharp({create:{width:1088,height:544,channels:4,background:'#e5e8e2'}}).composite(tiles).png().toFile(out+'/tank-textures.png');
}main().catch(e=>{console.error(e);process.exitCode=1;});
