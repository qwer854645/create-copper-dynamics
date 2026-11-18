// Read-only preview of the current checked-in assets. Never regenerates textures.
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
const root=path.resolve(__dirname,'..');
const entries=[
 ['增压泵','ccd:item/booster_pump',[8,8,8],19],
 ['流体传输器','ccd:block/fluid_transmitter',[8,8,8],19],
 ['铜龙头','ccd:block/faucet_closed',[8,8,6],23],
 ['智能龙头','ccd:block/smart_faucet_closed',[8,8,6],23],
 ['流体舱口','ccd:block/fluid_hatch_closed',[8,8,3],24],
 ['智能流体舱口','ccd:block/smart_fluid_hatch_closed',[8,8,3],24],
 ['铜水槽','ccd:block/copper_water_basin',[8,8,8],18],
 ['铜集液栅','ccd:block/copper_collection_grate',[8,4,8],19],
 ['智能集液栅','ccd:block/smart_collection_grate',[8,4,8],19],
 ['流体扩展端口','ccd:block/fluid_access_port',[8,8,8],18],
 ['流体桥接器','ccd:block/fluid_bridge',[8,8,8],18],
 ['智能储罐','ccd:block/smart_fluid_tank',[8,8,8],19],
];
async function main(){
 const out=path.join(root,'outputs/current-materials');fs.mkdirSync(out,{recursive:true});
 const panels=[],labels=[];
 for(let i=0;i<entries.length;i++){
  const [name,id,center,scale]=entries[i],left=i%3*400,top=70+Math.floor(i/3)*370;
  const faces=await mesh(id);
  const image=await render(faces,400,320,[26,20,-34],{center,scale});
  const file=id.split('/').pop()+'.png';await sharp(image).png().toFile(path.join(out,file));
  panels.push({input:image,left,top});
  labels.push(`<text x="${left+200}" y="${top+345}" font-size="18">${name}</text>`);
 }
 const text=Buffer.from(`<svg width="1200" height="1600" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" fill="#303535" text-anchor="middle"><text x="600" y="32" font-size="25">当前资源模型预览 · 贴图返修后</text><text x="600" y="60" font-size="14">直接读取当前模型与贴图 · 不重生成资源 · 不含运行时流体、过滤图标与连接纹理效果</text>${labels.join('')}<text x="600" y="1575" font-size="14">离线渲染，非游戏截图；场景光照与游戏中可能不同</text></g></svg>`);
 await sharp({create:{width:1200,height:1600,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:text,left:0,top:0}]).png().toFile(path.join(out,'preview.png'));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
