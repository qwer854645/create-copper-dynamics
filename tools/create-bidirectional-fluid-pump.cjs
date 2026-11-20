// Reuse current booster assets, sealing one shaft pair without modifying textures.
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),resources=path.join(root,'src/main/resources');
function save(rel,data){const p=path.join(resources,rel);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(data,null,2)+'\n');}
async function main(){
 const body=structuredClone(model('ccd:block/booster_pump'));
 for(const side of ['east','west'])body.elements[0].faces[side].texture='#body';
 save('assets/ccd/models/block/bidirectional_fluid_pump.json',body);
 const item=structuredClone(model('ccd:item/booster_pump'));
 item.elements=[...body.elements,...item.elements.filter(e=>/^inventory_shaft_(0|180)$/.test(e.name))];
 save('assets/ccd/models/item/bidirectional_fluid_pump.json',item);
 const variants={};
 for(const [facing,rotation]of Object.entries({up:{},down:{x:180},north:{x:90},south:{x:270},east:{x:90,y:90},west:{x:90,y:270}}))
  variants['facing='+facing]={model:'ccd:block/bidirectional_fluid_pump',...rotation};
 save('assets/ccd/blockstates/bidirectional_fluid_pump.json',{variants});
 save('data/ccd/recipe/bidirectional_fluid_pump.json',{type:'minecraft:crafting_shapeless',category:'redstone',ingredients:[{item:'create:mechanical_pump'},{item:'create:shaft'},{item:'create:copper_casing'}],result:{id:'ccd:bidirectional_fluid_pump',count:1}});
 save('data/ccd/loot_table/blocks/bidirectional_fluid_pump.json',{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:'ccd:bidirectional_fluid_pump'}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
 for(const rel of ['data/create/tags/block/wrench_pickup.json','data/minecraft/tags/block/mineable/pickaxe.json']){
  const data=JSON.parse(fs.readFileSync(path.join(resources,rel),'utf8'));if(!data.values.includes('ccd:bidirectional_fluid_pump'))data.values.push('ccd:bidirectional_fluid_pump');save(rel,data);
 }
 for(const locale of ['zh_cn','en_us']){
  const rel=`assets/ccd/lang/${locale}.json`,data=JSON.parse(fs.readFileSync(path.join(resources,rel),'utf8'));
  data['block.ccd.bidirectional_fluid_pump']=locale==='zh_cn'?'双向流体泵':'Bidirectional Fluid Pump';
  data['block.ccd.bidirectional_fluid_pump.tooltip.summary']=locale==='zh_cn'?'使用旋转动力泵送流体，_旋转方向_决定输送方向。':'Pumps fluids using rotational power. The _Direction of Rotation_ determines the flow direction.';
  save(rel,data);
 }
 const output=path.join(root,'outputs/bidirectional-fluid-pump');fs.mkdirSync(output,{recursive:true});
 const full=await mesh('ccd:item/bidirectional_fluid_pump'),panels=[];
 for(const [i,view]of [[0,[26,23,-34]],[1,[-26,23,34]]])panels.push({input:await render(full,500,480,view,{center:[8,8,8],scale:23}),left:i*500,top:55});
 const text=Buffer.from('<svg width="1000" height="580" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="500" y="35" font-size="26">双向流体泵 · 直通管口 / 双轴接口</text><text x="500" y="560" font-size="18">旋转方向决定流向 · 去除一对传动轴及轴孔</text></g></svg>');
 await sharp({create:{width:1000,height:580,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:text,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 console.log('Generated bidirectional pump resources and preview; existing textures unchanged.');
}main().catch(e=>{console.error(e);process.exitCode=1;});
