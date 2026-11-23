"""Three-by-two triangles at the pipe mouths, at one texel per model pixel."""
import json,importlib.util
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
texture=assets/'textures/block/variable_flow_pump_outlet_arrow.png'
ink=Image.open(texture).convert('RGBA')
color=next(pixel for pixel in ink.getdata() if pixel[3])
arrow=Image.new('RGBA',(3,2),(0,0,0,0))
for x,z in [(1,0),(0,1),(1,1),(2,1)]:arrow.putpixel((x,z),color)
arrow.save(texture)
for relative in ['models/block/variable_flow_pump/body.json','models/item/variable_flow_pump.json']:
    p=assets/relative;d=json.loads(p.read_text())
    d['textures']['outlet_mark']='ccd:block/variable_flow_pump_outlet_arrow'
    d['elements']=[e for e in d['elements'] if not e.get('name','').startswith('outlet_arrow_')]
    for end,z in [('north',1),('south',13)]:
        d['elements'].append({'name':f'outlet_arrow_{end}_up','from':[6,12.12,z],'to':[9,12.12,z+2],
            'faces':{'up':{'uv':[0,0,16,16],'texture':'#outlet_mark'}}})
    d['render_type']='minecraft:cutout_mipped'
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
out=root/'outputs/variable-pump-arrows';out.mkdir(parents=True,exist_ok=True)
spec=importlib.util.spec_from_file_location('preview',root/'tools/preview-smart-grate-indicators.py')
preview=importlib.util.module_from_spec(spec);spec.loader.exec_module(preview)
preview.render('ccd:item/variable_flow_pump').save(out/'preview.png')
print('Applied 3x2 triangles beside both pipe mouths at 1:1 pixel scale; world/item synchronized.')
