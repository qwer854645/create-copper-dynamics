// Inventory-only configured faces: upper shaft, north inlet, east outlet.
const fs=require('fs'),path=require('path');
const {model}=require('./preview-fluid-packager.cjs');
const {turn}=require('./create-turning-booster-pump.cjs');
const elements=[],textures={particle:'create:block/copper_underside'};
const identity=[[1,0,0],[0,1,0],[0,0,1]];
function append(id,basis=identity){
 const m=model(id),prefix='part'+elements.length+'_';
 for(const [key,value]of Object.entries(m.textures||{}))textures[prefix+key]=value.startsWith('#')?'#'+prefix+value.slice(1):value;
 for(const original of m.elements){
  const e=turn(original,basis);
  for(const f of Object.values(e.faces)){f.texture='#'+prefix+f.texture.slice(1);delete f.cullface;}
  elements.push(e);
 }
}
append('ccd:block/omni_pump/body');
append('ccd:block/omni_pump/input');
append('ccd:block/omni_pump/output',[[0,0,1],[0,1,0],[-1,0,0]]);
append('ccd:block/omni_pump/shaft',[[1,0,0],[0,0,1],[0,-1,0]]);
append('create:block/shaft_half',[[1,0,0],[0,0,-1],[0,1,0]]);
const result={parent:'create:block/block',render_type:'minecraft:cutout_mipped',textures,elements};
fs.writeFileSync(path.resolve(__dirname,'../src/main/resources/assets/ccd/models/item/omni_pump.json'),JSON.stringify(result,null,2)+'\n');
