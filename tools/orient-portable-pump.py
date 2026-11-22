"""Rotate the portable pump's native-local body together with its animated parts."""
import json
from pathlib import Path
r=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd'
variants={}
for facing,rotation in {'north':{},'east':{'y':90},'south':{'y':180},'west':{'y':270},'up':{'x':270},'down':{'x':90}}.items():
    for roll in range(4):
        name='portable_fluid_pump' if roll==0 else f'portable_fluid_pump_roll_{roll}'
        if roll:(r/f'models/block/{name}.json').write_text(json.dumps({'parent':'ccd:block/portable_fluid_pump','transform':{'rotation':{'z':-90*roll},'origin':'center'}},indent=2)+'\n')
        variants[f'facing={facing},roll={roll}']={'model':'ccd:block/'+name,**rotation}
(r/'blockstates/portable_fluid_pump.json').write_text(json.dumps({'variants':variants},indent=2)+'\n')
