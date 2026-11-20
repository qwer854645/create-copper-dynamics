"""Align the imported small north outlet with block FACING, which is output."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd'
path=root/'blockstates/fluid_transmitter.json'
states=json.loads(path.read_text())
rotations={'north':{},'south':{'y':180},'east':{'y':90},'west':{'y':270},'up':{'x':90},'down':{'x':270}}
for key,spec in states['variants'].items():
    facing=key.split(',')[0].split('=')[1]
    spec.pop('x',None);spec.pop('y',None);spec.update(rotations[facing])
path.write_text(json.dumps(states,indent=2)+'\n',encoding='utf-8')
