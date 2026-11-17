// Original 16x16 pixel artwork, referencing Create's pump and copper material palette.
// Usage: node tools/create-booster-pump.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render,model:loadModel}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd');
const textures=path.join(assets,'textures/block/booster_pump'),output=path.join(root,'outputs/booster-pump');
for(const dir of [textures,output])fs.mkdirSync(dir,{recursive:true});
const copper=[[70,32,24],[144,73,49],[154,80,56],[167,90,64],[178,98,71],[194,107,76],[200,116,86],[214,123,91],[227,130,108]];
const image=fn=>{const data=Buffer.alloc(1024);for(let y=0;y<16;y++)for(let x=0;x<16;x++)data.set([...fn(x,y),255],(y*16+x)*4);return data;};
const paint=(data,x,y,color)=>data.set([...color,255],(y*16+x)*4);
// Twelve-by-twelve copper shell. Seams, reinforcement and wear are pixel clusters, not noise.
const panel=[
 '278777676872',
 '765655565657',
 '853555655546',
 '765444554657',
 '754544455546',
 '765554544657',
 '754454554546',
 '765544455657',
 '754545554546',
 '765555655657',
 '254333434532',
 '121222121211',
];
const body=image(()=>copper[5]);
for(let y=0;y<12;y++)for(let x=0;x<12;x++)paint(body,x+2,y+2,copper[Number(panel[y][x])]);
// The ring opening reveals the recessed, dark pump hub behind it.
for(let y=6;y<10;y++)for(let x=6;x<10;x++)paint(body,x,y,copper[x===6||y===6?1:0]);
const elements=[];
function box(name,from,to,texture){
 const [x,y,z]=from,[X,Y,Z]=to,face=(uv)=>({uv,texture:'#'+texture});
 elements.push({name,from,to,faces:{
  north:face([X,16-Y,x,16-y]),south:face([x,16-Y,X,16-y]),
  east:face([Z,16-Y,z,16-y]),west:face([z,16-Y,Z,16-y]),
  up:face([x,z,X,Z]),down:face([x,Z,X,z]),
 }});
}
box('copper_housing',[2,2,2],[14,14,14],'body');
// Import the native pipe mouths verbatim, including their UV rotation and culling.
// No hand-drawn substitute, no CCD copy of the upstream pipe texture.
const nativeParts=[];
for(const side of ['up','down'])for(const part of ['rim_connector','rim']){
 const id='create:block/fluid_pipe/'+part+'/'+side,native=loadModel(id);
 nativeParts.push(id);
 for(const [i,original]of native.elements.entries()){
  const element=JSON.parse(JSON.stringify(original));element.name='native_'+part+'_'+side+'_'+i;
  for(const spec of Object.values(element.faces))spec.texture='#pipe';
  elements.push(element);
 }
}
const model={parent:'minecraft:block/block',render_type:'minecraft:cutout_mipped',
 textures:{body:'ccd:block/booster_pump/body',pipe:'create:block/pipes'},elements};
model.textures.particle=model.textures.body;
const rotate=(v,axis,angle,point)=>{
 const a=angle*Math.PI/180,c=Math.cos(a),s=Math.sin(a),p=v.map(n=>n-(point?8:0)),q=[...p];
 const [i,j]=axis==='x'?[1,2]:[2,0];q[i]=p[i]*c-p[j]*s;q[j]=p[i]*s+p[j]*c;
 return q.map(n=>n+(point?8:0));
};
const transform=(faces,axis,angle)=>faces.map(f=>({...f,vertices:f.vertices.map(v=>rotate(v,axis,angle,true)),n:rotate(f.n,axis,angle,false)}));
async function main(){
 for(const [name,data]of Object.entries({body}))
  await sharp(data,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,name+'.png'));
 fs.writeFileSync(path.join(assets,'models/block/booster_pump.json'),JSON.stringify(model,null,2)+'\n');
 const shell=await mesh('ccd:block/booster_pump'),shaft=await mesh('create:block/shaft_half');
 const full=[...shell,...shaft,...transform(shaft,'y',180),...transform(shaft,'y',90),...transform(shaft,'y',-90)];
 const horizontal=transform(full,'x',90),panels=[];
 for(const [i,faces,view]of [[0,horizontal,[26,20,-34]],[1,horizontal,[-26,20,34]],[2,full,[26,20,-34]]]){
  const buffer=await render(faces,500,480,view,{center:[8,8,8],scale:23});
  await sharp(buffer).toFile(path.join(output,['horizontal-front','horizontal-rear','vertical'][i]+'.png'));
  panels.push({input:buffer,left:i*500,top:100});
 }
 const labels=Buffer.from(`<svg width="1500" height="660" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei, sans-serif" text-anchor="middle" fill="#303535"><text x="750" y="42" font-size="29">增压泵 · 原生接口版</text><text x="750" y="78" font-size="16">铜壳 · Create 原生流体管口与旋转半轴 · 无额外轴承座或铆钉</text><text x="250" y="612" font-size="18">水平管路 · 正面</text><text x="750" y="612" font-size="18">水平管路 · 背面</text><text x="1250" y="612" font-size="18">竖直管路</text><text x="750" y="644" font-size="14" fill="#677070">实际模型离线渲染 / 铜壳为 16×16 原创像素贴图，接口使用原生资源</text></g></svg>`);
 await sharp({create:{width:1500,height:660,channels:4,background:'#e5e8e2'}})
  .composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'preview.png'));
 await sharp(path.join(textures,'body.png')).resize(256,256,{kernel:'nearest'}).png().toFile(path.join(output,'textures.png'));
 fs.writeFileSync(path.join(output,'model.meta.json'),JSON.stringify({originalArtwork:'copper housing texture only',textureSize:[16,16],
  paletteReference:['Create pump','Create copper_casing'],elements:elements.length,
  fluidAxis:'y before blockstate rotation',nativePipeModels:nativeParts,nativePipeTexture:'create:block/pipes',
  nativeShaftModel:'create:block/shaft_half',customBearings:0,rivets:0,previewOnlyShafts:true},null,2)+'\n');
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),elements:elements.length,customTextures:1}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
