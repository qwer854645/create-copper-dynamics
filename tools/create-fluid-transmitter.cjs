// CCD pixel-art housing, native pipe mouths, and existing four-sided filter interaction.
// Usage: node tools/create-fluid-transmitter.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render,model:loadModel}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd');
const textures=path.join(assets,'textures/block/fluid_transmitter'),output=path.join(root,'outputs/fluid-transmitter');
for(const dir of [textures,output])fs.mkdirSync(dir,{recursive:true});
const copper=[[96,46,31],[144,73,49],[154,80,56],[167,90,64],[178,98,71],[194,107,76],[214,123,91],[227,130,108]];
const steel=[[53,52,56],[79,79,79],[103,97,97],[127,133,132],[159,163,153]];
const image=fn=>{const data=Buffer.alloc(1024);for(let y=0;y<16;y++)for(let x=0;x<16;x++)data.set([...fn(x,y),255],(y*16+x)*4);return data;};
const paint=(data,x,y,color)=>data.set([...color,255],(y*16+x)*4);
// Uneven connected patches: no alternating-dot pattern, rivets or decorative fasteners.
const patches=[
 '166667776642','655666655543','655556655433','655555544433',
 '655555444443','655544444543','655444455553','554444555543',
 '554455555433','454555544433','343333333221','122211111110',
];
const body=image((x,y)=>copper[y===0?6:x===0?5:y===15||x===15?0:y===14||x===14?2:5]);
for(let y=0;y<12;y++)for(let x=0;x<12;x++)paint(body,x+2,y+2,copper[Number(patches[y][x])]);
const side=Buffer.from(body),top=Buffer.from(body);
for(const data of [side,top]){
 for(let y=4;y<12;y++)for(let x=4;x<12;x++){
  let color=x===4||y===4?copper[6]:x===11||y===11?copper[0]
   :x===5||y===5?steel[0]:x===10||y===10?steel[1]:steel[y===6?3:2];
  paint(data,x,y,color);
 }
}
// Positive local Z is the output. Rotate the small mark with each side's physical UV basis.
const arrow=['10','01','10'];
for(let y=0;y<3;y++)for(let x=0;x<2;x++)if(arrow[y][x]==='1'){
 paint(side,7+x,1+y,steel[0]);paint(top,1+y,7+x,steel[0]);
}
const elements=[{name:'copper_housing',from:[0,0,3],to:[16,16,13],faces:{
 north:{texture:'#body',uv:[16,0,0,16]},south:{texture:'#body',uv:[0,0,16,16]},
 east:{texture:'#side',uv:[13,0,3,16]},west:{texture:'#side',uv:[3,0,13,16]},
 up:{texture:'#top',uv:[0,3,16,13]},down:{texture:'#top',uv:[0,13,16,3]},
}}];
const nativeParts=[];
for(const face of ['north','south'])for(const part of ['rim_connector','rim']){
 const id='create:block/fluid_pipe/'+part+'/'+face,native=loadModel(id);nativeParts.push(id);
 for(const [i,original]of native.elements.entries()){
  const element=JSON.parse(JSON.stringify(original));element.name='native_'+part+'_'+face+'_'+i;
  for(const spec of Object.values(element.faces))spec.texture='#pipe';elements.push(element);
 }
}
const model={parent:'minecraft:block/block',render_type:'minecraft:cutout_mipped',textures:{
 body:'ccd:block/fluid_transmitter/body',side:'ccd:block/fluid_transmitter/side',
 top:'ccd:block/fluid_transmitter/top',pipe:'create:block/pipes',particle:'ccd:block/fluid_transmitter/body'},elements};
function rotate(v,axis,angle,point){
 const a=angle*Math.PI/180,c=Math.cos(a),s=Math.sin(a),p=v.map(n=>n-(point?8:0)),q=[...p];
 const [i,j]=axis==='x'?[1,2]:[2,0];q[i]=p[i]*c-p[j]*s;q[j]=p[i]*s+p[j]*c;
 return q.map(n=>n+(point?8:0));
}
const transform=(faces,axis,angle)=>faces.map(f=>({...f,vertices:f.vertices.map(v=>rotate(v,axis,angle,true)),n:rotate(f.n,axis,angle,false)}));
async function main(){
 for(const [name,data]of Object.entries({body,side,top}))await sharp(data,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,name+'.png'));
 fs.writeFileSync(path.join(assets,'models/block/fluid_transmitter.json'),JSON.stringify(model,null,2)+'\n');
 const faces=await mesh('ccd:block/fluid_transmitter'),panels=[];
 for(const [i,viewFaces,view]of [[0,faces,[26,20,-34]],[1,faces,[-26,20,34]],[2,transform(faces,'x',-90),[26,20,-34]]]){
  const buffer=await render(viewFaces,500,480,view,{center:[8,8,8],scale:22});
  await sharp(buffer).toFile(path.join(output,['input-side','output-side','vertical'][i]+'.png'));
  panels.push({input:buffer,left:i*500,top:100});
 }
 const labels=Buffer.from(`<svg width="1500" height="660" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei, sans-serif" text-anchor="middle" fill="#303535"><text x="750" y="42" font-size="29">流体传输器 · 铜壳与原生接口</text><text x="750" y="78" font-size="16">不规则铜色纹理 · 原生管口 · 四侧过滤面板与方向标记</text><text x="250" y="612" font-size="18">输入侧</text><text x="750" y="612" font-size="18">输出侧</text><text x="1250" y="612" font-size="18">竖直摆放</text><text x="750" y="644" font-size="14" fill="#677070">实际模型离线渲染 / 面板未设置流体过滤，游戏中由原生渲染器显示过滤图标</text></g></svg>`);
 await sharp({create:{width:1500,height:660,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 const layers=[];for(const [i,n]of ['body','side','top'].entries())layers.push({input:await sharp(path.join(textures,n+'.png')).resize(256,256,{kernel:'nearest'}).png().toBuffer(),left:i*272,top:0});
 await sharp({create:{width:800,height:256,channels:4,background:'#e5e8e2'}}).composite(layers).png().toFile(path.join(output,'textures.png'));
 fs.writeFileSync(path.join(output,'model.meta.json'),JSON.stringify({originalArtwork:'CCD copper housing/filter panel only',textureSize:[16,16],
  nativePipeModels:nativeParts,nativePipeTexture:'create:block/pipes',elements:elements.length,
  outputDirection:'local +Z, transformed by existing blockstate',filterFaces:['east','west','up','down'],
  filterCenters:[[16,8,8],[0,8,8],[8,16,8],[8,0,8]],rivets:0,shafts:0,fluidStorage:0},null,2)+'\n');
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),elements:elements.length,textures:3}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
