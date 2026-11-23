"""Compare actual CCD vertical shell before/after, using native model geometry and UVs."""
from pathlib import Path
import importlib.util, copy
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('preview',root/'tools/preview-smart-grate-indicators.py')
p=importlib.util.module_from_spec(spec);spec.loader.exec_module(p)
original=p.model
shell=original('ccd:block/fluid_packager_vertical')
before=copy.deepcopy(shell)
for element in before['elements']:
    # A 180 degree X blockstate transform, preserving inverted cuboid bounds.
    for key in ('from','to'):
        element[key][1]=16-element[key][1]
        element[key][2]=16-element[key][2]
    element.pop('rotation',None)
    element['faces']={ {'north':'south','south':'north','up':'down','down':'up'}.get(side,side):
        {**face,'rotation':(face.get('rotation',0)+180)%360} for side,face in element['faces'].items() }
p.model=lambda name:before if name=='before' else shell
out=root/'outputs/packager-vertical';out.mkdir(parents=True,exist_ok=True)
image=Image.new('RGBA',(960,520),'#e5e8e2')
image.paste(p.render('before'),(0,30));image.paste(p.render('after'),(480,30))
d=ImageDraw.Draw(image);d.text((20,10),'Before: extra X180',(30,35,35));d.text((500,10),'After: native upright shell',(30,35,35))
image.save(out/'comparison.png')
