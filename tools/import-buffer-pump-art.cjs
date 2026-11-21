// Import user parts byte-for-byte, then resolve inherited textures when assembling inventory.
const fs=require('fs'),path=require('path');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd');
const downloads=process.argv[3];if(!downloads)throw Error('Pass Create reference root and artwork directory.');
for(const [source,target]of [['pump_head.json','pump_head.json'],['cog (1).json','cog.json'],['tank.json','tank.json']]){
 const destination=path.join(assets,'models/block/buffer_pump',target);
 const backup=path.join(root,'outputs/buffer-pump-user-art/backup',target);
 fs.mkdirSync(path.dirname(backup),{recursive:true});
 if(!fs.existsSync(backup))fs.copyFileSync(destination,backup);
 fs.copyFileSync(path.join(downloads,source),destination);
}
function combine(parts){
 const textures={particle:'create:block/fluid_tank'},elements=[];
 for(const [index,id]of parts.entries()){
  const m=model('ccd:block/buffer_pump/'+id),prefix='part'+index+'_';
  for(const [key,value]of Object.entries(m.textures||{}))textures[prefix+key]=value.startsWith('#')?'#'+prefix+value.slice(1):value;
  for(const source of m.elements||[]){const e=structuredClone(source);
   for(const face of Object.values(e.faces)){face.texture='#'+prefix+face.texture.slice(1);delete face.cullface;}
   elements.push(e);
  }
 }
 return {parent:'create:block/block',render_type:'minecraft:cutout_mipped',textures,elements};
}
for(const [relative,parts]of [['block/buffer_pump',['tank','pump_head','mouth']],['item/buffer_pump',['tank','pump_head','mouth','cog']]])
 fs.writeFileSync(path.join(assets,'models',relative+'.json'),JSON.stringify(combine(parts),null,2)+'\n');
mesh('ccd:item/buffer_pump').then(m=>render(m,800,720,[26,24,-34],{center:[8,8,8],scale:30}))
 .then(image=>fs.writeFileSync(path.join(root,'outputs/buffer-pump-user-art/preview.png'),image))
 .catch(error=>{console.error(error);process.exitCode=1;});
