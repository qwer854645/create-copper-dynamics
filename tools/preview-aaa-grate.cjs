// Read-only render of the smart grate after applying the user's aaa.zip assets.
const fs=require('node:fs'),path=require('node:path'),sharp=require('sharp');
const {mesh,render}=require('./preview-fluid-packager.cjs');
async function main(){
 const out=path.resolve(__dirname,'../outputs/aaa-grate');fs.mkdirSync(out,{recursive:true});
 const faces=[...await mesh('ccd:block/smart_collection_grate'),...await mesh('ccd:block/smart_collection_grate_filter')];
 const panels=[];
 for(const [i,view]of [[0,[26,24,38]],[1,[0,40,1]],[2,[-26,18,-38]]])
  panels.push({input:await render(faces,440,440,view,{center:[8,8,8],scale:18}),left:i*440,top:50});
 const label=Buffer.from('<svg width="1320" height="550" xmlns="http://www.w3.org/2000/svg"><g font-family="Microsoft YaHei,sans-serif" text-anchor="middle" fill="#303535"><text x="660" y="32" font-size="24">智能集液栅 · aaa.zip 资源覆盖预览</text><text x="220" y="520" font-size="18">正面斜视</text><text x="660" y="520" font-size="18">顶视</text><text x="1100" y="520" font-size="18">背面斜视</text><text x="660" y="545" font-size="13">当前模型与贴图 · 不含运行时流体及过滤图标</text></g></svg>');
 await sharp({create:{width:1320,height:550,channels:4,background:'#e5e8e2'}}).composite([...panels,{input:label,left:0,top:0}]).png().toFile(path.join(out,'preview.png'));
} main().catch(e=>{console.error(e);process.exitCode=1;});
