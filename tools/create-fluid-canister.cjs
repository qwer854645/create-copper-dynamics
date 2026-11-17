// Original 16x16 pixel textures and four integer-grid cuboids. Pass the extracted Create source root.
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..');
const textures=path.join(root,'src/main/resources/assets/ccd/textures/item/fluid_canister');
const output=path.join(root,'outputs/fluid-canister');
fs.mkdirSync(textures,{recursive:true});fs.mkdirSync(output,{recursive:true});
// Copper and neutral frame colors sampled from Create's fluid tank / copper casing.
// Original pixel layout, four copper tones and three frame tones; no gradients.
const copper=[[144,73,49],[178,98,71],[194,107,76],[227,130,108]];
const steel=[[68,65,64],[94,91,90],[114,108,105]];
function image(pixel){
 const data=Buffer.alloc(16*16*4);
 for(let y=0;y<16;y++)for(let x=0;x<16;x++)data.set([...pixel(x,y),255],(y*16+x)*4);
 return data;
}
function paint(data,x,y,color){data.set([...color,255],(y*16+x)*4);}
// Visible 10x9 body panel. Broad color clusters follow Create's copper panels.
const panel=[
 'DHHHHHHHHD',
 'DSBBBBBBSD',
 'DSBBBBBBBD',
 'DSBBBSBBBD',
 'DSBBSSBBBD',
 'DSBBBBBBBD',
 'DSBBBBBBBD',
 'DSBBBBBBSD',
 'DDDDDDDDDD',
];
const ramp={D:copper[0],S:copper[1],B:copper[2],H:copper[3]};
const side=image(()=>copper[2]);
for(let y=0;y<panel.length;y++)for(let x=0;x<panel[y].length;x++)
 paint(side,3+x,6+y,ramp[panel[y][x]]);
const front=Buffer.from(side),back=Buffer.from(side);
// Six-by-seven pixel label; the four-by-five drop below is drawn pixel by pixel.
for(let y=7;y<=13;y++)for(let x=5;x<=10;x++)paint(front,x,y,[223,211,168]);
const drop=['0010','0110','1111','1111','0110'];
for(let y=0;y<drop.length;y++)for(let x=0;x<4;x++)if(drop[y][x]==='1')paint(front,6+x,8+y,steel[0]);
const band=image(x=>x===2||x===13?steel[0]:steel[1]);
const cap=image((x,y)=>{
 if(x===6||y===6)return steel[2];
 if(x===9||y===9)return steel[0];
 return steel[1];
});
const flat=image((x,y)=>{
 if(x===13||y===13)return steel[0];
 if(x===2||y===2)return steel[2];
 return steel[1];
});
const elements=[];
function cube(name,from,to,sideTexture,topTexture){
 const [x,y,z]=from,[X,Y,Z]=to;
 const face=(texture,uv)=>({texture:'#'+texture,uv});
 // One texture pixel per model unit; no stretched full-texture strips on the rims.
 elements.push({name,from,to,faces:{
  north:face(sideTexture==='body'?'front':sideTexture,[x,16-Y,X,16-y]),
  south:face(sideTexture==='body'?'back':sideTexture,[x,16-Y,X,16-y]),
  east:face(sideTexture==='body'?'side':sideTexture,[z,16-Y,Z,16-y]),
  west:face(sideTexture==='body'?'side':sideTexture,[z,16-Y,Z,16-y]),
  up:face(topTexture,[x,z,X,Z]),down:face(topTexture,[x,z,X,Z]),
 }});
}
cube('copper_body',[3,1,3],[13,10,13],'body','side');
cube('bottom_rim',[2,0,2],[14,1,14],'band','flat');
cube('lid',[2,10,2],[14,11,14],'band','flat');
cube('sealed_cap',[6,11,6],[10,12,10],'band','cap');
const model={parent:'create:item/package_transforms',render_type:'minecraft:solid',
 textures:Object.fromEntries(['side','front','back','band','cap','flat'].map(name=>[name,'ccd:item/fluid_canister/'+name])),
 elements,display:{gui:{rotation:[25,35,0],translation:[0,1,0],scale:[.95,.95,.95]},
 ground:{translation:[0,3,0],scale:[.5,.5,.5]},fixed:{rotation:[0,0,0],translation:[0,0,0],scale:[.5,.5,.5]}}};
model.textures.particle=model.textures.side;
async function main(){
 for(const [name,data]of Object.entries({side,front,back,band,cap,flat}))
  await sharp(data,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,name+'.png'));
 fs.writeFileSync(path.join(root,'src/main/resources/assets/ccd/models/item/fluid_package.json'),JSON.stringify(model,null,2)+'\n');
 const faces=await mesh('ccd:item/fluid_package'),panels=[];
 for(const [i,view]of [[0,[24,16,-34]],[1,[0,8,-40]],[2,[-24,20,34]]]){
  const buffer=await render(faces,500,480,view,{center:[8,6,8],scale:25});
  await sharp(buffer).toFile(path.join(output,['front-angle','front','rear-angle'][i]+'.png'));
  panels.push({input:buffer,left:i*500,top:95});
 }
 const labels=Buffer.from(`<svg width="1500" height="640" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei, sans-serif" text-anchor="middle" fill="#303535">
 <text x="750" y="44" font-size="29">流体罐 · Fluid Canister</text>
 <text x="750" y="78" font-size="16" fill="#677070">16×16 像素贴图 · 简单方块造型 · 铜色罐身与液滴标记</text>
 <text x="250" y="603" font-size="18">正面斜视</text><text x="750" y="603" font-size="18">正面</text><text x="1250" y="603" font-size="18">背面斜视</text>
 <text x="750" y="632" font-size="14" fill="#677070">实际模型离线渲染 / 最近邻纹理采样</text></g></svg>`);
 await sharp({create:{width:1500,height:640,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}])
  .png().toFile(path.join(output,'preview.png'));
 // Show the source pixels as well as the rendered model, enlarged only by nearest neighbor.
 const textureNames=['side','front','back','band','cap','flat'];
 await sharp({create:{width:96,height:16,channels:4,background:'#e5e8e2'}})
  .composite(textureNames.map((name,i)=>({input:path.join(textures,name+'.png'),left:i*16,top:0})))
  .png().toFile(path.join(output,'textures-source.png'));
 await sharp(path.join(output,'textures-source.png')).resize(960,160,{kernel:'nearest'})
  .png().toFile(path.join(output,'textures.png'));
 fs.writeFileSync(path.join(output,'model.meta.json'),JSON.stringify({name:'Fluid Canister',model:'ccd:item/fluid_package',
  originalArtwork:true,textureSize:[16,16],transportBounds:[12,12,12],capacityMillibuckets:4000,
  elements:elements.length,integerGeometry:true,texelsPerModelUnit:1,paletteColors:8,
  paletteReference:['Create copper_casing','Create fluid_tank'],textureOrder:textureNames,
  fluidTintedFaces:0,opaqueTextures:true},null,2)+'\n');
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),elements:elements.length,textures:6}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
