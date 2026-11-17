// Native Create arm geometry and animation transforms, with warm material pixels recolored to copper.
// Usage: node tools/recolor-fluid-arm.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render,model:loadModel}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd'),ref=path.resolve(process.argv[2]);
const textures=path.join(assets,'textures/block/fluid_arm'),models=path.join(assets,'models/block/fluid_arm');
const output=path.join(root,'outputs/fluid-arm-copper');for(const dir of [textures,models,output])fs.mkdirSync(dir,{recursive:true});
const copper=[[63,32,22],[96,46,31],[123,61,41],[144,73,49],[167,90,64],[194,107,76],[214,123,91],[227,130,108],[239,156,128]];
const thresholds=[40,60,85,105,135,165,190,215];
const textureNames=['mechanical_arm'];
const parts=['lower_body','upper_body','claw_base','claw_base_goggles','upper_claw_grip','lower_claw_grip'];
const save=(file,data)=>fs.writeFileSync(file,JSON.stringify(data,null,2)+'\n');
function overrides(id){
 const m=loadModel(id),result={};
 for(const [key,value]of Object.entries(m.textures))if(textureNames.some(name=>value==='create:block/'+name))
  result[key]='ccd:block/fluid_arm/'+value.split('/').pop();
 return result;
}
// Row-major matrices applied in the same order as ArmRenderer's public transform helpers.
const identity=()=>[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1];
const multiply=(a,b)=>Array.from({length:16},(_,i)=>{let sum=0;for(let k=0;k<4;k++)sum+=a[Math.floor(i/4)*4+k]*b[k*4+i%4];return sum;});
const translate=(x,y,z)=>[1,0,0,x,0,1,0,y,0,0,1,z,0,0,0,1];
function rotation(axis,angle){const a=angle*Math.PI/180,c=Math.cos(a),s=Math.sin(a);return axis==='x'?[1,0,0,0,0,c,-s,0,0,s,c,0,0,0,0,1]:[c,0,s,0,0,1,0,0,-s,0,c,0,0,0,0,1];}
const apply=(m,v,point)=>[0,1,2].map(row=>m[row*4]*v[0]+m[row*4+1]*v[1]+m[row*4+2]*v[2]+(point?m[row*4+3]:0));
const transformed=(faces,m)=>faces.map(f=>({...f,vertices:f.vertices.map(v=>apply(m,v,true)),n:apply(m,f.n,false)}));
async function assembly(copperMode,goggles=false){
 const id=name=>copperMode?'ccd:block/fluid_arm/'+name:'create:block/mechanical_arm/'+name;
 const result=[...await mesh(copperMode?'ccd:block/fluid_arm':'create:block/mechanical_arm/block'),...await mesh('create:block/mechanical_arm/cog')];
 let m=multiply(translate(8,8,8),translate(0,4,0));m=multiply(m,rotation('y',-30));
 result.push(...transformed(await mesh('create:block/mechanical_arm/base'),m));
 m=multiply(m,translate(0,2,0));m=multiply(m,rotation('x',90)); // lower=-45, plus native 135
 result.push(...transformed(await mesh(id('lower_body')),m));
 m=multiply(m,translate(0,0,-14));m=multiply(m,rotation('x',-45)); // upper=45, minus native 90
 result.push(...transformed(await mesh(id('upper_body')),m));
 m=multiply(m,translate(0,0,-15));m=multiply(m,rotation('x',-45)); // head=0, minus native 45
 result.push(...transformed(await mesh(id(goggles?'claw_base_goggles':'claw_base')),m));
 for(const [name,flip]of [['lower_claw_grip',1],['upper_claw_grip',-1]])
  result.push(...transformed(await mesh(id(name)),multiply(m,translate(0,-flip,-6))));
 return result;
}
function framing(faces,width,height,view){
 const norm=v=>{const n=Math.hypot(...v);return v.map(x=>x/n)},cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],dot=(a,b)=>a.reduce((s,x,i)=>s+x*b[i],0);
 const camera=norm(view),right=norm(cross([0,1,0],camera)),up=cross(camera,right),vertices=faces.flatMap(f=>f.vertices);
 const bounds=[0,1,2].map(i=>[Math.min(...vertices.map(v=>v[i])),Math.max(...vertices.map(v=>v[i]))]);
 const center=bounds.map(([a,b])=>(a+b)/2);
 const span=axis=>{const values=vertices.map(v=>dot(v.map((x,i)=>x-center[i]),axis));return Math.max(...values)-Math.min(...values)};
 return {center,scale:Math.min((width-70)/span(right),(height-70)/span(up))};
}
async function main(){
 const changes=[],palette=new Map();
 for(const name of textureNames){
  const {data,info}=await sharp(path.join(ref,'assets/create/textures/block',name+'.png')).ensureAlpha().raw().toBuffer({resolveWithObject:true});
  const updated=Buffer.from(data);let count=0;
  for(let i=0;i<data.length;i+=4){
   const [r,g,b]=data.subarray(i,i+3);
   if(!data[i+3]||r<g||g<b||r-b<20||g-b<8)continue;
   const value=.2126*r+.7152*g+.0722*b;let index=thresholds.findIndex(t=>value<t);if(index<0)index=8;
   updated.set(copper[index],i);palette.set([r,g,b].join(','),copper[index]);count++;
  }
  for(let i=3;i<data.length;i+=4)if(updated[i]!==data[i])throw Error('Alpha changed');
  await sharp(updated,{raw:{width:info.width,height:info.height,channels:4}}).png().toFile(path.join(textures,name+'.png'));
  changes.push({name,width:info.width,height:info.height,recoloredPixels:count});
 }
 for(const name of parts){const parent='create:block/mechanical_arm/'+name;save(path.join(models,name+'.json'),{parent,textures:overrides(parent)});}
 save(path.join(assets,'models/block/fluid_arm.json'),{parent:'create:block/mechanical_arm/block'});
 const nativeItem=loadModel('create:block/mechanical_arm/item'),itemElements=JSON.parse(JSON.stringify(nativeItem.elements));
 for(const element of itemElements)if(element.name==='Base'||element.name==='Joint 1')
  for(const face of Object.values(element.faces))if(face.texture==='#5')face.texture='#original_arm';
 save(path.join(assets,'models/item/fluid_arm.json'),{parent:'create:block/mechanical_arm/item',
  textures:{...overrides('create:block/mechanical_arm/item'),original_arm:'create:block/mechanical_arm'},elements:itemElements});
 const normal=await assembly(false),colored=await assembly(true),goggles=await assembly(true,true),panels=[];
 for(const [i,faces,view]of [[0,normal,[28,20,-38]],[1,colored,[28,20,-38]],[2,goggles,[-28,18,38]]]){
  const options=framing([...normal,...colored,...goggles],600,650,view),buffer=await render(faces,600,650,view,options);
  await sharp(buffer).toFile(path.join(output,['original','copper','copper-rear-goggles'][i]+'.png'));panels.push({input:buffer,left:i*600,top:95});
 }
 const labels=Buffer.from(`<svg width="1800" height="820" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="900" y="40" font-size="29">移液臂 · 铜色臂节与黄铜底座</text><text x="900" y="78" font-size="16">底座与旋转底座保持黄铜 · 臂节、夹爪外壳换铜色 · 保留灰色机械件与护目镜</text><text x="300" y="776" font-size="19">Create 原版</text><text x="900" y="776" font-size="19">CCD 铜色版</text><text x="1500" y="776" font-size="19">铜色背面 · 护目镜保留</text><text x="900" y="808" font-size="14" fill="#677070">原生模型与关节变换的离线渲染 / 空爪静止姿态，未启动游戏</text></g></svg>`);
 await sharp({create:{width:1800,height:820,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'comparison.png'));
 save(path.join(output,'palette.json'),{source:'Create 6.0.10-280',palette:Object.fromEntries(palette),textures:changes,
  geometry:'native parents; item geometry retains original base materials',preserved:['brass base','brass rotating base','neutral metal','cog','elevator_pulley texture','goggles texture','all alpha']});
 console.log(JSON.stringify({preview:path.join(output,'comparison.png'),textures:changes}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
