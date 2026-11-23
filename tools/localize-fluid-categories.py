"""Replace individual fluid configuration labels with processing-origin groups."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
groups={
 'seething_reagent':('超级熔岩','Super Lava','默认开启。统一控制超级熔岩、桶、燃料、超热加工与接触效果。','On by default. Controls Super Lava, its bucket, fuel, seething processing and exposure effects.'),
 'powder_snow':('细雪流体支持','Powder Snow Fluid Support','默认开启。控制 CCD 的细雪流体功能，不影响原版细雪桶和方块。','On by default. Controls CCD powder snow fluid support, not vanilla buckets or blocks.'),
 'quicksand':('流沙','Quicksand','默认开启。统一控制流沙、流动流沙、流沙桶及喷砂和接触效果。','On by default. Controls Quicksand, flowing Quicksand, its bucket, sanding and exposure effects.'),
 'base_fluids':('基础加工流体','Base Processing Fluids','史莱姆流体、灵魂液、熏烤液及其桶和效果。','Slime Fluid, Soul Fluid and Smoking Fluid, including their buckets and effects.'),
 'nuclear_fluids':('Create Nuclear 加工流体','Create Nuclear Media','富集介质及其桶、辐射效果；加工需要安装 Create Nuclear。','Enriching Reagent, its bucket and radiation effects. Processing requires Create Nuclear.'),
 'nether_industry_fluids':('Create Nether Industry 加工流体','Create Nether Industry Media','剥魂液及其桶和接触效果；加工需要安装 Create Nether Industry。','Soul Stripping Reagent, its bucket and exposure effects. Processing requires Create Nether Industry.'),
 'more_catalysts_fluids':('Create: More Catalysts 加工流体','Create: More Catalysts Media','爆破浆液、共鸣溶液、幽匿悬液、净化液、凋零液、潮涌能量液、苔藓悬浊液、催泪溶液、腐化液及其桶。加工需要更多触媒；腐化加工另需 AnvilCraft 或 L2 Hostility。','Explosive Slurry, Resonant Solution, Sculk Suspension, Purifying Solution, Withering Fluid, Conduit Energy Fluid, Moss Suspension, Tear Solution and Corrupting Fluid, including buckets. Processing requires More Catalysts; Corrupting also requires AnvilCraft or L2 Hostility.')}
old=['quicksand','slime','haunting','powder_snow','smoking_fluid','seething_reagent','enriching_reagent','soul_stripping_reagent','explosive_slurry','resonant_solution','sculk_suspension','purifying_solution','withering_fluid','conduit_energy_fluid','moss_suspension','tear_solution','corrupting_fluid']
for index,loc in enumerate(['zh_cn','en_us']):
 p=root/f'src/main/resources/assets/ccd/lang/{loc}.json';d=json.loads(p.read_text(encoding='utf-8'))
 for name in old:
  d.pop('ccd.configuration.content.'+name,None);d.pop('ccd.configuration.content.'+name+'.tooltip',None)
 for name,values in groups.items():
  d['ccd.configuration.content.'+name]=values[index]
  d['ccd.configuration.content.'+name+'.tooltip']=values[index+2]
  if name not in ['seething_reagent','powder_snow','quicksand']:
   d['ccd.configuration.content.'+name+'.tooltip']=('默认关闭。' if index==0 else 'Off by default. ')+values[index+2]
 for suffix in ['', '.tooltip']:d.pop('ccd.configuration.content.dndesires_fluids'+suffix,None)
 d['ccd.configuration.contentSection.fluids']=['按来源分类的流体','Fluids by Origin'][index]
 p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
