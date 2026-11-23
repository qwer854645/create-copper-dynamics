from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
values={
'ccd.smart_faucet.filter_mode':('筛选模式','Filter Mode'),
'ccd.smart_faucet.filter_fluid':('流体过滤','Fluid Filter'),
'ccd.smart_faucet.filter_output':('产物过滤','Output Filter'),
'block.ccd.smart_faucet.tooltip.control1':('长按正面过滤槽时','When Holding R-Click on the Front Filter Slot'),
'block.ccd.smart_faucet.tooltip.action1':('在同一槽位选择_流体过滤_或_产物过滤_。短按设置过滤物品。','Selects _Fluid Filter_ or _Output Filter_ in the same slot. Short-click to set the filter.'),
'block.ccd.smart_faucet.tooltip.condition2':('选择产物过滤时','When Output Filtering Is Selected'),
'block.ccd.smart_faucet.tooltip.behaviour2':('只执行可能产出_匹配物品_的注液配方；不直接向容器倒液。留空允许所有注液产物。','Allows filling recipes with a possible _matching output_. Direct container pouring is disabled. An empty filter allows all filling outputs.'),
}
for index,locale in enumerate(['zh_cn','en_us']):
 p=root/f'src/main/resources/assets/ccd/lang/{locale}.json';d=json.loads(p.read_text(encoding='utf-8'))
 for key,text in values.items():d[key]=text[index]
 p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
