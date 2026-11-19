// Use Create's dial artwork and needles; move only the dial surfaces outside the valve casing.
// Usage: node tools/preview-redstone-valve.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render,model:loadModel}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd');
const output=path.join(root,'outputs/redstone-fluid-valve');fs.mkdirSync(output,{recursive:true});
const planes=texture=>[
 {name:'outer_dial_front',from:[2,14.01,2],to:[14,14.01,14],faces:{up:{texture,uv:[2,2,14,14]}}},
 {name:'outer_dial_back',from:[2,1.99,2],to:[14,1.99,14],faces:{down:{texture,uv:[2,14,14,2]}}},
];
const glassPlanes=()=>[
 {name:'outer_glass_up',from:[2,14.12,2],to:[14,14.12,14],faces:{up:{texture:'#glass',uv:[1,1,7,7]}}},
 {name:'outer_glass_down',from:[2,1.88,2],to:[14,1.88,14],faces:{down:{texture:'#glass',uv:[1,1,7,7]}}},
];
function rotate(v,axis,angle,point){
 const a=angle*Math.PI/180,c=Math.cos(a),s=Math.sin(a),p=v.map(n=>n-(point?8:0)),q=[...p];
 const [i,j]=axis==='x'?[1,2]:[2,0];q[i]=p[i]*c-p[j]*s;q[j]=p[i]*s+p[j]*c;
 return q.map(n=>n+(point?8:0));
}
const transform=(faces,axis,angle)=>faces.map(f=>({...f,vertices:f.vertices.map(v=>rotate(v,axis,angle,true)),n:rotate(f.n,axis,angle,false)}));
function sealShaftFaces(original){
 const m=JSON.parse(JSON.stringify(original));let sealed=0;
 for(const e of m.elements)if(e.from.join(',')==='2,2,2'&&e.to.join(',')==='14,14,14')
  for(const face of Object.values(e.faces))if(face.texture==='#2'&&face.uv.join(',')==='9,1,15,7'){
   face.texture='#copper';face.uv=[0,0,16,16];sealed++;
  }
 if(sealed!==2)throw Error('Expected exactly two native shaft openings');
 return m.elements;
}
async function main(){
 for(const orientation of ['horizontal','vertical'])for(const state of ['closed','open']){
  const name='block_'+orientation+'_'+state,id='create:block/fluid_valve/'+name,native=loadModel(id);
  fs.writeFileSync(path.join(assets,'models/block/redstone_fluid_valve/'+name+'.json'),
   JSON.stringify({parent:id,ambientocclusion:false,render_type:'minecraft:cutout_mipped',
    textures:{copper:'create:block/copper_underside'},elements:sealShaftFaces(native)},null,2)+'\n');
 }
 for(const state of ['closed','open'])fs.writeFileSync(path.join(assets,'models/block/redstone_fluid_valve/dial_'+state+'.json'),
  JSON.stringify({ambientocclusion:false,render_type:'minecraft:cutout_mipped',textures:{dial:'create:block/valve_'+state,glass:'create:block/fluid_valve'},elements:[...planes('#dial'),...glassPlanes()]},null,2)+'\n');
 const nativeItem=loadModel('create:block/fluid_valve/item'),nativePointer=loadModel('create:block/fluid_valve/pointer');
 const pointerElements=JSON.parse(JSON.stringify(nativePointer.elements));
 for(const e of pointerElements){e.name='outer_native_pointer';for(const endpoint of [e.from,e.to])endpoint[1]=8+(endpoint[1]-8)*6.02/5.5;}
 const item={parent:'create:block/fluid_valve/item',ambientocclusion:false,render_type:'minecraft:cutout_mipped',textures:{...nativeItem.textures,
  copper:'create:block/copper_underside',dial:'create:block/valve_open',glass:'create:block/fluid_valve'},
  elements:[...sealShaftFaces(nativeItem).filter(e=>e.name!=='Axis'),...planes('#dial'),...glassPlanes(),...transformPointerElements(pointerElements)]};
 fs.writeFileSync(path.join(assets,'models/item/redstone_fluid_valve.json'),JSON.stringify(item,null,2)+'\n');
 const pointer=(await mesh('create:block/fluid_valve/pointer')).map(f=>({...f,vertices:f.vertices.map(([x,y,z])=>[x,8+(y-8)*6.02/5.5,z])}));
 const panels=[];
 for(const [i,state,value]of [[0,'closed',0],[1,'open',1]]){
  const shell=await mesh('ccd:block/redstone_fluid_valve/block_horizontal_'+state);
  let dial=await mesh('ccd:block/redstone_fluid_valve/dial_'+state);
  dial=transform(transform(dial,'y',90),'x',90);
  const needles=transform(transform(pointer,'y',90-90*value),'x',90);
  const buffer=await render([...shell,...dial,...needles],600,460,[22,20,34],{center:[8,8,8],scale:24});
  panels.push({input:buffer,left:i*600,top:85});
 }
 const labels=Buffer.from(`<svg width="1200" height="610" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="600" y="40" font-size="27">红石流体阀门 · 铜板封闭原轴口</text><text x="600" y="74" font-size="16">整面原生铜板 · 保留流体口、刻度与指针 · 不渲染传动轴</text><text x="300" y="570" font-size="18">关闭</text><text x="900" y="570" font-size="18">开启</text><text x="600" y="600" font-size="14" fill="#677070">实际模型离线预览 / 不包含游戏环境光遮蔽，未启动客户端验证</text></g></svg>`);
 await sharp({create:{width:1200,height:610,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),dialPlanes:2,nativeArtwork:true}));
}
function transformPointerElements(elements){
 // Inventory native dial points along local Z when closed. Its open state is a quarter turn.
 return elements.map(e=>({...e,rotation:{origin:[8,8,8],axis:'y',angle:0},
  from:[e.from[2],e.from[1],e.from[0]],to:[e.to[2],e.to[1],e.to[0]]}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
