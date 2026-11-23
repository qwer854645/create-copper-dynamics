"""Add smoking medium localization and an approved-silhouette pixel bucket."""
from pathlib import Path
import json,sys
from PIL import Image
sys.path.insert(0,r'C:\Users\Administrator\.config\opencode\skills\pixel-art\scripts')
from pxl import Canvas
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
c=Canvas.open(str(assets/'textures/item/conduit_energy_fluid_bucket.png'))
for y in range(16):
    for x in range(16):
        r,g,b,a=c.get(x,y)
        if a and g>r*1.3 and b>r*1.3:
            c.set(x,y,'#684832' if g<130 else '#9b7049' if g<175 else '#c69a65')
c.save(str(assets/'textures/item/smoking_fluid_bucket.png'))
for locale,name in [('zh_cn','熏烤液'),('en_us','Smoking Fluid')]:
    path=assets/f'lang/{locale}.json';data=json.loads(path.read_text(encoding='utf-8-sig'))
    for key in ['fluid_type.ccd.smoking_fluid','fluid.ccd.smoking_fluid','fluid.ccd.flowing_smoking_fluid','block.ccd.smoking_fluid']:data[key]=name
    data['item.ccd.smoking_fluid_bucket']=name+'桶' if locale=='zh_cn' else name+' Bucket'
    data['item.ccd.smoking_fluid_bucket.tooltip.summary']='使气流执行原生_烟熏配方_，也可作为_流体雾化器_的加工介质。' if locale=='zh_cn' else 'Enables native _Smoking Recipes_ in air currents and acts as a processing medium for the _Fluid Atomizer_.'
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
out=root/'outputs/smoking-fluid';out.mkdir(parents=True,exist_ok=True)
Image.open(assets/'textures/item/smoking_fluid_bucket.png').resize((256,256),Image.Resampling.NEAREST).save(out/'bucket-preview.png')
