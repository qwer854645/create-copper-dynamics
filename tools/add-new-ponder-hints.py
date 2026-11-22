"""Keep storyboard English defaults and both locale key sequences synchronized."""
from pathlib import Path
import json,re
root=Path(__file__).resolve().parents[1]
source=(root/'src/main/java/dev/ccd/client/ponder/CCDPonderScenes.java').read_text(encoding='utf-8')
translations={
'tank_burner':('为储罐烈焰人燃烧室供液',[
'下部储罐可储存 4000 mB 单种液体燃料，通过管道或移液臂注入',
'每 1000 mB 按对应桶燃料供燃，沿用原生供热规则',
'移液臂可直接选择燃烧室，无需流体舱口；固体燃料仍可照常使用']),
'atomizer_output_filter':('选择雾化器的目标产物',[
'在雾化器侧面设置目标物品或物品过滤器',
'仅允许可能产出匹配物品的配方；不匹配的输入保持原样',
'允许执行的配方保留全部副产物及原有产出概率',
'过滤留空时允许所有产物；修改过滤会重置进度，不会切换加工类型',
'产物过滤不会限制药水或生物效果']),
'mossification':('使用苔藓悬浊液扩散苔藓',[
'源液和流动的苔藓悬浊液都会苔化相邻的原版合法目标',
'上方有空间时，按原版骨粉规则扩散苔藓并生长植被',
'玻璃等不在苔藓可替换标签中的方块保持不变',
'接触的生物获得苔化效果，脚下和身体触碰到的合法目标都会苔化',
'直接接触和普通风扇给予 4 秒效果，雾化后为 2 秒，持续暴露会刷新']),
'powder_snow_fluid':('以流体形式运输细雪',[
'细雪桶可向流体容器提供 1000 mB 细雪',
'管道与流体设备可以运输储存的细雪',
'安装提供对应加工类型的附属后，雾化器可用细雪进行冰冻加工',
'这不会自动启用其他附属的所有细雪相关加工类型']),
'fluid_arm_setup':('配置流体机械臂',[
'手持流体机械臂，在放置前选择可访问的流体端点',
'右击流体舱口选择输入，再次点击可在输入与输出之间切换',
'放下配置好的机械臂并接入旋转动力，在所选端点间搬运真实流体',
'流体舱口可为其后方容器提供受控访问']),
'fluid_repackaging':('重新组合流体订单',[
'流体订单通过铜包裹运输，物品包裹也可以一同输送',
'流体重打包机把同一订单中的物品与流体组合为黄铜包裹',
'向重打包机运输包裹时，保留物流订单和地址信息',
'重打包只组合包裹内容，不会额外生成流体']),
'fluid_exposure':('加工介质与生物效果',[
'凋零液施加凋零，催泪溶液施加失明与虚弱',
'潮涌能量液给予水下呼吸与急迫，腐化液施加凋零与虚弱',
'直接接触与普通风扇使用普通效果，雾化效果更强且持续更短',
'净化液移除有害效果、灭火，并缓慢恢复生命',
'物品加工需要对应附属；腐化加工还需要铁砧工艺或 L2 敌对'])}
extra={
'Use a filled container or Fluid Filter in the filter slot; lit red indicators mean a Redstone signal is pausing the grate':'在过滤槽设置装液容器或流体过滤器；红色指示条亮起表示红石信号正在暂停集液栅',
'Repeatedly wrench any face to cycle all six pipe directions and four shaft orientations':'用扳手连续点击任意一面，循环六个管口方向与四档传动轴方位'}
paths={loc:root/f'src/main/resources/assets/ccd/lang/{loc}.json' for loc in ['zh_cn','en_us']}
data={loc:json.loads(p.read_text(encoding='utf-8')) for loc,p in paths.items()}
for match in re.finditer(r'public static void \w+\(SceneBuilder builder,SceneBuildingUtil util\)\s*\{(.*?)(?=\n    (?:public|private) static|\Z)',source,re.S):
    body=match.group(1);title=re.search(r'begin\(builder,"([^"]+)","([^"]+)"\)',body)
    if not title:continue
    id,en=title.groups();texts=re.findall(r'text\(s,util,.*?,"([^"]+)"\)',body)
    prefix=f'ccd.ponder.{id}.'
    old={data['en_us'].get(prefix+f'text_{i}'):data['zh_cn'].get(prefix+f'text_{i}') for i in range(1,30)}
    data['en_us'][prefix+'header']=en
    if id in translations:
        zh,lines=translations[id];assert len(lines)==len(texts),(id,texts);data['zh_cn'][prefix+'header']=zh
    for i,text in enumerate(texts,1):
        data['en_us'][prefix+f'text_{i}']=text
        data['zh_cn'][prefix+f'text_{i}']=translations[id][1][i-1] if id in translations else extra.get(text) or old.get(text) or text
for loc,p in paths.items():p.write_text(json.dumps(data[loc],ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Updated storyboard translations and shifted existing subtitles by English identity.')
