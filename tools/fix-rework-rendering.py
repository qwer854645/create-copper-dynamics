"""Fix imported layers, state orientation and inventory/live model consistency."""
import json
from pathlib import Path
from zipfile import ZipFile
root=Path(__file__).resolve().parents[1]
a=root/'src/main/resources/assets/ccd'
def read(p): return json.loads(p.read_text(encoding='utf-8'))
def save(p,d): p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
with ZipFile(jar) as archive:
    # Preserve native geometry but expose alpha-tested frame layers on CCD's
    # own model IDs rather than inheriting the default solid model layer.
    states=read(a/'blockstates/fluid_repackager.json')
    for value in states['variants'].values():
        original=value['model'];name=original.rsplit('/',1)[1]
        save(a/f'models/block/fluid_repackager/{name}.json',{'parent':original,'render_type':'minecraft:cutout_mipped','ambientocclusion':False})
        value['model']='ccd:block/fluid_repackager/'+name
    save(a/'blockstates/fluid_repackager.json',states)
    save(a/'models/item/fluid_repackager.json',{'parent':'create:block/repackager/item','render_type':'minecraft:cutout_mipped'})
    # Dial faces must be ahead of the opaque center, not embedded behind it.
    for state in ('open','closed'):
        path=a/f'models/block/redstone_fluid_valve/dial_{state}.json';model=read(path)
        for element in model['elements']:
            y=14.01 if 'up' in element['faces'] else 1.99
            element['from'][1]=element['to'][1]=y
        model['textures']['glass']='create:block/fluid_valve'
        for side,y in (('up',14.12),('down',1.88)):
            model['elements'].append({'name':'glass_'+side,'from':[2,y,2],'to':[14,y,14],
                'faces':{side:{'texture':'#glass','uv':[1,1,7,7]}}})
        save(path,model)
    for name in ('claw_base','claw_base_goggles'):
        path=a/f'models/block/fluid_arm/{name}.json';model=read(path);model['render_type']='minecraft:cutout';save(path,model)
    # Filter panel belongs on the outside of the newly thickened grate wall.
    path=a/'models/block/smart_collection_grate_filter.json';model=read(path)
    for element in model['elements']:
        element['from'][2]+=1.5;element['to'][2]+=1.5
    save(path,model)
    path=a/'models/item/smart_collection_grate.json';model=read(path)
    model['elements']=[e for e in model['elements'] if e.get('name')!='front_filter_panel']
    model['elements']+=read(a/'models/block/smart_collection_grate_filter.json')['elements'];save(path,model)
    # Rotate the imported transmitter mesh once, so every state and item agrees.
    path=a/'models/block/fluid_transmitter.json';model=read(path)
    opposite={'north':'south','south':'north','east':'west','west':'east','up':'up','down':'down'}
    for element in model['elements']:
        lo,hi=element['from'],element['to']
        element['from']=[16-hi[0],lo[1],16-hi[2]];element['to']=[16-lo[0],hi[1],16-lo[2]]
        if 'rotation' in element:
            rotation=element['rotation'];p=rotation['origin'];rotation['origin']=[16-p[0],p[1],16-p[2]]
            if rotation['axis']!='y': rotation['angle']=-rotation['angle']
        element['faces']={opposite[side]:face for side,face in element['faces'].items()}
    save(path,model)
