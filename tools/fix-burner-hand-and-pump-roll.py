from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd'
p=root/'models/item/tank_blaze_burner.json';d=json.loads(p.read_text())
for view in ['firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand']:
    d['display'][view]={'rotation':[0,45,0],'translation':[0,2,0],'scale':[.4,.4,.4]}
p.write_text(json.dumps(d,indent=2)+'\n')
variants={}
for facing,rotation in {'north':{},'east':{'y':90},'south':{'y':180},'west':{'y':270},'up':{'x':270},'down':{'x':90}}.items():
    for roll in range(4):
        name='body' if roll==0 else f'body_roll_{roll}'
        if roll:
            model={'parent':'ccd:block/variable_flow_pump/body','transform':{'rotation':{'z':-90*roll},'origin':'center'}}
            (root/f'models/block/variable_flow_pump/{name}.json').write_text(json.dumps(model,indent=2)+'\n')
        variants[f'facing={facing},roll={roll}']={'model':'ccd:block/variable_flow_pump/'+name,**rotation}
(root/'blockstates/variable_flow_pump.json').write_text(json.dumps({'variants':variants},indent=2)+'\n')
