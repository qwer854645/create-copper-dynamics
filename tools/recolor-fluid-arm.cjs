// Native Create arm joints, copper materials, brass base accents and a CCD syringe head.
// Usage: node tools/recolor-fluid-arm.cjs <extracted Create source root>
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render,model:loadModel}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..'),assets=path.join(root,'src/main/resources/assets/ccd'),ref=path.resolve(process.argv[2]);
const textures=path.join(assets,'textures/block/fluid_arm'),models=path.join(assets,'models/block/fluid_arm');
const output=path.join(root,'outputs/fluid-arm-copper');for(const dir of [textures,models,output])fs.mkdirSync(dir,{recursive:true});
const copper=[[63,32,22],[96,46,31],[123,61,41],[144,73,49],[167,90,64],[194,107,76],[214,123,91],[227,130,108],[239,156,128]];
const textureNames=['mechanical_arm'];
const parts=['lower_body','upper_body'];
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
async function assembly(copperMode,goggles=false,filled=false){
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
 if(copperMode&&filled){
  const s=[.625,0,0,0,0,.625,0,0,0,0,.625,0,0,0,0,1];
  const held=multiply(multiply(multiply(multiply(m,rotation('x',90)),translate(0,-10,0)),s),translate(-8,-8,-8));
  result.push(...transformed(await sampleFluid(),held));
 }
 return result;
}
function box(name,from,to,texture){
 const sizes=to.map((v,i)=>v-from[i]);
 const axes={north:[0,1],south:[0,1],east:[2,1],west:[2,1],up:[0,2],down:[0,2]};
 return {name,from,to,faces:Object.fromEntries(Object.entries(axes).map(([side,[u,v]])=>[side,{texture:'#'+texture,uv:[2,2,2+sizes[u],2+sizes[v]]}]))};
}
function syringe(){
 const elements=[box('Connector',[-2,-2,-4],[2,2,-2.5],'metal'),
  box('Rear copper collar',[-3,-3,-5],[3,3,-4],'copper'),
  box('Front copper collar',[-3,-3,-12],[3,3,-11],'copper'),
  box('Recessed empty barrel',[-2,-2,-10.9],[2,2,-5.1],'core'),
  box('Needle shoulder',[-1.5,-1.5,-13],[1.5,1.5,-12],'copper'),
  box('Needle',[-.5,-.5,-15],[.5,.5,-13],'metal')];
 for(const x of [-3,2])for(const y of [-3,2])elements.push(box('Copper window rail',[x,y,-11],[x+1,y+1,-5],'copper'));
 // Clear windows use separate opaque highlight planes: no alpha quads in native solid buffers.
 for(const axis of ['x','y'])for(const sign of [-1,1]){
  for(const [a,b,z,Z]of [[-2,-1,-10,-8],[-1,0,-8,-7],[1,2,-6,-5]]){
   const from=axis==='x'?[sign*2.8,a,z]:[a,sign*2.8,z];
   const to=axis==='x'?[sign*2.8,b,Z]:[b,sign*2.8,Z];
   const element=box('Observation window highlight',from,to,'glass');
   const directions=axis==='x'?['east','west']:['up','down'];
   element.faces=Object.fromEntries(directions.map(side=>[side,{texture:'#glass',uv:[0,0,1,1]}]));
   elements.push(element);
  }
 }
 return elements;
}
async function sampleFluid(){
 const faces=await mesh('ccd:item/fluid_arm_cargo');
 const data=Buffer.alloc(16*16*4);
 for(let y=0;y<16;y++)for(let x=0;x<16;x++)data.set((y<5&&x>3&&x<10)?[96,161,216,255]:[47,105,182,255],(y*16+x)*4);
 return faces.map(face=>({...face,pixels:{data,info:{width:16,height:16,channels:4}}}));
}
async function makeSyringeAssets(){
 const native=await sharp(path.join(ref,'assets/create/textures/block/mechanical_arm.png')).ensureAlpha().raw().toBuffer({resolveWithObject:true});
 const accent=Buffer.from(native.data),colored=await sharp(path.join(textures,'mechanical_arm.png')).ensureAlpha().raw().toBuffer();
 for(let y=16;y<=21;y++)for(let x=0;x<16;x++)if(y===16||y===21||x===0||x===15){const i=(y*32+x)*4;colored.copy(accent,i,i,i+4);}
 await sharp(accent,{raw:{width:32,height:32,channels:4}}).png().toFile(path.join(textures,'mechanical_arm_base.png'));
 for(const [name,base]of [['core',[32,40,42]],['metal',[133,145,144]],['glass',[182,218,218]]]){
  const data=Buffer.alloc(16*16*4);
  for(let y=0;y<16;y++)for(let x=0;x<16;x++){
   let rgb=base,alpha=255;
   if(name!=='glass'){const delta=(x<5&&y<6)?12:(x>9&&y>8)?-12:0;rgb=base.map(c=>c+delta);}
   data.set([...rgb,alpha],(y*16+x)*4);
  }
  await sharp(data,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,name+'.png'));
 }
 const head=syringe(),textureMap={'5':'ccd:block/fluid_arm/mechanical_arm',copper:'ccd:block/faucet/body',metal:'ccd:block/fluid_arm/metal',core:'ccd:block/fluid_arm/core',glass:'ccd:block/fluid_arm/glass'};
 for(const goggles of [false,true]){
  const name=goggles?'claw_base_goggles':'claw_base',parent='create:block/mechanical_arm/'+name,nativeModel=loadModel(parent);
  save(path.join(models,name+'.json'),{parent,textures:textureMap,elements:[nativeModel.elements[0],...head,...(goggles?nativeModel.elements.slice(3):[])]});
 }
 for(const name of ['upper_claw_grip','lower_claw_grip'])save(path.join(models,name+'.json'),{parent:'minecraft:block/block',elements:[]});
 save(path.join(assets,'models/block/fluid_arm.json'),{parent:'create:block/mechanical_arm/block',textures:{'3':'ccd:block/fluid_arm/mechanical_arm_base'}});
 const nativeItem=loadModel('create:block/mechanical_arm/item'),itemElements=JSON.parse(JSON.stringify(nativeItem.elements.slice(0,11)));
 for(const element of itemElements)if(element.name==='Base'||element.name==='Joint 1')
  for(const face of Object.values(element.faces))if(face.texture==='#5')face.texture=element.name==='Base'?'#base_accent':'#original_arm';
 const joint=JSON.parse(JSON.stringify(loadModel('create:block/mechanical_arm/claw_base').elements[0]));delete joint.rotation;
 const itemHead=[joint,...head].map(element=>({...element,from:element.from.map((v,i)=>v+[8,9,4][i]),to:element.to.map((v,i)=>v+[8,9,4][i])}));
 save(path.join(assets,'models/item/fluid_arm.json'),{parent:'create:block/mechanical_arm/item',textures:{...textureMap,original_arm:'create:block/mechanical_arm',base_accent:'ccd:block/fluid_arm/mechanical_arm_base'},elements:[...itemElements,...itemHead]});
 const fluid=box('In-transit fluid only',[3.76,6.72,3.76],[12.24,15.68,12.24],'fluid');
 for(const face of Object.values(fluid.faces))face.tintindex=0;
 save(path.join(assets,'models/item/fluid_arm_cargo.json'),{parent:'minecraft:block/block',textures:{fluid:'ccd:block/fluid_arm/core',particle:'ccd:block/fluid_arm/core'},elements:[fluid],display:{fixed:{rotation:[0,0,0],translation:[0,0,0],scale:[1,1,1]}}});
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
  // Rank mapping: the k-th source tone (by luminance) becomes the k-th copper tone.
  // The old absolute-threshold ladder ([40,60,85,105,135,165,190,215]) mapped Create's
  // eight copper tones (lum 86.4..149.0) onto indices [3,3,3,4,4,4,5,5], i.e. only three
  // distinct outputs, which flattened every copper strap on the arm.
  const warm=new Map();
  for(let i=0;i<data.length;i+=4){
   const [r,g,b]=data.subarray(i,i+3);
   if(!data[i+3]||r<g||g<b||r-b<20||g-b<8)continue;
   warm.set((r<<16)|(g<<8)|b,[r,g,b]);
  }
  const tones=[...warm.values()].sort((p,q)=>
   (.2126*p[0]+.7152*p[1]+.0722*p[2])-(.2126*q[0]+.7152*q[1]+.0722*q[2]));
  const rank=new Map(tones.map((c,k)=>[(c[0]<<16)|(c[1]<<8)|c[2],
   copper[tones.length===1?Math.floor(copper.length/2):Math.round(k*(copper.length-1)/(tones.length-1))]]));
  const updated=Buffer.from(data);let count=0;
  for(let i=0;i<data.length;i+=4){
   const [r,g,b]=data.subarray(i,i+3);
   const hit=rank.get((r<<16)|(g<<8)|b);if(!hit)continue;
   updated.set(hit,i);palette.set([r,g,b].join(','),hit);count++;
  }
  for(let i=3;i<data.length;i+=4)if(updated[i]!==data[i])throw Error('Alpha changed');
  await sharp(updated,{raw:{width:info.width,height:info.height,channels:4}}).png().toFile(path.join(textures,name+'.png'));
  changes.push({name,width:info.width,height:info.height,recoloredPixels:count});
 }
 for(const name of parts){const parent='create:block/mechanical_arm/'+name;save(path.join(models,name+'.json'),{parent,textures:overrides(parent)});}
 await makeSyringeAssets();
 const normal=await assembly(false),colored=await assembly(true),goggles=await assembly(true,true,true),panels=[];
 for(const [i,faces,view]of [[0,normal,[28,20,-38]],[1,colored,[28,20,-38]],[2,goggles,[-28,18,38]]]){
  const options=framing([...normal,...colored,...goggles],600,650,view),buffer=await render(faces,600,650,view,options);
  await sharp(buffer).toFile(path.join(output,['original','copper','copper-rear-goggles'][i]+'.png'));panels.push({input:buffer,left:i*600,top:95});
 }
 const labels=Buffer.from(`<svg width="1800" height="820" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="900" y="40" font-size="29">移液臂 · 黄铜底座配铜边 / 针管式末端</text><text x="900" y="78" font-size="16">铜色箍环与框架 · 四面观察窗 · 短针嘴 · 保留原生关节</text><text x="300" y="776" font-size="19">Create 原版</text><text x="900" y="776" font-size="19">CCD · 空载针管</text><text x="1500" y="776" font-size="19">反向视角 · 装水示意</text><text x="900" y="808" font-size="14" fill="#677070">实际模型离线预览 / 蓝色仅为装水示意，游戏内使用实际流体贴图与颜色 / 未启动游戏</text></g></svg>`);
 await sharp({create:{width:1800,height:820,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'comparison.png'));
 save(path.join(output,'palette.json'),{source:'Create 6.0.10-280',palette:Object.fromEntries(palette),textures:changes,
  geometry:'native arm joints; new fixed syringe head; base copper rim',preserved:['mostly brass base','brass rotating base','neutral metal','cog','elevator_pulley texture','goggles texture','native atlas alpha']});
 const head=await mesh('ccd:block/fluid_arm/claw_base'),fluid=await sampleFluid();
 const s=[.625,0,0,0,0,.625,0,0,0,0,.625,0,0,0,0,1];
 const held=multiply(multiply(multiply(rotation('x',90),translate(0,-10,0)),s),translate(-8,-8,-8));
 const filled=[...head,...transformed(fluid,held)],closeups=[];
 for(const [i,faces]of [head,filled].entries())closeups.push({input:await render(faces,650,600,[24,19,-30],framing(head,650,600,[24,19,-30])),left:i*650,top:65});
 const caption=Buffer.from('<svg width="1300" height="720" xmlns="http://www.w3.org/2000/svg"><g fill="#303535" font-family="Microsoft YaHei,sans-serif" text-anchor="middle"><text x="650" y="40" font-size="27">针管式末端 · 观察窗与铜框</text><text x="325" y="700" font-size="20">空载</text><text x="975" y="700" font-size="20">装水示意</text></g></svg>');
 await sharp({create:{width:1300,height:720,channels:4,background:'#e5e8e2'}}).composite([...closeups,{input:caption,left:0,top:0}]).png().toFile(path.join(output,'syringe.png'));
 console.log(JSON.stringify({preview:path.join(output,'comparison.png'),textures:changes}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
