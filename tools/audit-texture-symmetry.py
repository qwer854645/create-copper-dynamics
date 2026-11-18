"""Read-only RGBA reflection audit; output reports without changing game assets."""
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'src/main/resources/assets/ccd/textures'
OUT=ROOT/'outputs/texture-symmetry'

def difference(a,b):
    # Ignore RGB hidden below full transparency, but retain alpha differences.
    a=a.copy(); b=b.copy()
    a[a[:,:,3]==0,:3]=0; b[b[:,:,3]==0,:3]=0
    visible=(a[:,:,3]>0)|(b[:,:,3]>0)
    mismatch=np.any(a!=b,axis=2)&visible
    return int(mismatch.sum()),int(visible.sum()),int(np.count_nonzero(a[:,:,3]!=b[:,:,3]))

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    entries=[]
    for path in sorted(BASE.rglob('*.png')):
        image=Image.open(path).convert('RGBA')
        metadata=path.with_suffix('.png.mcmeta')
        animation=json.loads(metadata.read_text(encoding='utf-8')).get('animation') if metadata.exists() else None
        width,height=image.size
        fw=animation.get('width',width) if animation is not None else width
        fh=animation.get('height',min(width,height)) if animation is not None else height
        frames=[image.crop((x,y,x+fw,y+fh)) for y in range(0,height,fh) for x in range(0,width,fw)]
        totals=np.zeros((2,3),dtype=np.int64)
        for frame in frames:
            pixels=np.array(frame)
            for axis,other in enumerate([pixels[:,::-1],pixels[::-1,:]]): totals[axis]+=difference(pixels,other)
        entries.append(dict(path=path.relative_to(BASE).as_posix(),width=width,height=height,frames=len(frames),
            kind='动画逐帧' if animation is not None else '连接图集（整图）' if '_connected' in path.stem else '静态整图',
            horizontal=int(totals[0,0]),vertical=int(totals[1,0]),
            horizontalPercent=round(100*totals[0,0]/max(1,totals[0,1]),1),verticalPercent=round(100*totals[1,0]/max(1,totals[1,1]),1),
            alphaHorizontal=int(totals[0,2]),alphaVertical=int(totals[1,2])))
    (OUT/'results.json').write_text(json.dumps(entries,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    asymmetric=[e for e in entries if e['horizontal'] or e['vertical']]
    font=ImageFont.truetype(r'C:\Windows\Fonts\msyh.ttc',13)
    # A labelled contact sheet: each row has source pixels and both reflection mismatch masks.
    for category,subset in [('all',asymmetric),('outline',[e for e in entries if e['alphaHorizontal'] or e['alphaVertical']])]:
        if not subset: continue
        cols=4; cellw,cellh=310,150
        sheet=Image.new('RGB',(cols*cellw,((len(subset)+cols-1)//cols)*cellh),'#e5e8e2'); draw=ImageDraw.Draw(sheet)
        for i,e in enumerate(subset):
            x=i%cols*cellw; y=i//cols*cellh
            image=Image.open(BASE/e['path']).convert('RGBA')
            if e['frames']>1: image=image.crop((0,0,image.width,min(image.width,image.height)))
            pixels=np.array(image); normalized=pixels.copy(); normalized[normalized[:,:,3]==0,:3]=0
            thumb=image.copy(); thumb.thumbnail((80,80),Image.Resampling.NEAREST)
            # Enlarge small source textures, never smooth pixels.
            thumb=image.resize((80,80),Image.Resampling.NEAREST)
            sheet.paste(thumb,(x+8,y+8),thumb)
            for axis,other in enumerate([normalized[:,::-1],normalized[::-1,:]]):
                mask=np.any(normalized!=other,axis=2)
                heat=np.zeros_like(pixels); heat[:]=[57,64,59,255]; heat[mask]=[208,115,75,255]
                sheet.paste(Image.fromarray(heat).resize((80,80),Image.Resampling.NEAREST),(x+104+axis*96,y+8))
            draw.text((x+8,y+94),e['path'],font=font,fill='#303535')
            draw.text((x+8,y+115),f"左右 {e['horizontalPercent']}% / 上下 {e['verticalPercent']}%",font=font,fill='#303535')
        sheet.save(OUT/f'{category}.png')
    counts=dict(total=len(entries),asymmetric=len(asymmetric),bothSymmetric=len(entries)-len(asymmetric),
        outlineAsymmetric=sum(bool(e['alphaHorizontal'] or e['alphaVertical']) for e in entries))
    print(json.dumps(counts,ensure_ascii=False))
    lines=['# CCD 贴图对称性检查','',f"共 {len(entries)} 张 PNG，{len(asymmetric)} 张至少一个方向不对称。",'',
           '左右／上下表示整张图沿中心轴镜像后有可见像素差异；动画按帧。此处是严格像素检查，不是美术缺陷判断。',
           '透明轮廓差异也可能来自玻璃高光、不同透明度、图集分区和非对称物品轮廓。路径相对于 `src/main/resources/assets/ccd/textures/`。','',
           '## 不对称文件','']
    for e in asymmetric:
        axes='、'.join(axis for axis,key in [('左右','horizontal'),('上下','vertical')] if e[key])
        lines.append(f"- `{e['path']}`：{axes}；左右差异 {e['horizontalPercent']}%，上下差异 {e['verticalPercent']}%；{e['kind']}。")
    lines+=['','## 两个方向均完全对称','']
    lines += [f"- `{e['path']}`" for e in entries if not e['horizontal'] and not e['vertical']]
    lines+=['','## 检查边界','',
            '- 仅检查 CCD 自带 PNG，包含可能已不再引用的历史资源。',
            '- 未检查 Create／Minecraft 被直接引用的原图，也未按模型 UV 裁剪实际可见面。',
            '- 连接纹理与机械部件图集整图不对称通常属于布局设计，不代表方块成品轮廓不对称。',
            '- 未修改任何游戏贴图。']
    (OUT/'report.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    for e in entries:
        if e['alphaHorizontal'] or e['alphaVertical']: print('ALPHA',e['path'],e['alphaHorizontal'],e['alphaVertical'])
    # Standalone, filterable report. Data embedded; no runtime filesystem/network access.
    canvas=Path.home()/'.cursor/projects/f-others/canvases/CCD-texture-symmetry.canvas.tsx'
    source="""import { useHostTheme, useCanvasState } from 'cursor/canvas';
const data = DATA;
export default function Audit() {
 const t=useHostTheme(); const [query,setQuery]=useCanvasState('query',''); const [mode,setMode]=useCanvasState('mode','asymmetric');
 const rows=data.filter(e=>e.path.includes(query)&&(mode==='all'||mode==='outline'?(mode!=='outline'||e.alphaHorizontal>0||e.alphaVertical>0):e.horizontal>0||e.vertical>0));
 return <main style={{padding:24,fontFamily:'system-ui',color:t.text.primary,background:t.bg.editor,lineHeight:1.6}}>
 <h1 style={{fontSize:24}}>CCD 贴图镜像对称性检查</h1>
 <p>检查 COUNT 张项目自带 PNG。ASYM 张在至少一个方向存在像素差异；ALPHA 张在透明轮廓或透明度上存在差异。</p>
 <p style={{color:t.text.secondary}}>比较左右镜像与上下镜像；动画按帧统计。百分比为镜像位置 RGBA 不同的可见像素占比，非瑕疵评分。完全透明区域的隐藏 RGB 已忽略。连接图集按整图比较，不代表单个模型面。没有修改任何游戏贴图。</p>
 <div style={{display:'flex',gap:12,margin:'20px 0'}}><input aria-label="按路径筛选" placeholder="按设备或文件名筛选" value={query} onChange={e=>setQuery(e.target.value)} style={{padding:8,color:t.text.primary,background:t.bg.elevated,border:`1px solid ${t.stroke.primary}`}}/>
 <select aria-label="结果类别" value={mode} onChange={e=>setMode(e.target.value)} style={{color:t.text.primary,background:t.bg.elevated}}><option value="asymmetric">所有不对称</option><option value="outline">轮廓／透明度不对称</option><option value="all">全部贴图</option></select></div>
 <p>当前 {rows.length} 张。路径相对于 src/main/resources/assets/ccd/textures/。</p>
 <table style={{width:'100%',borderCollapse:'collapse',fontSize:13}}><thead><tr>{['贴图','尺寸','检查单位','左右差异','上下差异','透明度差异像素 左右／上下'].map(s=><th key={s} style={{textAlign:'left',padding:8,borderBottom:`1px solid ${t.stroke.primary}`}}>{s}</th>)}</tr></thead><tbody>{rows.map(e=><tr key={e.path}><td style={{padding:8}}>{e.path}</td><td>{e.width}×{e.height}</td><td>{e.kind}{e.frames>1?` · ${e.frames}帧`:''}</td><td>{e.horizontalPercent}%</td><td>{e.verticalPercent}%</td><td>{e.alphaHorizontal} / {e.alphaVertical}</td></tr>)}</tbody></table>
 <p style={{color:t.text.secondary}}>边界：只扫描 CCD 资源目录，包含可能未使用的历史文件；没有判断模型 UV 的可见区域，也没有检查直接引用的 Create／Minecraft 原图。材质像素簇、光照、玻璃反光、箭头和状态标记通常故意不对称，不能据此自动修复。</p>
 </main>;
}
""".replace('DATA',json.dumps(entries,ensure_ascii=False)).replace('COUNT',str(counts['total'])).replace('ASYM',str(counts['asymmetric'])).replace('ALPHA',str(counts['outlineAsymmetric']))
    canvas.write_text(source,encoding='utf-8')

if __name__=='__main__': main()
