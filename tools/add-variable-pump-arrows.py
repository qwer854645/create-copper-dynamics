"""Pixel-aligned triangles on three-pixel-wide copper housings, pointing to local -Z."""
import json,importlib.util
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
texture=assets/'textures/block/variable_flow_pump_outlet_arrow.png'
old=Image.open(texture).convert('RGBA')
ink=next(pixel for pixel in old.getdata() if pixel[3])
arrow=Image.new('RGBA',(3,3),(0,0,0,0))
for z,row in enumerate(['.#.','###','###']):
    for x,pixel in enumerate(row):
        if pixel=='#':arrow.putpixel((x,z),ink)
arrow.save(texture)
for relative in ['models/block/variable_flow_pump/body.json','models/item/variable_flow_pump.json']:
    p=assets/relative;d=json.loads(p.read_text())
    d['textures']['outlet_mark']='ccd:block/variable_flow_pump_outlet_arrow'
    d['elements']=[e for e in d['elements'] if not e.get('name','').startswith('outlet_arrow_')]
    for x in [3.5,12.5]:
        d['elements'].append({'name':f'outlet_arrow_{x}_up','from':[x-1.5,14.02,7],'to':[x+1.5,14.02,10],
            'faces':{'up':{'uv':[0,0,16,16],'texture':'#outlet_mark'}}})
    d['render_type']='minecraft:cutout_mipped'
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
out=root/'outputs/variable-pump-arrows';out.mkdir(parents=True,exist_ok=True)
spec=importlib.util.spec_from_file_location('preview',root/'tools/preview-smart-grate-indicators.py')
preview=importlib.util.module_from_spec(spec);spec.loader.exec_module(preview)
preview.render('ccd:item/variable_flow_pump').save(out/'preview.png')
print('Added approved upper-surface triangles to world and item models; preview generated.')
