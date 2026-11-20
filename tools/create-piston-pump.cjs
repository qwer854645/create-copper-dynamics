// Horizontal piston prototype. Existing materials only; portable bellows remain untouched.
const fs=require('fs'),path=require('path'),sharp=require('sharp');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'../src/main/resources/assets/ccd');
const textures={copper:'create:block/copper_casing',metal:'create:block/industrial_iron_block',brass:'create:block/brass_block',shaft:'ccd:block/booster_pump/shaft_face',pipe:'create:block/pipes',particle:'create:block/copper_casing'};
function box(name,from,to,texture){return{name,from,to,faces:Object.fromEntries(['north','south','east','west','up','down'].map(s=>[s,{texture:'#'+texture,uv:[0,0,16,16]}]))};}
const wrap=elements=>({parent:'create:block/block',render_type:'minecraft:cutout_mipped',textures,elements});
function save(rel,data){const p=path.join(root,rel);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(data,null,2)+'\n');}
async function main(){
 const base=[box('base',[1,0,0],[15,2,16],'copper'),box('pump_cylinder',[3,3,2.05],[13,13,5],'copper'),box('cylinder_end',[4,4,5.01],[12,12,6],'brass'),box('crank_support',[5,2,12],[11,6,14],'copper')];
 base.push(box('left_guide',[3,3,6],[4,4,12],'metal'),box('right_guide',[12,3,6],[13,4,12],'metal'));
 for(const side of ['north','south'])for(const kind of ['rim_connector','rim'])for(const original of model(`create:block/fluid_pipe/${kind}/${side}`).elements){const e=structuredClone(original);for(const f of Object.values(e.faces)){f.texture='#pipe';delete f.cullface;}base.push(e);}
 const piston=[box('piston_rod',[7,7,3],[9,9,7],'metal'),box('crosshead',[5,6,6.5],[14.5,10,7.5],'brass')];
 // Crank rotates around local X at (8,8,13); linkage is drawn on its visible east side.
 const crank=[box('crank_disc',[13.05,5,10],[14.05,11,16],'brass'),box('offset_pin',[14.05,7.5,10.5],[14.8,8.5,11.5],'metal')];
 const link=[box('connecting_rod',[14.1,7.5,7],[14.6,8.5,11],'metal')];
 for(const[name,elements]of Object.entries({body:base,piston,crank,link}))save('models/block/piston_fluid_pump/'+name+'.json',wrap(elements));
 save('models/item/reciprocating_fluid_pump.json',wrap([...base,...piston,...crank,...link]));
 const variants={};for(const[facing,y]of Object.entries({north:0,east:90,south:180,west:270,up:0,down:0}))variants['facing='+facing]={model:'ccd:block/piston_fluid_pump/body',...(y?{y}:{})};
 save('blockstates/reciprocating_fluid_pump.json',{variants});
 const out='outputs/piston-fluid-pump';fs.mkdirSync(out,{recursive:true});
 const full=await mesh('ccd:item/reciprocating_fluid_pump');const panels=[];
 for(const[i,view]of [[0,[30,24,-32]],[1,[30,28,32]]])panels.push({input:await render(full,600,550,view,{center:[8,8,8],scale:27}),left:i*600,top:0});
 await sharp({create:{width:1200,height:550,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile(out+'/preview.png');
}main().catch(e=>{console.error(e);process.exitCode=1;});
