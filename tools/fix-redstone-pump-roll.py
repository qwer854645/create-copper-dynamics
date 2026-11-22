from pathlib import Path
import json
r=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd'
variants={}
for facing,rotation in {'north':{},'east':{'y':90},'south':{'y':180},'west':{'y':270},'up':{'x':270},'down':{'x':90}}.items():
    for roll in range(4):
        model='block' if roll==0 else f'block_roll_{roll}'
        if roll:(r/f'models/block/redstone_fluid_pump/{model}.json').write_text(json.dumps({'parent':'ccd:block/redstone_fluid_pump/block','transform':{'rotation':{'z':-90*roll},'origin':'center'}},indent=2)+'\n')
        variants[f'facing={facing},roll={roll}']={'model':'ccd:block/redstone_fluid_pump/'+model,**rotation}
(r/'blockstates/redstone_fluid_pump.json').write_text(json.dumps({'variants':variants},indent=2)+'\n')
for loc,text in [('zh_cn','使用扳手绕_点击面的轴_旋转整个机器。点击管口面可调整齿轮或轴孔方位，点击侧面可改变管口方向。'),('en_us','Use a Wrench to rotate the entire machine around the _clicked face axis_. Click a pipe end to roll the gear or shaft; click a side to turn the ports.')]:
    p=r/f'lang/{loc}.json';d=json.loads(p.read_text(encoding='utf-8'))
    d['block.ccd.redstone_fluid_pump.tooltip.behaviour2']=text
    d['block.ccd.variable_flow_pump.tooltip.condition2']='使用扳手时' if loc=='zh_cn' else 'When using a Wrench'
    d['block.ccd.variable_flow_pump.tooltip.behaviour2']=text
    d['ccd.ponder.pump_variants.text_4']=text.replace('_','')
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
