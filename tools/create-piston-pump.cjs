// Horizontal piston prototype. Existing materials only; portable bellows remain untouched.
const fs=require('fs'),path=require('path'),sharp=require('sharp');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'../src/main/resources/assets/ccd');
const textures={copper:'create:block/copper_underside',metal:'create:block/industrial_iron_block',brass:'create:block/brass_block',shaft:'ccd:block/booster_pump/shaft_face',pipe:'create:block/pipes',particle:'create:block/copper_underside'};
function box(name,from,to,texture){const[x,y,z]=from,[X,Y,Z]=to;return{name,from,to,faces:{
 north:{texture:'#'+texture,uv:[x,16-Y,X,16-y]},south:{texture:'#'+texture,uv:[x,16-Y,X,16-y]},
 east:{texture:'#'+texture,uv:[z,16-Y,Z,16-y]},west:{texture:'#'+texture,uv:[z,16-Y,Z,16-y]},
 up:{texture:'#'+texture,uv:[x,z,X,Z]},down:{texture:'#'+texture,uv:[x,z,X,Z]}}};}
const wrap=elements=>({parent:'create:block/block',render_type:'minecraft:cutout_mipped',textures,elements});
function save(rel,data){const p=path.join(root,rel);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(data,null,2)+'\n');}
async function main(){
 const base=[box('copper_pump_body',[2,2,2.05],[14,11.99,13.95],'copper'),
  box('top_rod_guide',[5,11.99,5],[11,12.5,11],'brass')];
 for(const side of ['east','west'])base[0].faces[side]={texture:'#shaft',uv:[2,2,14,12]};
 for(const side of ['north','south'])for(const kind of ['rim_connector','rim'])for(const original of model(`create:block/fluid_pipe/${kind}/${side}`).elements){const e=structuredClone(original);for(const f of Object.values(e.faces)){f.texture='#pipe';delete f.cullface;}base.push(e);}
 const piston=[box('vertical_push_rod',[7,10,7],[9,15.75,9],'metal'),box('brass_piston_head',[5,15.75,5],[11,16,11],'brass')];
 for(const[name,elements]of Object.entries({body:base,piston}))save('models/block/piston_fluid_pump/'+name+'.json',wrap(elements));
 save('models/item/reciprocating_fluid_pump.json',wrap([...base,...piston]));
 const variants={};for(const[facing,y]of Object.entries({north:0,east:90,south:180,west:270,up:0,down:0}))variants['facing='+facing]={model:'ccd:block/piston_fluid_pump/body',...(y?{y}:{})};
 save('blockstates/reciprocating_fluid_pump.json',{variants});
 const out='outputs/piston-fluid-pump';fs.mkdirSync(out,{recursive:true});
 const full=await mesh('ccd:item/reciprocating_fluid_pump');const panels=[];
 for(const[i,view]of [[0,[30,24,-32]],[1,[30,28,32]]])panels.push({input:await render(full,600,550,view,{center:[8,8,8],scale:27}),left:i*600,top:0});
 await sharp({create:{width:1200,height:550,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile(out+'/preview.png');
}main().catch(e=>{console.error(e);process.exitCode=1;});
