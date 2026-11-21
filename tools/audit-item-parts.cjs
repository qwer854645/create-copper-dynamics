// Read-only comparison, resolving texture variables instead of comparing their names.
const {model}=require('./preview-fluid-packager.cjs');
function canonical(e,m){
 const c=structuredClone(e);delete c.name;
 if(!c.rotation?.angle)delete c.rotation;
 for(const f of Object.values(c.faces)){
  for(let i=0;f.texture.startsWith('#')&&i<30;i++)f.texture=m.textures[f.texture.slice(1)];
  delete f.cullface;if(!f.rotation)delete f.rotation;
 }
 function sort(v){return Array.isArray(v)?v.map(sort):v&&typeof v==='object'?Object.fromEntries(Object.keys(v).sort().map(k=>[k,sort(v[k])])):v;}
 return JSON.stringify(sort(c));
}
for(const [item,parts]of Object.entries({
 buffer_pump:['buffer_pump/tank','buffer_pump/pump_head','buffer_pump/mouth','buffer_pump/cog'],
 reciprocating_fluid_pump:['frequency_pump/body','frequency_pump/cog'],
 vertical_turning_booster_pump:['vertical_turning_booster_pump'],
 flow_meter:['flow_meter/body','flow_meter/head'],
 creative_delivery_fluid_tank:['creative_delivery_fluid_tank'],
 omni_pump:['omni_pump/body'],
 fluid_access_port:['fluid_access_port_casing'],
 fluid_bridge:['fluid_bridge_casing']
})){
 const inventory=model('ccd:item/'+item),available=new Map();
 for(const e of inventory.elements||[]){const k=canonical(e,inventory);available.set(k,(available.get(k)||0)+1);}
 let missing=0,total=0;
 for(const part of parts){const m=model('ccd:block/'+part);for(const e of m.elements||[]){total++;const k=canonical(e,m),count=available.get(k)||0;if(count)available.set(k,count-1);else missing++;}}
 console.log(`${item}: ${total-missing}/${total} expected static/partial elements match; ${[...available.values()].reduce((a,b)=>a+b,0)} extra item elements`);
 if(missing)throw Error('Item parts out of sync: '+item);
}
for(const suffix of ['','_reversed']){
 const source=model('ccd:block/turning_booster_pump'+suffix),vertical=model('ccd:block/vertical_turning_booster_pump'+suffix);
 const mouths=m=>m.elements.filter(e=>e.name?.startsWith('native_rim')).map(e=>canonical(e,m)).sort();
 if(JSON.stringify(mouths(source))!==JSON.stringify(mouths(vertical)))throw Error('Vertical turning pump changed fluid mouths '+suffix);
 const plates=vertical.elements.filter(e=>e.name==='copper_housing');
 if(plates.length!==3)throw Error('Expected housing and two blank-face plates '+suffix);
 console.log('vertical_turning_booster_pump'+suffix+': two mouths preserved; two blank-face plates present');
}
