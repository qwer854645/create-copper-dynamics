// Closed copper body plus shared native port partials; no per-direction model copies.
const fs=require('fs'),path=require('path'),sharp=require('sharp');
const {model,mesh,render}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'../src/main/resources');
const save=(p,d)=>{p=path.join(root,p);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(d,null,2)+'\n');};
const faces=['north','south','east','west','up','down'];
const wrap=elements=>({parent:'create:block/block',render_type:'minecraft:cutout_mipped',textures:{copper:'create:block/copper_underside',pipe:'create:block/pipes',shaft:'ccd:block/booster_pump/shaft_face',input:'ccd:block/three_way_valve/common',output:'ccd:block/three_way_valve/selected',particle:'create:block/copper_underside'},elements});
const cube=(name,from,to,texture,uv)=>({name,from,to,faces:Object.fromEntries(faces.map(s=>[s,{texture,uv}]))});
const body=wrap([cube('copper_housing',[2,2,2],[14,14,14],'#copper',[2,2,14,14])]);
save('assets/ccd/models/block/omni_pump/body.json',body);
for(const [mode,source]of [['input','common_north'],['output','selected_north']]){
 save('assets/ccd/models/block/omni_pump/'+mode+'.json',{parent:'ccd:block/three_way_valve/'+source});
}
const socket=cube('shaft_socket',[3,3,.5],[13,13,2],'#copper',[3,3,13,13]);socket.faces.north={texture:'#shaft',uv:[3,3,13,13]};
save('assets/ccd/models/block/omni_pump/shaft.json',wrap([socket]));
const rotations={north:{},south:{y:180},east:{y:90},west:{y:270},up:{x:90},down:{x:270}};
const multipart=[{apply:{model:'ccd:block/omni_pump/body'}}];
for(const side of faces)for(const [mode,id]of [[1,'input'],[2,'output'],[3,'shaft']])multipart.push({when:{[side]:String(mode)},apply:{model:'ccd:block/omni_pump/'+id,...rotations[side]}});
save('assets/ccd/blockstates/omni_pump.json',{multipart});save('assets/ccd/models/item/omni_pump.json',{parent:'ccd:block/omni_pump/body'});
save('data/ccd/recipe/omni_pump.json',{type:'minecraft:crafting_shapeless',ingredients:[{item:'ccd:booster_pump'},{item:'create:precision_mechanism'}],result:{id:'ccd:omni_pump',count:1}});
save('data/ccd/loot_table/blocks/omni_pump.json',{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:'ccd:omni_pump'}],conditions:[{condition:'minecraft:survives_explosion'}]}]});
for(const p of ['data/create/tags/block/wrench_pickup.json','data/minecraft/tags/block/mineable/pickaxe.json']){const d=JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));if(!d.values.includes('ccd:omni_pump'))d.values.push('ccd:omni_pump');save(p,d);}
for(const locale of ['zh_cn','en_us']){
 const p=`assets/ccd/lang/${locale}.json`,d=JSON.parse(fs.readFileSync(path.join(root,p),'utf8')),zh=locale==='zh_cn';
 d['block.ccd.omni_pump']=zh?'万向泵':'Omnidirectional Pump';
 for(const [i,label]of (zh?['关闭','输入','输出','应力口']:['Closed','Input','Output','Shaft']).entries())d['ccd.omni_pump.mode.'+i]=label;
 d['ccd.omni_pump.face']='%s: %s';
 for(const [side,label]of Object.entries(zh?{north:'北',south:'南',east:'东',west:'西',up:'上',down:'下'}:{north:'North',south:'South',east:'East',west:'West',up:'Up',down:'Down'}))d['ccd.direction.'+side]=label;
 d['block.ccd.omni_pump.tooltip.summary']=zh?'用_扳手_逐面切换输入、输出、应力口和关闭。默认六面关闭。':'Use a _Wrench_ on each face to cycle input, output, shaft and closed. All faces start closed.';save(p,d);
}
async function main(){const out='outputs/omni-pump';fs.mkdirSync(out,{recursive:true});const base=await mesh('ccd:item/omni_pump'),panels=[{input:await render(base,500,480,[26,23,-34],{center:[8,8,8],scale:23}),left:0,top:0}];const configured=[...base,...await mesh('ccd:block/omni_pump/input')];panels.push({input:await render(configured,500,480,[26,23,-34],{center:[8,8,8],scale:23}),left:500,top:0});await sharp({create:{width:1000,height:480,channels:4,background:'#e5e8e2'}}).composite(panels).png().toFile(out+'/preview.png');}
require('node:child_process').execFileSync(process.execPath,[path.join(__dirname,'sync-omni-pump-item.cjs'),process.argv[2]],{stdio:'inherit'});
main().catch(e=>{console.error(e);process.exitCode=1;});
