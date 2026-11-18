"""Copper parcel shell, keeping the existing dynamic fluid-window template."""
import json,sys
from pathlib import Path
sys.path.insert(0,str(Path.home()/'.config/opencode/skills/pixel-art/scripts'))
from pxl import Canvas
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/ccd'
folder=A/'textures/item/copper_parcel'; folder.mkdir(parents=True,exist_ok=True)
palette=['#793b2b','#904931','#a75a40','#b26247','#c26b4c','#c87456','#d67b5b','#e3826c']
rows=['166666666661','654444554453','654334554443','644334444553','645444433553','655554433443',
      '654444445543','644334455543','645334444443','655444433453','644444444443','133333333331']
for name in ['side','top','bottom']:
    c=Canvas(16,16,fill=palette[3])
    for y,row in enumerate(rows,2):
        for x,index in enumerate(row,2): c.set(x,y,palette[int(index)])
    if name=='side':
        # A flush dark copper sealing strip with a thin folded edge.
        c.rect(7,2,2,12,palette[1]); c.line_v(7,2,13,palette[5])
    else:
        c.line_v(7,2,13,palette[0]); c.line_v(8,2,13,palette[5])
        for i in range(5):
            c.set(3+i,3+i,palette[2]); c.set(12-i,3+i,palette[2])
            c.set(3+i,12-i,palette[2]); c.set(12-i,12-i,palette[2])
    c.save(str(folder/(name+'.png')))
path=A/'models/item/fluid_package.json'; model=json.loads(path.read_text())
windows=[e for e in model['elements'] if e['name'].startswith(('window_back_','fluid_','glass_glint_'))]
body={'name':'copper_parcel_body','from':[2,0,2],'to':[14,12,14],'faces':{}}
for face in ['south','east','west']:
    body['faces'][face]={'texture':'#parcel_side','uv':[2,4,14,16]}
body['faces']['up']={'texture':'#parcel_top','uv':[2,2,14,14]}
body['faces']['down']={'texture':'#parcel_bottom','uv':[2,2,14,14]}
elements=[body]
# Preserve exact dynamic-fluid height and template naming, move its face to new shell.
for e in windows:
    z=2.04 if e['name'].startswith('window_back_') else 2.02 if e['name'].startswith('fluid_') else 1.99
    e['from'][2]=z; e['to'][2]=z; elements.append(e)
for y in range(4,16):
    for x in range(2,14):
        if 4<=x<12 and 7<=y<14: continue
        elements.append({'name':f'parcel_front_{x}_{y}','from':[x,15-y,2],'to':[x+1,16-y,2],
            'faces':{'north':{'texture':'#parcel_side','uv':[x+1,y,x,y+1]}}})
for name in ['side','top','bottom']: model['textures']['parcel_'+name]='ccd:item/copper_parcel/'+name
model['textures']['particle']=model['textures']['parcel_side']
model['elements']=elements
path.write_text(json.dumps(model,indent=2)+'\n')
