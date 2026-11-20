"""Import user booster geometry and share the same body with its inventory display."""
import json,copy
from pathlib import Path
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
backup=root/'outputs/booster-pump-revision/backup'
for relative in ('models/block/booster_pump.json','models/item/booster_pump.json'):
 source=assets/relative;destination=backup/relative
 if not destination.exists():destination.parent.mkdir(parents=True,exist_ok=True);destination.write_bytes(source.read_bytes())
model=json.loads((Path.home()/'Downloads/booster_pump.json').read_text(encoding='utf-8'))
item=json.loads((assets/'models/item/booster_pump.json').read_text())
shafts=[e for e in item['elements'] if e.get('name','').startswith('inventory_shaft_')]
assert len(shafts)==4
for relative,data in [('models/block/booster_pump.json',model),('models/item/booster_pump.json',{
 **copy.deepcopy(model),'parent':'ccd:block/booster_pump',
 'textures':{**model['textures'],'shaft_side':item['textures']['shaft_side'],'shaft_end':item['textures']['shaft_end']},
 'elements':copy.deepcopy(model['elements'])+shafts})]:
 (assets/relative).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
