// Reuse the approved turning-pump geometry, changing only shaft faces and shaft direction.
const fs=require('fs'),path=require('path'),sharp=require('sharp');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const {turn}=require('./create-turning-booster-pump.cjs');
const root=path.resolve(__dirname,'../src/main/resources');
const read=p=>JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));
const save=(p,d)=>{p=path.join(root,p);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(d,null,2)+'\n');};
const id='vertical_turning_booster_pump';
for(const suffix of ['','_reversed','_cross','_reversed_cross']){
 const sourceId='ccd:block/turning_booster_pump'+suffix;
 // The cross variant is generated from the same edited base below.
 if(suffix.includes('cross'))continue;
 const source=structuredClone(model(sourceId));
 for(const e of source.elements)for(const f of Object.values(e.faces))if(f.texture==='#shaft_face')f.texture='#body';
 for(const e of source.elements.slice(1,3))for(const f of Object.values(e.faces))f.texture='#shaft_face';
 save(`assets/ccd/models/block/${id}${suffix}.json`,source);
 const basis=[[0,1,0],[-1,0,0],[0,0,1]];
 save(`assets/ccd/models/block/${id}${suffix}_cross.json`,{...source,elements:source.elements.map(e=>turn(e,basis))});
}
const states=read('assets/ccd/blockstates/turning_booster_pump.json');
for(const spec of Object.values(states.variants))spec.model=spec.model.replace('turning_booster_pump',id);
save(`assets/ccd/blockstates/${id}.json`,states);
const body=read(`assets/ccd/models/block/${id}.json`),shaft=model('create:block/shaft_half');
const shafts=[];
for(const basis of [[[0,0,-1],[0,1,0],[1,0,0]],[[0,0,1],[0,1,0],[-1,0,0]]])for(const original of shaft.elements){
 const e=turn(original,basis);for(const f of Object.values(e.faces))f.texture=f.texture==='#0'?'#shaft_side':'#shaft_end';shafts.push(e);
}
save(`assets/ccd/models/item/${id}.json`,{...body,textures:{...body.textures,shaft_side:shaft.textures['0'],shaft_end:shaft.textures['1']},elements:[...body.elements,...shafts]});
for(const [result,ingredient]of [[id,'turning_booster_pump'],['turning_booster_pump',id]])
 save(`data/ccd/recipe/${result}_from_${ingredient}.json`,{type:'minecraft:crafting_shapeless',category:'redstone',ingredients:[{item:'ccd:'+ingredient}],result:{id:'ccd:'+result,count:1}});
save(`data/ccd/loot_table/blocks/${id}.json`,{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:'ccd:'+id}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
for(const p of ['data/create/tags/block/wrench_pickup.json','data/minecraft/tags/block/mineable/pickaxe.json']){const d=read(p);if(!d.values.includes('ccd:'+id))d.values.push('ccd:'+id);save(p,d);}
for(const locale of ['zh_cn','en_us']){const p=`assets/ccd/lang/${locale}.json`,d=read(p);d['block.ccd.'+id]=locale==='zh_cn'?'垂直转向增压泵':'Perpendicular Turning Booster Pump';d['block.ccd.'+id+'.tooltip.summary']=locale==='zh_cn'?'传动轴_垂直于双管口平面_，输送能力与转向增压泵相同。':'The shaft is _perpendicular to the fluid port plane_, retaining Turning Booster Pump performance.';save(p,d);}
async function main(){
 const out='outputs/vertical-turning-pump';fs.mkdirSync(out,{recursive:true});const m=await mesh('ccd:item/'+id),panels=[];
 for(const [i,view]of [[0,[26,23,-34]],[1,[-26,23,34]]])panels.push({input:await render(m,500,480,view,{center:[8,8,8],scale:23}),left:i*500,top:0});
 await sharp({create:{width:1000,height:480,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile(out+'/preview.png');
}
main().catch(e=>{console.error(e);process.exitCode=1;});
