"""Register fluid filter resources using native filter artwork and transforms."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def save(rel,data):
 p=root/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for name,native in [('fluid_filter','filter'),('fluid_attribute_filter','attribute_filter')]:
 save(f'assets/ccd/models/item/{name}.json',{'parent':f'create:item/{native}'})
 save(f'data/ccd/recipe/{name}.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':f'create:{native}'},{'item':'create:fluid_pipe'}],'result':{'id':f'ccd:{name}','count':1}})
translations={
 'item.ccd.fluid_filter':('流体过滤器','Fluid Filter'),
 'item.ccd.fluid_attribute_filter':('流体属性过滤器','Fluid Attribute Filter'),
 'ccd.filter.fluids':('筛选多种流体','Filters multiple fluids'),
 'ccd.filter.attributes':('按流体属性筛选','Filters by fluid attributes'),
 'ccd.filter.whitelist':('允许列表','Allow List'), 'ccd.filter.blacklist':('拒绝列表','Deny List'),
 'ccd.filter.any':('满足任意属性','Match Any'), 'ccd.filter.all':('满足全部属性','Match All'),
 'ccd.filter.exact':('匹配流体组件','Match Components'), 'ccd.filter.type':('仅匹配流体种类','Match Fluid Type'),
 'ccd.filter.choose_sample':('先选择流体样本','Select a fluid sample'),
 'ccd.filter.help':('拖入流体或选择桶；右键移除','Drag fluid or select a bucket; right-click removes'),
 'ccd.filter.attribute.mod':('来源模组：%s','Mod: %s'), 'ccd.filter.attribute.tag':('流体标签：%s','Fluid Tag: %s'),
 'ccd.filter.attribute.temperature':('温度类别：%s','Temperature: %s'), 'ccd.filter.attribute.density':('密度类别：%s','Density: %s'),
 'ccd.filter.attribute.light':('发光类别：%s','Light: %s'), 'ccd.filter.inverted':('已取反；Shift+左键切换','Inverted; Shift-click to toggle'),
 'ccd.filter.attribute_help':('Shift+左键取反此属性','Shift-click to invert this attribute')}
for i,locale in enumerate(('zh_cn','en_us')):
 path=root/f'assets/ccd/lang/{locale}.json';data=json.loads(path.read_text(encoding='utf-8'))
 data.update({key:value[i] for key,value in translations.items()});save(f'assets/ccd/lang/{locale}.json',data)
