"""Split user inventory artwork into matching static and rotating runtime parts."""
import json,sys,shutil
from pathlib import Path
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
model=json.loads(Path(sys.argv[1]).read_text(encoding='utf-8'))
model['textures']['gear']='ccd:block/variable_flow_pump'
def save(relative,data):
    path=assets/relative
    backup=root/'outputs/variable-flow-art-backup'/relative
    if path.exists() and not backup.exists():
        backup.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,backup)
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
save('models/item/variable_flow_pump.json',model)
gear=[e for e in model['elements'] if any(f['texture']=='#gear' for f in e.get('faces',{}).values())]
body=[e for e in model['elements'] if e not in gear and e.get('name')!='signal_gauge_needle']
save('models/block/variable_flow_pump/body.json',{**model,'elements':body})
save('models/block/variable_flow_pump/cog.json',{**model,'elements':gear})
shutil.copy2(Path(sys.argv[2]),assets/'textures/block/variable_flow_pump.png')
print(f'Imported {len(body)} static elements, {len(gear)} rotating elements and inventory needles; texture path updated.')
