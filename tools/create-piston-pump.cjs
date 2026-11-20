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
 for(const side of ['north','south'])for(const kind of ['rim_connector','rim'])for(const original of model(`create:block/fluid_pipe/${kind}/${side}`).elements){const e=structuredClone(original);for(const f of Object.values(e.faces)){f.texture='#pipe';delete f.cullface;}base.push(e);}
 const piston=[box('vertical_push_rod',[7,10,7],[9,15.75,9],'metal'),box('brass_piston_head',[5,15.75,5],[11,16,11],'brass')];
 const tallPiston=[box('vertical_piston',[7,9,7],[9,18,9],'metal'),box('crosshead',[6,17,6],[10,19,10],'brass')];
 const drive=[box('left_support',[1,0,5],[3,12,11],'copper'),box('right_support',[13,0,5],[15,12,11],'copper')];
 for(const[i,side]of ['west','east'].entries())drive[i].faces[side]={texture:'#shaft',uv:[1,0,15,12]};
 const crank=[box('upper_cross_shaft',[0,23,7],[16,25,9],'metal'),box('small_crank',[10.5,22,5],[11.5,26,11],'brass'),box('crank_pin',[10,21.5,7.5],[12,22.5,8.5],'metal')];
 const link=[box('short_connecting_rod',[10.25,18,7.5],[10.75,22,8.5],'metal')];
 for(const[name,elements]of Object.entries({body:base,piston:tallPiston,drive,crank,link}))save('models/block/piston_fluid_pump/'+name+'.json',wrap(elements));
 const upper=drive.map(e=>{const v=structuredClone(e);v.from[1]+=16;v.to[1]+=16;return v;});
 const item=wrap([...base,...upper,...tallPiston,...crank,...link]);
 item.display={gui:{rotation:[30,225,0],translation:[0,-4,0],scale:[.4,.4,.4]},ground:{translation:[0,3,0],scale:[.3,.3,.3]},fixed:{rotation:[0,180,0],translation:[0,-4,0],scale:[.35,.35,.35]},thirdperson_righthand:{rotation:[75,45,0],translation:[0,0,0],scale:[.25,.25,.25]},firstperson_righthand:{rotation:[0,45,0],translation:[0,-2,0],scale:[.35,.35,.35]}};
 save('models/item/reciprocating_fluid_pump.json',item);
 save('blockstates/reciprocating_pump_drive.json',{variants:{'axis=x':{model:'ccd:block/piston_fluid_pump/drive'},'axis=z':{model:'ccd:block/piston_fluid_pump/drive',y:90},'axis=y':{model:'ccd:block/piston_fluid_pump/drive'}}});
 save('models/item/reciprocating_pump_drive.json',{parent:'ccd:block/piston_fluid_pump/drive'});
 const variants={};for(const[facing,y]of Object.entries({north:0,east:90,south:180,west:270,up:0,down:0}))variants['facing='+facing]={model:'ccd:block/piston_fluid_pump/body',...(y?{y}:{})};
 save('blockstates/reciprocating_fluid_pump.json',{variants});
 const out='outputs/piston-fluid-pump';fs.mkdirSync(out,{recursive:true});
 const full=await mesh('ccd:item/reciprocating_fluid_pump');const panels=[];
 for(const[i,view]of [[0,[30,24,-32]],[1,[30,28,32]]])panels.push({input:await render(full,600,700,view,{center:[8,15,8],scale:21}),left:i*600,top:0});
 await sharp({create:{width:1200,height:700,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile(out+'/preview.png');
}main().catch(e=>{console.error(e);process.exitCode=1;});
