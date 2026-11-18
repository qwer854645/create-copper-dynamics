// Deterministic palette replacement and a textured orthographic render of the real Minecraft model.
// Usage: node tools/preview-fluid-packager.cjs <Create source root>
// Requires sharp (NODE_PATH may point to an existing installation).
const fs = require('node:fs');
const path = require('node:path');
const sharp = require('sharp');
const root = path.resolve(__dirname, '..');
const reference = path.resolve(process.argv[2] || '');
if (!process.argv[2]) throw Error('Pass the extracted Create source root.');
const sourceTextures = path.join(reference, 'assets/create/textures/block');
const destination = path.join(root, 'src/main/resources/assets/ccd/textures/block/fluid_packager');
const output = path.join(root, 'outputs/fluid-packager-copper');
fs.mkdirSync(destination, { recursive: true });
fs.mkdirSync(output, { recursive: true });

// Explicit blue/purple material ramp only. Neutral metal, green-grey frame, wood and redstone stay intact.
const palette = new Map([
  ['5,4,14', [14,7,5]], ['12,10,21', [27,14,10]], ['18,15,27', [42,23,17]],
  ['30,24,39', [63,32,22]], ['42,36,53', [96,46,31]], ['43,43,49', [117,56,36]],
  ['53,52,61', [144,73,49]], ['61,60,72', [167,90,64]], ['65,65,80', [178,98,71]],
  ['68,72,90', [194,107,76]], ['73,86,105', [214,123,91]], ['75,94,113', [227,130,108]],
]);
const add = (a,b) => a.map((v,i)=>v+b[i]);
const sub = (a,b) => a.map((v,i)=>v-b[i]);
const mul = (a,s) => a.map(v=>v*s);
const dot = (a,b) => a.reduce((s,v,i)=>s+v*b[i],0);
const cross = (a,b) => [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
const normal = a => mul(a,1/Math.hypot(...a));
const textureCache = new Map();

function asset(id, kind) {
  const [ns, name] = id.includes(':') ? id.split(':') : ['minecraft', id];
  const base = ns === 'ccd' ? path.join(root,'src/main/resources/assets/ccd') : path.join(reference,'assets',ns);
  return path.join(base,kind,name + (kind==='models' ? '.json' : '.png'));
}
function model(id) {
  if (id === 'block/block' || id === 'minecraft:block/block') return {textures:{},elements:[]};
  const own = JSON.parse(fs.readFileSync(asset(id,'models'),'utf8'));
  const parent = own.parent ? model(own.parent) : {textures:{},elements:[]};
  return {...parent,...own,textures:{...parent.textures,...own.textures}};
}
async function texture(id) {
  if (!textureCache.has(id)) textureCache.set(id, await sharp(asset(id,'textures')).ensureAlpha().raw().toBuffer({resolveWithObject:true}));
  return textureCache.get(id);
}
function faceVertices(from,to,face) {
  const [x,y,z]=from, [X,Y,Z]=to;
  return {
    down:[[x,y,Z],[x,y,z],[X,y,z],[X,y,Z]], up:[[x,Y,z],[x,Y,Z],[X,Y,Z],[X,Y,z]],
    north:[[X,Y,z],[X,y,z],[x,y,z],[x,Y,z]], south:[[x,Y,Z],[x,y,Z],[X,y,Z],[X,Y,Z]],
    west:[[x,Y,z],[x,y,z],[x,y,Z],[x,Y,Z]], east:[[X,Y,Z],[X,y,Z],[X,y,z],[X,Y,z]],
  }[face];
}
function rotate(p,rotation) {
  if (!rotation || !rotation.angle) return p;
  const o=rotation.origin, v=sub(p,o), a=rotation.angle*Math.PI/180, c=Math.cos(a), s=Math.sin(a);
  const axes={x:[1,2],y:[2,0],z:[0,1]}, [i,j]=axes[rotation.axis];
  const q=[...v];q[i]=v[i]*c-v[j]*s;q[j]=v[i]*s+v[j]*c;
  if (rotation.rescale) {q[i]/=Math.abs(c);q[j]/=Math.abs(c);}
  return add(q,o);
}
async function mesh(id) {
  const m=model(id), result=[];
  for (const element of m.elements) for (const [face,spec] of Object.entries(element.faces)) {
    let name=spec.texture, guard=0;
    while (name.startsWith('#') && guard++<20) name=m.textures[name.slice(1)];
    const pixels=await texture(name);
    const vertices=faceVertices(element.from,element.to,face).map(p=>rotate(p,element.rotation));
    const n=normal(cross(sub(vertices[1],vertices[0]),sub(vertices[2],vertices[0])));
    if (!Number.isFinite(n[0])) continue;
    if (!spec.uv) throw Error('Explicit UVs required for this preview: '+id);
    const [u,v,U,V]=spec.uv, base=[[u,v],[u,V],[U,V],[U,v]];
    const uv=base.map((_,i)=>base[(i+(spec.rotation||0)/90)%4]);
    result.push({vertices,n,uv,pixels});
  }
  return result;
}
function render(faces,width,height,view,options={}) {
  const camera=normal(view), right=normal(cross([0,1,0],camera)), up=cross(camera,right);
  const scale=options.scale || 15.5, center=options.center || [8,7,8];
  const project=p=>{const d=sub(p,center);return [width/2+dot(d,right)*scale,height/2-dot(d,up)*scale,dot(d,camera)];};
  const rgba=Buffer.alloc(width*height*4), depth=new Float64Array(width*height).fill(-Infinity);
  // Opt-in sorted alpha compositing for glass/water studies; existing opaque previews stay unchanged.
  const layers=options.alphaBlend ? new Array(width*height) : null;
  const edge=(a,b,x,y)=>(x-a[0])*(b[1]-a[1])-(y-a[1])*(b[0]-a[0]);
  for (const f of faces) {
    if (dot(f.n,camera)<=0.00001) continue;
    const p=f.vertices.map(project), light=.62+.36*Math.max(0,dot(f.n,normal([-1,3,-2])));
    for (const ids of [[0,1,2],[0,2,3]]) {
      const [a,b,c]=ids.map(i=>p[i]), area=edge(a,b,c[0],c[1]);
      if (Math.abs(area)<1e-8) continue;
      const x0=Math.max(0,Math.floor(Math.min(a[0],b[0],c[0]))), x1=Math.min(width-1,Math.ceil(Math.max(a[0],b[0],c[0])));
      const y0=Math.max(0,Math.floor(Math.min(a[1],b[1],c[1]))), y1=Math.min(height-1,Math.ceil(Math.max(a[1],b[1],c[1])));
      for (let y=y0;y<=y1;y++) for (let x=x0;x<=x1;x++) {
        const w0=edge(b,c,x+.5,y+.5)/area, w1=edge(c,a,x+.5,y+.5)/area, w2=1-w0-w1;
        if (Math.min(w0,w1,w2)<-1e-7) continue;
        const z=w0*a[2]+w1*b[2]+w2*c[2], index=y*width+x;
        if (!layers && z<=depth[index]) continue;
        const weights=[w0,w1,w2], coords=[0,1].map(axis=>weights.reduce((sum,w,j)=>sum+w*f.uv[ids[j]][axis],0));
        const info=f.pixels.info, tx=Math.max(0,Math.min(info.width-1,Math.floor(coords[0]/16*info.width)));
        const ty=Math.max(0,Math.min(info.height-1,Math.floor(coords[1]/16*info.height))), src=(ty*info.width+tx)*4;
        const alpha=f.pixels.data[src+3];
        if (!alpha || (!layers && alpha<128)) continue;
        if (layers) {
          (layers[index] ||= []).push([z,...[0,1,2].map(channel=>Math.round(f.pixels.data[src+channel]*light)),alpha]);
          continue;
        }
        depth[index]=z;
        for(let channel=0;channel<3;channel++) rgba[index*4+channel]=Math.round(f.pixels.data[src+channel]*light);
        rgba[index*4+3]=255;
      }
    }
  }
  if (layers) for(let index=0;index<layers.length;index++) {
    const fragments=layers[index];if(!fragments)continue;
    fragments.sort((a,b)=>a[0]-b[0]);
    let last=-Infinity,a=0,color=[0,0,0];
    for(const [z,r,g,b,alpha] of fragments) {
      // Both triangles of a quad can cover their shared edge; blend it only once.
      if(Math.abs(z-last)<1e-7)continue;last=z;
      const opacity=alpha/255,out=opacity+a*(1-opacity);
      color=[r,g,b].map((v,c)=>(v*opacity+color[c]*a*(1-opacity))/out);a=out;
    }
    for(let c=0;c<3;c++)rgba[index*4+c]=Math.round(color[c]);
    rgba[index*4+3]=Math.round(a*255);
  }
  return sharp(rgba,{raw:{width,height,channels:4}}).png().toBuffer();
}
async function main() {
  const changes=[];
  for (const file of fs.readdirSync(sourceTextures).filter(n=>n.startsWith('packager_')&&n.endsWith('.png'))) {
    const input=path.join(sourceTextures,file), {data,info}=await sharp(input).ensureAlpha().raw().toBuffer({resolveWithObject:true});
    const updated=Buffer.from(data);let count=0;
    for(let i=0;i<data.length;i+=4) {
      const replacement=data[i+3] && palette.get([...data.subarray(i,i+3)].join(','));
      if (!replacement) continue;
      for(let channel=0;channel<3;channel++) updated[i+channel]=replacement[channel];
      count++;
    }
    for(let i=3;i<data.length;i+=4) if(updated[i]!==data[i]) throw Error('Alpha changed in '+file);
    await sharp(updated,{raw:{width:info.width,height:info.height,channels:4}}).png().toFile(path.join(destination,file));
    if(fs.existsSync(input+'.mcmeta')) fs.copyFileSync(input+'.mcmeta',path.join(destination,file+'.mcmeta'));
    changes.push({file,width:info.width,height:info.height,recoloredPixels:count});
  }
  const original=await mesh('create:block/packager/item'), copper=await mesh('ccd:item/fluid_packager');
  const panels=[];
  for(const [row,view] of [[0,[26,20,-34]],[1,[-26,20,34]]]) {
    for(const [col,faces] of [[0,original],[1,copper]]) {
      const buffer=await render(faces,640,430,view);
      const file=(col?'copper':'original')+'-'+(row?'rear':'front')+'.png';
      await sharp(buffer).toFile(path.join(output,file));
      panels.push({input:buffer,left:col*640,top:100+row*440});
    }
  }
  const labels=Buffer.from(`<svg width="1280" height="1000" xmlns="http://www.w3.org/2000/svg">
    <g font-family="Microsoft YaHei, sans-serif" fill="#303535" text-anchor="middle">
      <text x="320" y="48" font-size="27">Create 原版</text><text x="960" y="48" font-size="27">CCD 铜色替换</text>
      <text x="640" y="86" font-size="16" fill="#677070">原模型 · 深蓝色阶替换为铜色 · 同角度与光照</text>
      <text x="640" y="985" font-size="15" fill="#677070">离线模型预览 / 上：开口侧　下：舱门侧</text>
    </g><path d="M640 115V950" stroke="#c8ccc8"/></svg>`);
  await sharp({create:{width:1280,height:1000,channels:4,background:'#e5e8e2'}})
    .composite([...panels,{input:labels,left:0,top:0}]).png().toFile(path.join(output,'comparison.png'));
  fs.writeFileSync(path.join(output,'palette.json'),JSON.stringify({source:'Create 6.0.10-280',palette:Object.fromEntries(palette),textures:changes},null,2)+'\n');
  console.log(JSON.stringify({output:path.join(output,'comparison.png'),textures:changes.length,recoloredPixels:changes.reduce((sum,x)=>sum+x.recoloredPixels,0)}));
}
module.exports={mesh,render,model};
if (require.main === module) main().catch(e=>{console.error(e);process.exitCode=1;});
