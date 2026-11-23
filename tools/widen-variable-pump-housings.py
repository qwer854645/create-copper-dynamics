"""Preview/user revision: extend only the outer copper housing faces to block edges."""
from pathlib import Path
import json,copy,importlib.util
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
before=None
for relative in ['models/block/variable_flow_pump/body.json','models/item/variable_flow_pump.json']:
    path=assets/relative
    data=json.loads(path.read_text())
    if relative.startswith('models/item'):before=copy.deepcopy(data)
    for element in data['elements']:
        if element.get('name')=='left_copper_housing':
            element['from'][0]=0
            for side in ['north','south','up','down']:
                uv=element['faces'][side]['uv']
                if uv[0]>uv[2]:uv[0]=16
                else:uv[2]=16
        if element.get('name')=='right_copper_housing':
            element['to'][0]=16
            for side in ['north','south','up','down']:
                uv=element['faces'][side]['uv']
                if uv[0]>uv[2]:uv[2]=0
                else:uv[0]=0
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
out=root/'outputs/variable-pump-wide-housing';out.mkdir(parents=True,exist_ok=True)
spec=importlib.util.spec_from_file_location('preview',root/'tools/preview-smart-grate-indicators.py')
p=importlib.util.module_from_spec(spec);spec.loader.exec_module(p)
lookup=p.model
p.model=lambda name:before if name=='before' else lookup(name)
image=Image.new('RGBA',(960,520),'#e5e8e2')
image.paste(p.render('before'),(0,30));image.paste(p.render('ccd:item/variable_flow_pump'),(480,30))
d=ImageDraw.Draw(image);d.text((20,10),'Before',fill='#303535');d.text((500,10),'Housing extended to X=0 / X=16',fill='#303535')
image.save(out/'comparison.png')
