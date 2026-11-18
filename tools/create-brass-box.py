"""Build the brass package using pixel-art textures and the copper canister proportions."""
import json
import math
import sys
from io import BytesIO
from zipfile import ZipFile
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path.home() / '.config/opencode/skills/pixel-art/scripts'))
from pxl import Canvas

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/ccd'
OUT = ROOT / 'outputs/brass-box'
TEXTURES = ASSETS / 'textures/item/brass_box'
BRASS = [(114,71,49),(164,107,71),(175,121,79),(199,151,88),(215,170,94),(228,183,99),(247,203,108),(251,220,125)]
# Hand-drawn panel using the actual mechanical_arm / mechanical_arm_side ramp.
# Bright folded lip, broad golden face, connected asymmetric wear and brown folds.
# Each character is one source pixel; this is independent of the copper layout.
PANEL = (
    '1677777661',
    '2566655661',
    '1565556641',
    '1566555441',
    '2566655541',
    '1556655441',
    '1544556541',
    '1445554431',
    '0122222210',
)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    TEXTURES.mkdir(parents=True, exist_ok=True)
    for name in ('side', 'back'):
        canvas = Canvas(16, 16)
        for y in range(16):
            for x in range(16):
                canvas.set(x, y, BRASS[4])
        for y,row in enumerate(PANEL,6):
            for x,index in enumerate(row,3):
                # Back gets a mirrored wear arrangement, retaining the same folded edges.
                canvas.set(x,y,BRASS[int(row[9-(x-3)] if name=='back' else index)])
        canvas.save(str(TEXTURES / f'{name}.png'))
    # The front uses the same metal panel; windows are actual geometry openings.
    elements = []
    def cube(name, lo, hi, side, top):
        x,y,z = lo
        X,Y,Z = hi
        elements.append(dict(name=name, from_=lo, to=hi, faces={
            'north':dict(texture='#'+side, uv=[x,16-Y,X,16-y]),
            'south':dict(texture='#'+side, uv=[x,16-Y,X,16-y]),
            'east':dict(texture='#'+side, uv=[z,16-Y,Z,16-y]),
            'west':dict(texture='#'+side, uv=[z,16-Y,Z,16-y]),
            'up':dict(texture='#'+top, uv=[x,z,X,Z]),
            'down':dict(texture='#'+top, uv=[x,z,X,Z])}))
    cube('brass_body', [3,1,3], [13,10,13], 'side', 'side')
    del elements[0]['faces']['north']
    del elements[0]['faces']['south']
    del elements[0]['faces']['east']
    del elements[0]['faces']['west']
    cube('bottom_rim', [2,0,2], [14,1,14], 'bottom_band', 'bottom_flat')
    cube('lid', [2,10,2], [14,11,14], 'band', 'flat')
    cube('lid_grip', [6,11,6], [10,12,10], 'band', 'cap')
    def plane(name, x, y, width, z, texture, tint=None, face='north'):
        spec = dict(texture='#'+texture, uv=[x+width,y,x,y+1])
        if tint is not None:
            spec['tintindex'] = tint
        elements.append(dict(name=name, from_=[x,15-y,z], to=[x+width,16-y,z], faces={face:spec}))
    for face,tint,z,texture in (('north',0,3,'side'),('south',1,13,'back')):
        for y in range(6,15):
            for x in range(3,13):
                window = 7 <= y <= 13 and 4 <= x <= 11
                depth = z + (.02 if face=='north' else -.02) if window else z
                plane(f'{face}_{x}_{y}', x,y,1,depth,
                      'window' if window else texture, tint if window else None,face)
        glints = ((5,8),(4,9)) if face=='north' else ((10,8),(11,9))
        for x,y in glints:
            plane(f'{face}_glint_{x}_{y}',x,y,1,z+(-.01 if face=='north' else .01),'glass',face=face)
    for face,axis in (('west',3),('east',13)):
        for y in range(6,15):
            for z in range(3,13):
                window = 8 <= y <= 13 and 5 <= z <= 10
                depth = axis + (.04 if face=='west' else -.04) if window else axis
                spec = dict(texture='#backing' if window else '#side', uv=[z,y,z+1,y+1])
                elements.append(dict(name=f'{face}_{z}_{y}',from_=[depth,15-y,z],to=[depth,16-y,z+1],faces={face:spec}))
        # Small, different glass reflections sit in front of each actual item display.
        for z,y in (((6,8),(5,9)) if face=='west' else ((9,8),(10,9))):
            depth = axis + (-.01 if face=='west' else .01)
            elements.append(dict(name=f'{face}_glint_{z}_{y}',from_=[depth,15-y,z],to=[depth,16-y,z+1],
                                 faces={face:dict(texture='#glass',uv=[z,y,z+1,y+1])}))
    # Shared iron closure and glass resources deliberately match the copper canister.
    textures = {key:'ccd:item/fluid_canister/'+key for key in ('band','flat','cap','glass')}
    textures.update({key:'ccd:item/brass_box/'+key for key in ('bottom_band','bottom_flat')})
    textures.update(side='ccd:item/brass_box/side', back='ccd:item/brass_box/back', backing='ccd:item/fluid_canister/fluid', window='minecraft:block/white_concrete', particle='ccd:item/brass_box/side')
    model = dict(parent='create:item/package_transforms', render_type='minecraft:solid', textures=textures,
                 elements=[{('from' if k == 'from_' else k):v for k,v in e.items()} for e in elements],
                 display=json.loads((ASSETS / 'models/item/fluid_package.json').read_text())['display'])
    (ASSETS / 'models/item/composite_package.json').write_text(json.dumps(model, indent=2)+'\n', encoding='utf-8')
    atlas = {}
    for key,value in textures.items():
        if key == 'window':
            atlas[key] = Image.new('RGBA',(16,16),'white')
        else:
            atlas[key] = Image.open(ASSETS / ('textures/'+value.split(':')[1]+'.png')).convert('RGBA')
    jar = next((ROOT / 'build/moddev/artifacts').glob('*minecraft-resources.jar'))
    with ZipFile(jar) as resources:
        item_samples = {face:Image.open(BytesIO(resources.read(f'assets/minecraft/textures/item/{item}.png'))).convert('RGBA')
                        for face,item in (('west','iron_ingot'),('east','gold_ingot'))}
    def render(view):
        length = math.sqrt(sum(v*v for v in view))
        camera = [v/length for v in view]
        rlength = math.hypot(camera[0],camera[2])
        right = [camera[2]/rlength,0,-camera[0]/rlength]
        up = [-camera[1]*camera[0]/rlength,rlength,-camera[1]*camera[2]/rlength]
        def dot(a,b): return sum(x*y for x,y in zip(a,b))
        def project(p):
            p = [p[0]-8,p[1]-6,p[2]-8]
            return (200+dot(p,right)*20,190-dot(p,up)*20)
        tiles = []
        for e in model['elements']:
            x,y,z = e['from']; X,Y,Z = e['to']
            faces = {
                'north':([[X,Y,z],[X,y,z],[x,y,z],[x,Y,z]],[0,0,-1]),
                'south':([[x,Y,Z],[x,y,Z],[X,y,Z],[X,Y,Z]],[0,0,1]),
                'east':([[X,Y,Z],[X,y,Z],[X,y,z],[X,Y,z]],[1,0,0]),
                'west':([[x,Y,z],[x,y,z],[x,y,Z],[x,Y,Z]],[-1,0,0]),
                'up':([[x,Y,z],[x,Y,Z],[X,Y,Z],[X,Y,z]],[0,1,0]),
                'down':([[x,y,Z],[x,y,z],[X,y,z],[X,y,Z]],[0,-1,0])}
            for side,spec in e['faces'].items():
                vertices,normal = faces[side]
                if dot(camera,normal) <= 0: continue
                u,v,U,V = spec['uv']
                cols, rows = max(1,round(abs(U-u))),max(1,round(abs(V-v)))
                for row in range(rows):
                    for col in range(cols):
                        def point(a,b):
                            return [vertices[0][i]+(vertices[3][i]-vertices[0][i])*a+(vertices[1][i]-vertices[0][i])*b for i in range(3)]
                        points = [point(col/cols,row/rows),point(col/cols,(row+1)/rows),point((col+1)/cols,(row+1)/rows),point((col+1)/cols,row/rows)]
                        tx = max(0,min(15,math.floor(u+(U-u)*(col+.5)/cols)))
                        ty = max(0,min(15,math.floor(v+(V-v)*(row+.5)/rows)))
                        color = atlas[spec['texture'][1:]].getpixel((tx,ty))
                        if spec['texture'] == '#backing':
                            sample = item_samples[side].getpixel((min(15,int((tx-5+.5)*16/6)),min(15,int((ty-8+.5)*16/6))))
                            if sample[3]: color = sample
                        if 'tintindex' in spec:
                            color = ((53,111,175,255),(211,118,44,255))[spec['tintindex']]
                        light = {'up':1,'north':.88,'south':.75,'east':.72,'west':.85,'down':.6}[side]
                        color = tuple(round(c*light) for c in color[:3])+(color[3],)
                        tiles.append((sum(dot(p,camera) for p in points)/4,[project(p) for p in points],color))
        image = Image.new('RGBA',(400,380))
        draw = ImageDraw.Draw(image)
        for _,polygon,color in sorted(tiles,key=lambda t:t[0]): draw.polygon(polygon, fill=color)
        return image
    preview = Image.new('RGBA',(1200,490),'#e5e8e2')
    draw = ImageDraw.Draw(preview)
    font = ImageFont.truetype(r'C:\Windows\Fonts\msyh.ttc',20)
    draw.text((600,28),'黄铜盒 · Brass Box',font=font,fill='#303535',anchor='mm')
    for i,(view,label) in enumerate([([24,16,-34],'正面斜视'),([0,8,-40],'正面'),([-24,20,34],'背面斜视')]):
        preview.alpha_composite(render(view),(i*400,48))
        draw.text((i*400+200,443),label,font=font,fill='#303535',anchor='mm')
    draw.text((600,478),'模型离线预览 · 流体颜色与铁锭／金锭为示例内容',font=ImageFont.truetype(r'C:\Windows\Fonts\msyh.ttc',14),fill='#677070',anchor='mm')
    preview.save(OUT / 'preview.png')
    pixels = Image.new('RGBA',(32,16))
    pixels.alpha_composite(Image.open(TEXTURES/'side.png'),(0,0))
    pixels.alpha_composite(Image.open(TEXTURES/'back.png'),(16,0))
    pixels.save(OUT/'textures-source.png')
    pixels.resize((512,256),Image.Resampling.NEAREST).save(OUT/'textures.png')
    (OUT / 'model.meta.json').write_text(json.dumps(dict(name='Brass Box',model='ccd:item/composite_package',
        textureSize=[16,16],reference='Fluid Canister geometry; Create mechanical_arm and mechanical_arm_side brass',bounds=[12,12,12],palette=BRASS,
        artwork='hand-drawn brass panel, independent pixel layout from the copper canister',
        windows='opposite north/south 8x7 panes; actual fluid sprite and tint, stable random content samples',
        itemWindows='opposite east/west 6x6 panes; runtime displays sampled actual item models',
        sharedResources=['fluid_canister/band','fluid_canister/flat','fluid_canister/cap','fluid_canister/glass']),indent=2)+'\n')


if __name__ == '__main__':
    main()
