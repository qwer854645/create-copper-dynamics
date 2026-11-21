const fs=require('fs'),path=require('path');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'../src/main/resources');
function save(rel,data){const p=path.join(root,rel);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(data,null,2)+'\n');}
async function main(){
 const body=model('create:block/portable_fluid_interface/block');
 body.parent='create:block/block';body.textures['0']='create:block/creative_fluid_tank';body.textures['2']='create:block/creative_fluid_tank';body.textures.particle='create:block/creative_fluid_tank';
 for(const e of body.elements)for(const f of Object.values(e.faces))f.uv=[0,0,16,16];
 const top=model('create:block/portable_fluid_interface/block_top');
 for(const[k,v]of Object.entries(top.textures))body.textures['mouth_'+k]=v;
 for(const original of top.elements){const e=structuredClone(original);for(const f of Object.values(e.faces)){if(f.texture.startsWith('#'))f.texture='#mouth_'+f.texture.slice(1);delete f.cullface;}body.elements.push(e);}
 save('assets/ccd/models/block/creative_delivery_fluid_tank.json',body);
 save('assets/ccd/models/item/creative_delivery_fluid_tank.json',{parent:'ccd:block/creative_delivery_fluid_tank'});
 const variants={};for(const[facing,rotation]of Object.entries({up:{},down:{x:180},north:{x:90},south:{x:270},east:{x:90,y:90},west:{x:90,y:270}}))variants['facing='+facing]={model:'ccd:block/creative_delivery_fluid_tank',...rotation};
 save('assets/ccd/blockstates/creative_delivery_fluid_tank.json',{variants});
 for(const rel of ['data/create/tags/block/wrench_pickup.json','data/minecraft/tags/block/mineable/pickaxe.json']){const d=JSON.parse(fs.readFileSync(path.join(root,rel)));if(!d.values.includes('ccd:creative_delivery_fluid_tank'))d.values.push('ccd:creative_delivery_fluid_tank');save(rel,d);}
 save('data/ccd/loot_table/blocks/creative_delivery_fluid_tank.json',{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:'ccd:creative_delivery_fluid_tank'}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
 for(const locale of ['zh_cn','en_us']){const rel=`assets/ccd/lang/${locale}.json`,d=JSON.parse(fs.readFileSync(path.join(root,rel),'utf8'));
 d['block.ccd.creative_delivery_fluid_tank']=locale==='zh_cn'?'输送式创造流体储罐':'Creative Delivery Fluid Tank';
 d['ccd.creative_delivery.empty']=locale==='zh_cn'?'未选择流体':'No fluid selected';
 d['ccd.creative_delivery.active']=locale==='zh_cn'?'无限供应 · 自动向开口输送':'Infinite supply · automatic outward delivery';save(rel,d);}
 fs.mkdirSync('outputs/creative-delivery-tank',{recursive:true});fs.writeFileSync('outputs/creative-delivery-tank/preview.png',await render(await mesh('ccd:item/creative_delivery_fluid_tank'),650,650,[26,28,-34],{center:[8,8,8],scale:24}));
}main().catch(e=>{console.error(e);process.exitCode=1;});
