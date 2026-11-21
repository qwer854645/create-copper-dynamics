const fs=require('fs'),path=require('path'),sharp=require('sharp');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const {turn}=require('./create-turning-booster-pump.cjs');
const root=path.resolve(__dirname,'../src/main/resources');
const save=(p,d)=>{p=path.join(root,p);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(d,null,2)+'\n');};
const base=structuredClone(model('create:block/gauge/base'));
base.parent='create:block/block';base.textures={'2':'ccd:block/flow_meter',copper:'create:block/copper_underside',pipe:'create:block/pipes',particle:'create:block/copper_underside'};
for(const side of ['north','south'])base.elements[0].faces[side]={texture:'#copper',uv:[2,2,14,14]};
for(const side of ['north','south'])for(const part of ['rim_connector','rim'])for(const original of model(`create:block/fluid_pipe/${part}/${side}`).elements){const e=structuredClone(original);for(const f of Object.values(e.faces)){f.texture='#pipe';delete f.cullface;}base.elements.push(e);}
save('assets/ccd/models/block/flow_meter/body.json',base);
const head=structuredClone(model('create:block/gauge/stressometer/head'));head.parent='create:block/block';head.textures={'2':'ccd:block/flow_meter'};
save('assets/ccd/models/block/flow_meter/head.json',head);
const multipart=[];
for(const [property,rotation]of [['north',{}],['east',{y:90}],['up',{x:90}]])multipart.push({when:{[property]:'true'},apply:{model:'ccd:block/flow_meter/body',...rotation}});
for(const [facing,y]of Object.entries({east:0,south:90,west:180,north:270}))multipart.push({when:{facing},apply:{model:'ccd:block/flow_meter/head',...(y?{y}:{})}});
save('assets/ccd/blockstates/flow_meter.json',{multipart});
const dial=model('create:block/gauge/dial');
const elements=structuredClone(base.elements);
for(const [m,prefix]of [[head,'head'],[dial,'dial']])for(const original of m.elements){const e=structuredClone(original);for(const f of Object.values(e.faces))f.texture='#'+prefix;elements.push(e);}
save('assets/ccd/models/item/flow_meter.json',{...base,display:{gui:{rotation:[30,225,0],translation:[0,0,0],scale:[.625,.625,.625]}},textures:{...base.textures,head:'ccd:block/flow_meter',dial:'create:block/gauge'},elements});
save('data/ccd/recipe/flow_meter.json',{type:'minecraft:crafting_shapeless',ingredients:[{item:'create:stressometer'},{item:'create:fluid_pipe'},{item:'minecraft:copper_ingot'}],result:{id:'ccd:flow_meter',count:1}});
save('data/ccd/loot_table/blocks/flow_meter.json',{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:'ccd:flow_meter'}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
for(const p of ['data/create/tags/block/wrench_pickup.json','data/minecraft/tags/block/mineable/pickaxe.json']){const d=JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));if(!d.values.includes('ccd:flow_meter'))d.values.push('ccd:flow_meter');save(p,d);}
for(const locale of ['zh_cn','en_us']){const p=`assets/ccd/lang/${locale}.json`,d=JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));d['block.ccd.flow_meter']=locale==='zh_cn'?'流量计':'Flow Meter';d['ccd.flow_meter.rate']=locale==='zh_cn'?'当前流速：%s mB/t':'Current flow: %s mB/t';d['block.ccd.flow_meter.tooltip.summary']=locale==='zh_cn'?'串接管道，使用_工程师护目镜_读取当前流速。':'Install in a pipe and read current flow with _Engineer’s Goggles_.';save(p,d);}
async function main(){const out='outputs/flow-meter';fs.mkdirSync(out,{recursive:true});const m=await mesh('ccd:item/flow_meter');const panels=[];for(const[i,view]of [[0,[30,24,-32]],[1,[-30,24,32]]])panels.push({input:await render(m,500,480,view,{center:[8,8,8],scale:22}),left:i*500,top:0});await sharp({create:{width:1000,height:480,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile(out+'/preview.png');}
main().catch(e=>{console.error(e);process.exitCode=1;});
