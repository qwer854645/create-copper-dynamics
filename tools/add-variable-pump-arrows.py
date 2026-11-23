"""Remove retired arrow overlays from both variable pump models and preview the result."""
import json,importlib.util
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
texture=assets/'textures/block/variable_flow_pump_outlet_arrow.png'
for relative in ['models/block/variable_flow_pump/body.json','models/item/variable_flow_pump.json']:
    p=assets/relative;d=json.loads(p.read_text())
    d['textures'].pop('outlet_mark',None)
    d['elements']=[e for e in d['elements'] if not e.get('name','').startswith('outlet_arrow_')]
    d['render_type']='minecraft:cutout_mipped'
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
out=root/'outputs/variable-pump-arrows';out.mkdir(parents=True,exist_ok=True)
spec=importlib.util.spec_from_file_location('preview',root/'tools/preview-smart-grate-indicators.py')
preview=importlib.util.module_from_spec(spec);spec.loader.exec_module(preview)
preview.render('ccd:item/variable_flow_pump').save(out/'preview.png')
print('Removed arrows from world and item models; preview generated.')
