"""Export current fluid artwork and a Chinese artist guide without changing game assets."""
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
import json,re,shutil,datetime,sys
from PIL import Image

root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
desktop=Path(r'C:\Users\Administrator\Desktop')
out=Path(sys.argv[1]) if len(sys.argv)>1 else desktop/('CCD全部流体贴图_'+datetime.datetime.now().strftime('%Y%m%d_%H%M%S'))
out.mkdir(exist_ok=True)
lang=json.loads((assets/'lang/zh_cn.json').read_text(encoding='utf-8'))
media=re.findall(r'new Medium\("([^"]+)",0xff([0-9a-f]{6})',(root/'src/main/java/dev/ccd/compat/ProcessingMedia.java').read_text(encoding='utf-8'))
entries=[('quicksand',None,'quicksand_still','quicksand_flow'),('powder_snow',None,'minecraft:powder_snow','minecraft:powder_snow'),('haunting','59c8c5','processing_medium_still','processing_medium_flow'),('slime','82c66a','processing_medium_still','processing_medium_flow')]
entries += [(id,color,'processing_medium_still','processing_medium_flow') for id,color in media]
jar=ZipFile(next((root/'build/moddev/artifacts').glob('*minecraft-resources.jar')))
manifest=[]
def fetch(name):
    if name.startswith('minecraft:'):return jar.read('assets/minecraft/textures/block/'+name.split(':')[1]+'.png'),None
    p=assets/f'textures/block/{name}.png'
    return p.read_bytes(),p.with_suffix('.png.mcmeta')
for id,color,still,flow in entries:
    zh=lang.get('fluid_type.ccd.'+id,id)
    folder=out/'按流体分类'/f'{zh}_{id}';folder.mkdir(parents=True,exist_ok=True)
    for role,name in [('still',still),('flow',flow)]:
        data,meta=fetch(name)
        (folder/f'{id}_{role}.png').write_bytes(data)
        if meta and meta.exists():shutil.copy2(meta,folder/f'{id}_{role}.png.mcmeta')
        # Exact current resources, with namespace preserved, suitable for a resource pack.
        ns='minecraft' if name.startswith('minecraft:') else 'ccd'
        path=name.split(':')[-1]
        raw=out/'原始资源/assets'/ns/'textures/block'/f'{path}.png';raw.parent.mkdir(parents=True,exist_ok=True);raw.write_bytes(data)
        if meta and meta.exists():shutil.copy2(meta,raw.with_suffix('.png.mcmeta'))
        if color:
            image=Image.open(BytesIO(data)).convert('RGBA');rgb=bytes.fromhex(color)
            image.putdata([tuple(round(c*t/255) for c,t in zip(pixel[:3],rgb))+(pixel[3],) for pixel in image.getdata()])
            image.save(folder/f'{id}_{role}_着色参考_不可直接覆盖.png')
    bucket=assets/f'textures/item/{id}_bucket.png'
    if id=='powder_snow':bucketdata=jar.read('assets/minecraft/textures/item/powder_snow_bucket.png');ns='minecraft'
    elif bucket.exists():bucketdata=bucket.read_bytes();ns='ccd'
    else:bucketdata=None
    if bucketdata is not None:
        (folder/f'{id}_bucket.png').write_bytes(bucketdata)
        target=out/'原始资源/assets'/ns/'textures/item'/f'{id}_bucket.png';target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(bucketdata)
    model=assets/f'models/item/{id}_bucket.json'
    if model.exists():
        shutil.copy2(model,folder/f'{id}_bucket模型.json')
        target=out/'原始资源/assets/ccd/models/item'/model.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(model,target)
    manifest.append({'id':'ccd:'+id,'name':zh,'tint':'#FF'+color.upper() if color else '默认白色（不额外染色）','still':('minecraft:block/'+still.split(':')[1] if still.startswith('minecraft:') else 'ccd:block/'+still),'flow':('minecraft:block/'+flow.split(':')[1] if flow.startswith('minecraft:') else 'ccd:block/'+flow)})
(out/'流体贴图对应关系.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
lines=['CCD 流体贴图机制与绘制注意事项',
'导出自当前项目源文件，不是从旧版 JAR 提取。共 '+str(len(entries))+' 种流体。',
'',
'一、文件夹怎么用',
'原始资源：按 assets/命名空间/textures/... 保存游戏当前真正引用的文件，同名共享贴图只保留一份。',
'按流体分类：每种流体都有 still（静止）、flow（流动）副本，静态桶另附 bucket 图。缠魂液和史莱姆是动态桶，没有独立桶 PNG，附桶模型 JSON。副本为编辑模板，不代表目前已配置为独立纹理。',
'文件名带“着色参考_不可直接覆盖”的图，是把当前颜色乘到原始纹理后的参考，不是独立原始美术；游戏光照、渲染器、雾效会进一步影响实际观感。',
'流体贴图对应关系.json：记录所有流体 ID、名称、颜色和实际引用。',
'',
'二、游戏怎样显示这些流体',
'1. 源液和流动液是两个注册对象，通常共用同一个 FluidType 和同一组静止／流动贴图，并不是“源液只用 still、流动液只用 flow”。顶面、侧面及流速由渲染器选择贴图和 UV。',
'2. 缠魂液、史莱姆和全部 ProcessingMedia 介质目前共用 processing_medium_still.png 与 processing_medium_flow.png，以 getTintColor() 乘色。修改这两张原始图会影响全部共用流体。',
'3. 乘色近似为：最终 RGB = 贴图 RGB × 配置 RGB / 255；不是直接替换颜色。白色保留配置颜色，灰色使其变暗，已上色纹理再染色容易偏色。',
'4. 这些共享流体的当前染色 alpha 均为 FF；不要以为把贴图画透明就一定能得到玻璃般半透明世界液体，实际还取决于渲染层、模型和第三方渲染器。',
'5. 流沙使用 quicksand_still/flow 独立纹理；细雪流体的 still/flow 均引用 minecraft:block/powder_snow。细雪主要是容器／管道流体适配，不是新增一套可倾倒液体方块。',
'6. 超热介质（超热熔岩）当前同样使用共享介质动画与自己的颜色，虽然世界行为继承熔岩，但没有引用原版 lava_still/flow。',
'7. 12 种 ProcessingMedia 介质和流沙桶使用静态物品 PNG；改世界流体纹理不会自动改这些桶，改桶也不会自动改世界纹理。缠魂液、史莱姆桶使用 neoforge:fluid_container 动态模型，会读取流体纹理与颜色，没有独立桶 PNG。细雪桶使用原版资源。',
'8. 管道、储罐及雾化粒子通常通过流体扩展读取贴图和颜色；不同容器可能只取 still 或采用不同光照。',
'',
'三、动画布局',
'PNG 可以是竖向帧条，旁边的 .png.mcmeta 决定播放。务必把图和 mcmeta 一起交回。',
'当前共享介质 still 是 16×64（4 帧，每帧 16×16），flow 是 32×128（4 帧，每帧 32×32），frametime=8，即每帧 8 游戏刻，整轮约 1.6 秒。',
'其他贴图尺寸与播放配置以随包原文件为准；没有 mcmeta 的细雪贴图为静态。',
'不要把整张帧条当作单帧缩放；保持每帧宽高一致，顺序从上到下。若改变帧宽高、排列或数量，需要同步修改 mcmeta。',
'',
'四、绘制注意事项',
'保持像素画：整数像素、最近邻缩放，不用抗锯齿、模糊或有损 JPEG；交付原尺寸 PNG，不交放大预览替代原图。',
'still 推荐每帧 16×16，flow 推荐每帧 32×32；流动面会滚动并采样局部区域，避免只在整张图中心放一个必须完整显示的标志。',
'上下左右边缘尽量无缝衔接，每帧之间也要连续，避免突跳、闪烁与整齐棋盘噪点。用连续像素簇表达流体层次。',
'共享纹理建议画中性灰度；若希望某种流体有独立花纹或彩色纹理，可编辑该流体分类目录里的副本，交回后需要为它设置独立资源引用，并评估是否把 tint 改白。仅更改文件名不会生效。',
'不要把着色参考直接当共享贴图覆盖，否则会二次染色。不要用一张共用纹理修改来只调整一种液体。',
'桶图标保持 16×16、透明背景和现有桶轮廓，液面图案与液体色系一致；桶目前不播放动画。',
'原版细雪与桶资源属于 Minecraft 美术，仅作为当前引用参考；用户提供的桶图原样导出。不要把引用资源标记为原创。',
'',
'五、当前对应关系（颜色为代码 ARGB）']
for e in manifest:lines.append(e['name']+' | '+e['id']+' | '+e['tint']+' | still='+e['still']+' | flow='+e['flow'])
lines+=['','六、修改后如何验证','先检查静止顶面、水平流动、下落侧面、储罐、管道、雾化粒子及桶图标，再核对亮暗环境；不只看图片编辑器。','资源包可覆盖“原始资源”里的现有路径；每流体独立纹理和染色调整需要改客户端注册代码 CCDClient.java。导出操作未修改项目资源，也未打包新 JAR。']
(out/'流体贴图机制与绘制注意事项.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8-sig')
jar.close()
assert len(entries)==16
print(out)
print('Fluids:',len(entries),'PNG files:',len(list(out.rglob('*.png'))))
