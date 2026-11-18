// CCD pixel-art housing, native pipe mouths, and existing four-sided filter interaction.
// Usage: node tools/create-fluid-transmitter.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render,model:loadModel}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd');
const textures=path.join(assets,'textures/block/fluid_transmitter'),output=path.join(root,'outputs/fluid-transmitter');
for(const dir of [textures,output])fs.mkdirSync(dir,{recursive:true});
const copper=[[96,46,31],[144,73,49],[154,80,56],[167,90,64],[178,98,71],[194,107,76],[214,123,91],[227,130,108]];
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
// Like the pump's flow indication, show the actual output direction, not a decorative mark.
// The markings use a 32px atlas (as native pipe/pump textures do), so a proper five-row
// arrowhead fits in the strip beside the filter without enlarging or moving that filter.
const arrow=['0000100','0000110','1111111','0000110','0000100'];
const arrowColor=[123,61,41];
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
 // Native smart-pipe filter face: the north face of the filter bezel uses UV [8,8,16,16].
 // Adapt that exact 8x8 face to four flush-mounted filter positions; retain existing hit boxes.
 const reference=path.resolve(process.argv[2]);
 const {data:filter,info}=await sharp(path.join(reference,'assets/create/textures/block/smart_pipe_2.png'))
  .ensureAlpha().raw().toBuffer({resolveWithObject:true});
 if(info.width!==16||info.height!==16)throw Error('Unexpected smart-pipe filter atlas size');
 for(let y=0;y<8;y++)for(let x=0;x<8;x++){
  const source=((y+8)*16+x+8)*4,destination=((y+4)*16+x+4)*4;
  for(const data of [side,top])filter.copy(data,destination,source,source+4);
 }
 await sharp(body,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,'body.png'));
 for(const [name,data]of Object.entries({side,top})){
  const pixels=await sharp(data,{raw:{width:16,height:16,channels:4}}).resize(32,32,{kernel:'nearest'}).raw().toBuffer();
  for(let y=0;y<arrow.length;y++)for(let x=0;x<arrow[y].length;x++)if(arrow[y][x]==='1'){
   const u=name==='side'?12+x:2+y,v=name==='side'?2+y:12+x;
   pixels.set([...arrowColor,255],(v*32+u)*4);
  }
  await sharp(pixels,{raw:{width:32,height:32,channels:4}}).png().toFile(path.join(textures,name+'.png'));
 }
  require('node:child_process').execFileSync('py',[path.join(root,'tools/symmetrize-fluid-housings.py'),'fluid_transmitter'],{stdio:'inherit'});
  fs.writeFileSync(path.join(assets,'models/block/fluid_transmitter.json'),JSON.stringify(model,null,2)+'\n');
 const faces=await mesh('ccd:block/fluid_transmitter'),panels=[];
 for(const [i,viewFaces,view]of [[0,faces,[26,20,-34]],[1,faces,[-26,20,34]],[2,transform(faces,'x',-90),[26,20,-34]]]){
  const buffer=await render(viewFaces,500,480,view,{center:[8,8,8],scale:22});
  await sharp(buffer).toFile(path.join(output,['input-side','output-side','vertical'][i]+'.png'));
  panels.push({input:buffer,left:i*500,top:100});
 }
 const labels=Buffer.from(`<svg width="1500" height="660" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei, sans-serif" text-anchor="middle" fill="#303535"><text x="750" y="42" font-size="29">流体传输器 · 原生过滤面板与流向箭头</text><text x="750" y="78" font-size="16">智能流体管道的黄铜面板 · 深铜色小箭头指向输出端</text><text x="250" y="612" font-size="18">输入侧</text><text x="750" y="612" font-size="18">输出侧</text><text x="1250" y="612" font-size="18">竖直摆放</text><text x="750" y="644" font-size="14" fill="#677070">实际模型离线渲染 / 面板未设置流体过滤，游戏中由原生渲染器显示过滤图标</text></g></svg>`);
 await sharp({create:{width:1500,height:660,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 const layers=[];for(const [i,n]of ['body','side','top'].entries())layers.push({input:await sharp(path.join(textures,n+'.png')).resize(256,256,{kernel:'nearest'}).png().toBuffer(),left:i*272,top:0});
 await sharp({create:{width:800,height:256,channels:4,background:'#e5e8e2'}}).composite(layers).png().toFile(path.join(output,'textures.png'));
 fs.writeFileSync(path.join(output,'model.meta.json'),JSON.stringify({originalArtwork:'CCD copper housing and arrow only; filter panel belongs to Create',textureSizes:{body:[16,16],side:[32,32],top:[32,32]},
  nativeFilterFace:'create:block/smart_pipe_2 UV [8,8,16,16]',filterMounting:'four flush faces, existing interaction centers',
  directionArrow:{mode:'small, always visible on housing',sizeInModelUnits:[3.5,2.5],shape:arrow,color:arrowColor,pointsTo:'output'},
  nativePipeModels:nativeParts,nativePipeTexture:'create:block/pipes',elements:elements.length,
  outputDirection:'local +Z, transformed by existing blockstate',filterFaces:['east','west','up','down'],
  filterCenters:[[16,8,8],[0,8,8],[8,16,8],[8,0,8]],rivets:0,shafts:0,fluidStorage:0},null,2)+'\n');
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),elements:elements.length,textures:3}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
