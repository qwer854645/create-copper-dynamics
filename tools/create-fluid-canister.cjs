// Original 16x16 pixel textures and four integer-grid cuboids. Pass the extracted Create source root.
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..');
const textures=path.join(root,'src/main/resources/assets/ccd/textures/item/fluid_canister');
const output=path.join(root,'outputs/fluid-canister');
fs.mkdirSync(textures,{recursive:true});fs.mkdirSync(output,{recursive:true});
// Copper and neutral frame colors sampled from Create's fluid tank / copper casing.
// Original pixel clusters using the full native copper ramp, not broad flat fills.
const copper=[[144,73,49],[154,80,56],[167,90,64],[178,98,71],[194,107,76],[200,116,86],[214,123,91],[227,130,108]];
const steel=[[53,52,56],[63,62,66],[79,79,79],[89,88,88],[103,97,97],[152,151,151]];
function image(pixel){
 const data=Buffer.alloc(16*16*4);
 for(let y=0;y<16;y++)for(let x=0;x<16;x++)data.set([...pixel(x,y),255],(y*16+x)*4);
 return data;
}
function paint(data,x,y,color){data.set([...color,255],(y*16+x)*4);}
// Visible 10x9 body panel. Broad color clusters follow Create's copper panels.
const panel=[
 '2766776651',
 '1655445430',
 '1544355431',
 '2643454430',
 '1533545541',
 '1544543440',
 '2655445541',
 '1433233320',
 '1021112100',
];
const ramp=Object.fromEntries(copper.map((color,i)=>[i,color]));
const side=image(()=>copper[4]);
for(let y=0;y<panel.length;y++)for(let x=0;x<panel[y].length;x++)
 paint(side,3+x,6+y,ramp[panel[y][x]]);
const front=Buffer.from(side),back=Buffer.from(side);
// The entire front panel inside the one-pixel frame is clear glass.
const windowRows=Array(7).fill('11111111');
const windowPixels=[];
for(let y=0;y<windowRows.length;y++)for(let x=0;x<8;x++)windowPixels.push([4+x,7+y]);
for(const [x,y]of windowPixels)for(const [dx,dy]of [[1,0],[-1,0],[0,1],[0,-1]])
 if(!windowPixels.some(([a,b])=>a===x+dx&&b===y+dy))
  paint(front,x+dx,y+dy,dy<0?copper[6+x%2]:dx<0?copper[3+y%2]:copper[(x+y)%2]);
const band=image((x,y)=>steel[(x===2||x===13)?0:(x+y)%5===0?4:(x+y)%3===0?2:3]);
const cap=image((x,y)=>{
 if(x===6||y===6)return steel[4];
 if(x===9||y===9)return steel[0];
 return steel[3];
});
// Original 12x12 plate: industrial-iron folded seams, recessed reinforcement and four fasteners.
const plate=[
 '055444444550',
 '532222222235',
 '421333333124',
 '423444444324',
 '423433334324',
 '423432234324',
 '423432234324',
 '423433334324',
 '423444444324',
 '421333333124',
 '532222222235',
 '000111111000',
];
const flat=image(()=>steel[3]);
for(let y=0;y<12;y++)for(let x=0;x<12;x++)paint(flat,2+x,2+y,steel[Number(plate[y][x])]);
const fluid=image(()=>[56,62,61]);
const glass=image(()=>[182,207,205]);
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
cube('bottom_rim',[2,0,2],[14,1,14],'bottom_band','bottom_flat');
cube('lid',[2,10,2],[14,11,14],'band','flat');
cube('sealed_cap',[6,11,6],[10,12,10],'band','cap');
// Cut the front wall geometrically. Solid native package rendering never depends on alpha discard.
delete elements[0].faces.north;
function frontPixel(name,x,y,z,texture,tint,width=1){
 elements.push({name,from:[x,15-y,z],to:[x+width,16-y,z],faces:{north:{
  texture:'#'+texture,uv:[x+width,y,x,y+1],...(tint===undefined?{}:{tintindex:tint})}}});
}
const inWindow=(x,y)=>windowPixels.some(([a,b])=>a===x&&b===y);
for(let y=6;y<15;y++)for(let x=3;x<13;){
 const isWindow=inWindow(x,y);let end=x+1;
 while(end<13&&inWindow(end,y)===isWindow)end++;
 if(isWindow){
  frontPixel('window_back_'+x+'_'+y,x,y,3.04,'fluid',undefined,end-x);
  frontPixel('fluid_'+x+'_'+y,x,y,3.02,'fluid',0,end-x);
 }else frontPixel('front_'+x+'_'+y,x,y,3,'front',undefined,end-x);
 x=end;
}
for(const [x,y]of [[5,8],[4,9]])frontPixel('glass_glint_'+x+'_'+y,x,y,2.99,'glass');
const model={parent:'create:item/package_transforms',render_type:'minecraft:solid',
 textures:Object.fromEntries(['side','front','back','band','cap','flat','fluid','glass'].map(name=>[name,'ccd:item/fluid_canister/'+name])),
 elements,display:{gui:{rotation:[25,35,0],translation:[0,1,0],scale:[.95,.95,.95]},
 ground:{translation:[0,3,0],scale:[.5,.5,.5]},fixed:{rotation:[0,0,0],translation:[0,0,0],scale:[.5,.5,.5]}}};
model.textures.particle=model.textures.side;
model.textures.bottom_band='ccd:item/fluid_canister/bottom_band';
model.textures.bottom_flat='ccd:item/fluid_canister/bottom_flat';
async function main(){
 for(const [name,data]of Object.entries({side,front,back,band,cap,flat,fluid,glass}))
  await sharp(data,{raw:{width:16,height:16,channels:4}}).png().toFile(path.join(textures,name+'.png'));
 fs.writeFileSync(path.join(root,'src/main/resources/assets/ccd/models/item/fluid_package.json'),JSON.stringify(model,null,2)+'\n');
 const faces=await mesh('ccd:item/fluid_package'),panels=[];
 // Offline illustration only. Runtime uses the contained FluidStack's actual sprite, tint and level.
 const previewFluid=image((x,y)=>[42+(x+y)%3*4,92+(x*2+y)%4*4,173+(x+y*3)%3*6]);
 let faceIndex=0;
 for(const element of elements)for(const spec of Object.values(element.faces)){
  if(spec.tintindex===0)faces[faceIndex].pixels={data:previewFluid,info:{width:16,height:16,channels:4}};
  faceIndex++;
 }
 for(const [i,view]of [[0,[24,16,-34]],[1,[0,8,-40]],[2,[-24,20,34]]]){
  const buffer=await render(faces,500,480,view,{center:[8,6,8],scale:25});
  await sharp(buffer).toFile(path.join(output,['front-angle','front','rear-angle'][i]+'.png'));
  panels.push({input:buffer,left:i*500,top:95});
 }
 const labels=Buffer.from(`<svg width="1500" height="640" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei, sans-serif" text-anchor="middle" fill="#303535">
 <text x="750" y="44" font-size="29">流体罐 · Fluid Canister</text>
 <text x="750" y="78" font-size="16" fill="#677070">Create 铜色阶与像素纹理 · 正面整面玻璃观察窗</text>
 <text x="250" y="603" font-size="18">正面斜视</text><text x="750" y="603" font-size="18">正面</text><text x="1250" y="603" font-size="18">背面斜视</text>
 <text x="750" y="632" font-size="14" fill="#677070">模型离线预览 / 窗内蓝色流体为示意，游戏中读取实际流体</text></g></svg>`);
 await sharp({create:{width:1500,height:640,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:labels,left:0,top:0}])
  .png().toFile(path.join(output,'preview.png'));
 const plateViews=[];
 for(const [i,view]of [[0,[12,35,-20]],[1,[12,-35,-20]]]){
  const buffer=await render(faces,500,480,view,{center:[8,6,8],scale:25});
  plateViews.push({input:buffer,left:i*500,top:65});
 }
 const plateLabels=Buffer.from(`<svg width="1000" height="580" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei, sans-serif" text-anchor="middle" fill="#303535"><text x="500" y="38" font-size="25">上下端板 · 工业铁块风格的像素接缝与铆接</text><text x="250" y="565" font-size="18">顶部</text><text x="750" y="565" font-size="18">底部</text></g></svg>`);
 await sharp({create:{width:1000,height:580,channels:4,background:'#e5e8e2'}})
  .composite([...plateViews,{input:plateLabels,left:0,top:0}]).png().toFile(path.join(output,'top-bottom.png'));
 // Show the source pixels as well as the rendered model, enlarged only by nearest neighbor.
 const textureNames=['side','front','back','band','cap','flat','fluid','glass'];
 await sharp({create:{width:128,height:16,channels:4,background:'#e5e8e2'}})
  .composite(textureNames.map((name,i)=>({input:path.join(textures,name+'.png'),left:i*16,top:0})))
  .png().toFile(path.join(output,'textures-source.png'));
 await sharp(path.join(output,'textures-source.png')).resize(1280,160,{kernel:'nearest'})
  .png().toFile(path.join(output,'textures.png'));
 fs.writeFileSync(path.join(output,'model.meta.json'),JSON.stringify({name:'Fluid Canister',model:'ccd:item/fluid_package',
  originalArtwork:true,textureSize:[16,16],transportBounds:[12,12,12],capacityMillibuckets:4000,
  elements:elements.length,integerPixelLayout:true,texelsPerModelUnit:1,paletteColors:16,
  paletteReference:['Create copper_casing','Create fluid_tank','Create industrial_iron_block_top'],textureOrder:textureNames,
  windowFrame:'copper',endPlates:'original pixel seams, reinforcement and fasteners',
  fluidTintedFaces:windowRows.length,windowPixels:windowPixels.length,windowBounds:[4,2,12,9],
  opaqueTextures:true,glass:'full front panel inside one-pixel frame, with pixel glints',
  dynamicFluid:['still sprite','stack tint','fill level'],previewFluid:'illustrative blue'},null,2)+'\n');
 console.log(JSON.stringify({preview:path.join(output,'preview.png'),elements:elements.length,textures:textureNames.length}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
