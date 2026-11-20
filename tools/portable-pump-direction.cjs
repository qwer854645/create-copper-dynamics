// Reuse the approved booster pump arrow UV and native blockstate rotation.
const fs=require('fs'),path=require('path');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const {turn}=require('./create-turning-booster-pump.cjs');
const root=path.resolve(__dirname,'../src/main/resources/assets/ccd');
const body=model('ccd:block/reciprocating_fluid_pump/body');
body.textures.native_pump='create:block/pump';
const old=JSON.parse(fs.readFileSync(path.resolve(__dirname,'../outputs/booster-pump-revision/backup/models/block/booster_pump.json'),'utf8'));
const arrows=[]; // Portable pump follows shaft direction; no fixed flow arrows.
// Fit the arrows to the sides of the low base rather than over the bellows.
for(const e of arrows)for(const p of [e.from,e.to])p[1]-=2;
body.elements=[...body.elements,...arrows];
fs.writeFileSync(path.join(root,'models/block/portable_fluid_pump.json'),JSON.stringify(body,null,2)+'\n');
const states={variants:{}};
for(const[facing,y]of Object.entries({north:0,east:90,south:180,west:270,up:0,down:0}))
 states.variants['facing='+facing]={model:'ccd:block/portable_fluid_pump',...(y?{y}:{})};
fs.writeFileSync(path.join(root,'blockstates/portable_fluid_pump.json'),JSON.stringify(states,null,2)+'\n');
const item=model('ccd:item/reciprocating_fluid_pump');
item.elements=[...item.elements,...arrows];item.textures.native_pump='create:block/pump';
fs.writeFileSync(path.join(root,'models/item/portable_fluid_pump.json'),JSON.stringify(item,null,2)+'\n');
fs.mkdirSync('outputs/portable-fluid-pump',{recursive:true});
(async()=>fs.writeFileSync('outputs/portable-fluid-pump/preview.png',await render(await mesh('ccd:item/portable_fluid_pump'),650,600,[26,26,-34],{center:[8,8,8],scale:26})))().catch(e=>{console.error(e);process.exitCode=1;});
