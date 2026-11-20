// Shared bellows geometry, native blockstate rotation; no copied body or fixed arrows.
const fs=require('fs'),path=require('path');
const {mesh,render}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'../src/main/resources/assets/ccd');
fs.writeFileSync(path.join(root,'models/block/portable_fluid_pump.json'),JSON.stringify({parent:'ccd:block/reciprocating_fluid_pump/body'},null,2)+'\n');
const states={variants:{}};
for(const[facing,y]of Object.entries({north:0,east:90,south:180,west:270,up:0,down:0}))
 states.variants['facing='+facing]={model:'ccd:block/portable_fluid_pump',...(y?{y}:{})};
fs.writeFileSync(path.join(root,'blockstates/portable_fluid_pump.json'),JSON.stringify(states,null,2)+'\n');
fs.writeFileSync(path.join(root,'models/item/portable_fluid_pump.json'),JSON.stringify({parent:'ccd:item/reciprocating_fluid_pump'},null,2)+'\n');
fs.mkdirSync('outputs/portable-fluid-pump',{recursive:true});
(async()=>fs.writeFileSync('outputs/portable-fluid-pump/preview.png',await render(await mesh('ccd:item/portable_fluid_pump'),650,600,[26,26,-34],{center:[8,8,8],scale:26})))().catch(e=>{console.error(e);process.exitCode=1;});
