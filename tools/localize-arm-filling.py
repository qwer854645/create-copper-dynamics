from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
texts={
'ccd.fluid_arm.filter_mode':('移液臂模式','Fluid Arm Mode'),
'ccd.fluid_arm.transfer_mode':('搬运流体','Transfer Fluid'),
'ccd.fluid_arm.filling_mode':('物品注液','Fill Items'),
'block.ccd.fluid_arm.tooltip.control3':('长按基座过滤槽时','When Holding R-Click on an East/West Base Filter Slot'),
'block.ccd.fluid_arm.tooltip.action3':('选择_搬运流体_或_物品注液_。短按在同一槽位设置过滤。','Selects _Transfer Fluid_ or _Fill Items_. Short-click the same slot to set its filter.'),
'block.ccd.fluid_arm.tooltip.condition3':('选择物品注液模式时','When Fill Items Is Selected'),
'block.ccd.fluid_arm.tooltip.behaviour3':('将置物台或传送带选为_输出点_，执行注液配方。槽位筛选_产物_；搬液模式筛选流体。','Selects Depots or Belts as _outputs_ for filling recipes. The slot filters _outputs_; in transfer mode it filters fluids.'),
}
for index,loc in enumerate(['zh_cn','en_us']):
 p=root/f'src/main/resources/assets/ccd/lang/{loc}.json';d=json.loads(p.read_text(encoding='utf-8'))
 for k,v in texts.items():d[k]=v[index]
 p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
