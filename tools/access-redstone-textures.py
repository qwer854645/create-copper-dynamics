"""Power indicators for existing access models; preserve geometry and non-red pixels."""
import json
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd'
folder=root/'textures/block/fluid_access'
names=('connected_port','connected_bridge','copper_band','bridge_band')
for name in names:
    source=Image.open(folder/(name+'.png')).convert('RGBA')
    for powered in (False,True):
        image=source.copy()
        for y in range(source.height):
            for x in range(source.width):
                r,g,b,a=source.getpixel((x,y))
                # Red indicator pigments are distinct from warm copper and iron.
                if a and r>2*g and r>2*b and g<45:
                    if powered:
                        shade=max(0,min(1,(r-44)/44))
                        image.putpixel((x,y),(round(130+100*shade),round(4+12*shade),round(3+8*shade),a))
        image.save(folder/(name+('_powered' if powered else '_unpowered')+'.png'))
for name in ('fluid_access_port','fluid_bridge'):
    for suffix in ('','_casing','_socket'):
        path=root/f'models/block/{name}{suffix}.json'
        model=json.loads(path.read_text(encoding='utf-8'))
        for key,value in model.get('textures',{}).items():
            for texture in names:
                if value=='ccd:block/fluid_access/'+texture:
                    model['textures'][key]=value+'_unpowered'
        path.write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
        powered={'parent':f'ccd:block/{name}{suffix}','textures':{
            key:value.replace('_unpowered','_powered') for key,value in model['textures'].items()
            if value.endswith('_unpowered')}}
        (root/f'models/block/{name}{suffix}_powered.json').write_text(json.dumps(powered,indent=2)+'\n',encoding='utf-8')
    path=root/f'blockstates/{name}.json';states=json.loads(path.read_text(encoding='utf-8'))
    parts=[]
    for part in states['multipart']:
        for powered in (False,True):
            parts.append({'when':{**part.get('when',{}),'powered':powered},
                          'apply':{**part['apply'],'model':part['apply']['model']+('_powered' if powered else '')}})
    path.write_text(json.dumps({'multipart':parts},indent=2)+'\n',encoding='utf-8')
