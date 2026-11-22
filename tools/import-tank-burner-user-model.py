"""Import approved shell verbatim and add native blaze only to the item model."""
from pathlib import Path
from zipfile import ZipFile
import json,shutil,importlib.util
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
downloads=Path(r'C:\Users\Administrator\Downloads')
out=root/'outputs/tank-burner-user-model';backup=out/'backup';backup.mkdir(parents=True,exist_ok=True)
for rel in ['models/block/tank_blaze_burner/block.json','models/item/tank_blaze_burner.json','textures/block/glass_of_tank_blaze_burner.png']:
    p=assets/rel
    if p.exists() and not (backup/rel).exists():
        (backup/rel).parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,backup/rel)
shell=json.loads((downloads/'tank (1).json').read_text(encoding='utf-8'))
shell['render_type']='minecraft:cutout_mipped'
(assets/'models/block/tank_blaze_burner/block.json').write_text(json.dumps(shell,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
shutil.copy2(downloads/'glass_of_tank_blaze_burner.png',assets/'textures/block/glass_of_tank_blaze_burner.png')
jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
with ZipFile(jar) as z:
    blaze=json.loads(z.read('assets/create/models/block/blaze_burner/blaze/idle.json'))
    # Keep native UVs, fit the same upper-space transform used by the runtime renderer.
    for e in blaze['elements']:
        for k in ['from','to']:e[k][1]=5+e[k][1]*11/16
        if 'rotation' in e:e['rotation']['origin'][1]=5+e['rotation']['origin'][1]*11/16
    (assets/'models/block/tank_blaze_burner/item_blaze.json').write_text(json.dumps(blaze,indent=2)+'\n')
    # Include all native blaze geometry (head plus both rod groups), excluding its old cage.
    obj=z.read('assets/create/models/block/blaze_burner/item.obj').decode()
    lines=[];material=None
    for line in obj.splitlines():
        if line.startswith('mtllib '):line='mtllib create:models/block/blaze_burner/item.mtl'
        if line.startswith('v '):
            x,y,zz=map(float,line.split()[1:]);line=f'v {x:.6f} {5/16+y*11/16:.6f} {zz:.6f}'
        if line.startswith('usemtl '):material=line.split()[1]
        if line.startswith('f ') and material!='m_1':continue
        lines.append(line)
    (assets/'models/block/tank_blaze_burner/item_blaze.obj').write_text('\n'.join(lines)+'\n')
    rods=[];group=''
    for line in lines:
        if line.startswith('o '):group=line.split()[1]
        if line.startswith('f ') and group not in ('cube2','cube3'):continue
        rods.append(line)
    (assets/'models/block/tank_blaze_burner/idle_rods.obj').write_text('\n'.join(rods)+'\n')
    (assets/'models/block/tank_blaze_burner/idle_rods.json').write_text(json.dumps({'loader':'neoforge:obj','flip_v':True,'model':'ccd:models/block/tank_blaze_burner/idle_rods.obj','textures':{'0':'create:block/blaze_heater_brazier','1':'create:block/blaze_idle'}},indent=2)+'\n')
    (assets/'models/block/tank_blaze_burner/item_blaze.json').write_text(json.dumps({'parent':'minecraft:block/block','loader':'neoforge:obj','flip_v':True,'model':'ccd:models/block/tank_blaze_burner/item_blaze.obj','textures':{'0':'create:block/blaze_heater_brazier','1':'create:block/blaze_idle'}},indent=2)+'\n')
    old=json.loads((assets/'models/item/tank_blaze_burner.json').read_text())
    item={'parent':'minecraft:block/block','loader':'neoforge:composite','render_type':'minecraft:cutout_mipped',
        'children':{'shell':{'parent':'ccd:block/tank_blaze_burner/block'},'blaze':{'parent':'ccd:block/tank_blaze_burner/item_blaze'}},
        'textures':{'particle':'create:block/fluid_tank'},'display':old.get('display',{})}
    (assets/'models/item/tank_blaze_burner.json').write_text(json.dumps(item,indent=2)+'\n')
    # Preview fixture combines the exact shell and native head; no change to runtime shell.
    preview={**shell,'textures':{**shell['textures'],'preview_blaze':'create:block/blaze_idle'},'elements':json.loads(json.dumps(shell['elements']))}
    for e in blaze['elements']:
        for f in e['faces'].values():f['texture']='#preview_blaze'
        preview['elements'].append(e)
    # The shared renderer accepts a model resolver; provide only this in-memory fixture.
    spec=importlib.util.spec_from_file_location('preview',root/'tools/preview-smart-grate-indicators.py')
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    module.model=lambda name:shell
    from PIL import Image
    from io import BytesIO
    texture=Image.open(BytesIO(z.read('assets/create/textures/block/blaze_idle.png'))).convert('RGBA')
    vertices=[];uvs=[];extra=[]
    for line in lines:
        parts=line.split()
        if not parts:continue
        if parts[0]=='v':vertices.append([float(v)*16 for v in parts[1:4]])
        elif parts[0]=='vt':uvs.append([float(parts[1])*texture.width,(1-float(parts[2]))*texture.height])
        elif parts[0]=='f':
            indices=[tuple(int(v) for v in token.split('/')[:2]) for token in parts[1:]]
            extra.append(([vertices[v-1] for v,t in indices],[uvs[t-1] for v,t in indices],texture))
    module.render('preview',extra).save(out/'preview.png')
original=json.loads((downloads/'tank (1).json').read_text())
assert shell['elements']==original['elements'] and shell['textures']==original['textures']
print('Imported shell without geometry/UV edits, embedded native item blaze, and generated preview.')
