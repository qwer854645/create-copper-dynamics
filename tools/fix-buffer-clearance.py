"""Update only the copper filler, leaving the tank, wheel and user artwork intact."""
import json, copy
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd/models'
for relative in ['block/buffer_pump.json','block/buffer_pump/pump_head.json','item/buffer_pump.json']:
    path=root/relative
    model=json.loads(path.read_text(encoding='utf-8'))
    elements=model['elements']
    housing=next(e for e in elements if e.get('name')=='copper_middle_housing')
    housing['from']=[2,10.6,2]
    housing['to']=[14,13,14]
    elements[:]=[e for e in elements if e.get('name') not in ('copper_lower_collar','copper_center_hub')]
    lower=copy.deepcopy(housing)
    lower.update(name='copper_lower_collar',**{'from':[2,5,2],'to':[14,5.4,14]})
    hub=copy.deepcopy(housing)
    hub.update(name='copper_center_hub',**{'from':[6.75,5.4,6.75],'to':[9.25,10.6,9.25]})
    elements[0:0]=[lower,hub]
    path.write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
