"""Complete bilingual Create-style hints for recently added interactions."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd/lang'
updates={
'zh_cn':{
'block.ccd.fluid_atomizer.tooltip.condition2':'在侧面设置目标产物时',
'block.ccd.fluid_atomizer.tooltip.behaviour2':'只执行可能产出_匹配物品_的配方，保留全部副产物。留空时不限制产物。',
'block.ccd.fluid_atomizer.tooltip.condition3':'更换目标产物过滤条件时',
'block.ccd.fluid_atomizer.tooltip.behaviour3':'重新计算加工进度。过滤_不切换加工类型_，也不限制生物效果。',
'block.ccd.redstone_fluid_pump.tooltip.condition2':'使用扳手调整朝向时',
'block.ccd.redstone_fluid_pump.tooltip.behaviour2':'点击侧面旋转管口，可朝向_六个方向_；点击管口端面翻转朝向。',
'block.ccd.smart_collection_grate.tooltip.condition2':'接收红石信号时',
'block.ccd.smart_collection_grate.tooltip.behaviour2':'红色指示条_亮起_，暂停收集与排液；无信号时指示条变暗。',
'item.ccd.moss_suspension_bucket.tooltip.behaviour2':'给予_苔化_效果。持续期间，生物接触的合法方块变为苔藓，并在上方有空间时触发骨粉扩散。',
'item.ccd.moss_suspension_bucket.tooltip.condition3':'液体接触可苔化的方块时',
'item.ccd.moss_suspension_bucket.tooltip.behaviour3':'源液和流动液均会苔化_相邻目标_，并在上方有空间时扩散苔藓与植被。仅影响原版苔藓可替换目标。',
'item.ccd.moss_suspension_bucket.tooltip.condition4':'通过雾化器施加苔化时',
'item.ccd.moss_suspension_bucket.tooltip.behaviour4':'苔化持续_2 秒_；直接接触与普通风扇为_4 秒_，持续暴露会刷新效果。',
'effect.ccd.mossification.description':'接触可被苔藓替换的方块时，使其苔化并触发原版骨粉扩散。',
},
'en_us':{
'block.ccd.fluid_atomizer.tooltip.condition2':'When a target output is set on a side face',
'block.ccd.fluid_atomizer.tooltip.behaviour2':'Only runs recipes with a possible _matching output_, keeping all byproducts. An empty filter allows all outputs.',
'block.ccd.fluid_atomizer.tooltip.condition3':'When changing the target output filter',
'block.ccd.fluid_atomizer.tooltip.behaviour3':'Restarts processing progress. Filtering _does not switch processing types_ or restrict entity effects.',
'block.ccd.redstone_fluid_pump.tooltip.condition2':'When adjusting the facing with a Wrench',
'block.ccd.redstone_fluid_pump.tooltip.behaviour2':'Click a side to rotate the ports through _all six directions_; click a pipe end to reverse the facing.',
'block.ccd.smart_collection_grate.tooltip.condition2':'When receiving a Redstone signal',
'block.ccd.smart_collection_grate.tooltip.behaviour2':'The red indicators _light up_ and collection and discharge pause. Without a signal, the indicators turn dark.',
'item.ccd.moss_suspension_bucket.tooltip.behaviour2':'Grants _Mossification_. During the effect, valid blocks touched by the entity become moss and trigger bonemeal spread when space is available above.',
'item.ccd.moss_suspension_bucket.tooltip.condition3':'When the liquid touches moss-replaceable blocks',
'item.ccd.moss_suspension_bucket.tooltip.behaviour3':'Source and flowing liquid mossify _adjacent targets_, spreading moss and vegetation when space is available above. Uses vanilla moss-replaceable targets.',
'item.ccd.moss_suspension_bucket.tooltip.condition4':'When Mossification is applied by an Atomizer',
'item.ccd.moss_suspension_bucket.tooltip.behaviour4':'Lasts _2 seconds_, compared with _4 seconds_ for direct contact and ordinary fans. Continued exposure refreshes the effect.',
'effect.ccd.mossification.description':'Mossifies touched moss-replaceable blocks and triggers vanilla bonemeal spread.',
}}
details={
'withering_fluid':('施加_凋零 I_，持续 4 秒。雾化后为_凋零 III_，持续 2 秒。','Applies _Wither I_ for 4 seconds. Atomized: _Wither III_ for 2 seconds.'),
'conduit_energy_fluid':('给予_水下呼吸 I_和_急迫 I_，持续 5 秒。雾化后提升至 III 级，持续 2.5 秒。','Grants _Water Breathing I_ and _Haste I_ for 5 seconds. Atomized: level III for 2.5 seconds.'),
'tear_solution':('施加_失明 I_ 3 秒和_虚弱 I_ 4 秒。雾化后提升至 III 级，持续时间减半。','Applies _Blindness I_ for 3 seconds and _Weakness I_ for 4 seconds. Atomized: level III with half the duration.'),
'corrupting_fluid':('施加_凋零 I_和_虚弱 I_，持续 4 秒。雾化后提升至 III 级，持续 2 秒。','Applies _Wither I_ and _Weakness I_ for 4 seconds. Atomized: level III for 2 seconds.')}
for locale,changes in updates.items():
    p=root/f'{locale}.json';data=json.loads(p.read_text(encoding='utf-8'))
    for id,texts in details.items():changes[f'item.ccd.{id}_bucket.tooltip.behaviour2']=texts[0 if locale=='zh_cn' else 1]
    for id in ['withering_fluid','conduit_energy_fluid','moss_suspension','tear_solution']:
        changes[f'item.ccd.{id}_bucket.tooltip.condition1']='安装更多触媒并用于风扇或雾化器加工时' if locale=='zh_cn' else 'When used for fan or Atomizer processing with More Catalysts installed'
    data.update(changes)
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
assert updates['zh_cn'].keys()==updates['en_us'].keys()
print('Updated bilingual tooltips; added keys match between locales.')
