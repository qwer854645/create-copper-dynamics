// Synchronize inventory dials from their actual runtime partials; preview only current art.
const fs=require('fs'),path=require('path'),sharp=require('sharp');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const {turn}=require('./create-turning-booster-pump.cjs');
const root=path.resolve(__dirname,'../src/main/resources/assets/ccd');
const p=path.join(root,'models/item/three_way_valve.json'),item=JSON.parse(fs.readFileSync(p,'utf8'));
item.elements=item.elements.filter(e=>!e.name?.startsWith('dial_item_'));
for(const [i,basis]of [[[0,1,0],[1,0,0],[0,0,-1]],[[0,-1,0],[-1,0,0],[0,0,-1]]].entries())
 for(const id of ['dial_unpowered','pointer']){
  const source=model('ccd:block/three_way_valve/'+id);Object.assign(item.textures,source.textures);
  for(const original of source.elements){const e=turn({...original,rotation:undefined},basis);e.name='dial_item_'+i+'_'+(e.name||id);item.elements.push(e);}
 }
fs.writeFileSync(p,JSON.stringify(item,null,2)+'\n');
async function main(){
 const out='outputs/art-revision-5';fs.mkdirSync(out,{recursive:true});const panels=[];
 for(const[i,id]of ['redstone_fluid_pump','redstone_fluid_valve','three_way_valve','turning_booster_pump'].entries())
  panels.push({input:await render(await mesh('ccd:item/'+id),450,460,[26,27,-34],{center:[8,8,8],scale:20}),left:i*450,top:0});
 await sharp({create:{width:1800,height:460,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile(out+'/preview.png');
}main().catch(e=>{console.error(e);process.exitCode=1;});
