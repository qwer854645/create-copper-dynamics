"""Read-only orthographic preview of the registered models and their actual UVs."""
import json
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFont

root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets'
reference=Path(r'C:\Users\Administrator\AppData\Local\Temp\opencode\create-art-reference\assets')
def asset(name,kind):
    ns,name=name.split(':')
    return (assets if ns=='ccd' else reference)/ns/kind/(name+('.json' if kind=='models' else '.png'))
def model(name):
    own=json.loads(asset(name,'models').read_text())
    parent=model(own['parent']) if own.get('parent') and own['parent'] not in ('minecraft:block/block','block/block') else {}
    return {**parent,**own,'textures':{**parent.get('textures',{}),**own.get('textures',{})}}
def render(name):
    m=model(name);faces=[]
    camera=np.array([26,24,38],float);camera/=np.linalg.norm(camera)
    right=np.cross([0,1,0],camera);right/=np.linalg.norm(right);up=np.cross(camera,right)
    for e in m['elements']:
        x,y,z=e['from'];X,Y,Z=e['to']
        verts={'down':[(x,y,Z),(x,y,z),(X,y,z),(X,y,Z)],'up':[(x,Y,z),(x,Y,Z),(X,Y,Z),(X,Y,z)],
               'north':[(X,Y,z),(X,y,z),(x,y,z),(x,Y,z)],'south':[(x,Y,Z),(x,y,Z),(X,y,Z),(X,Y,Z)],
               'west':[(x,Y,z),(x,y,z),(x,y,Z),(x,Y,Z)],'east':[(X,Y,Z),(X,y,Z),(X,y,z),(X,Y,z)]}
        for face,spec in e['faces'].items():
            p=np.array(verts[face],float);n=np.cross(p[1]-p[0],p[2]-p[0]);n/=np.linalg.norm(n)
            if n@camera<=0:continue
            q=np.array([[240+(v-[8,8,8])@right*19,245-(v-[8,8,8])@up*19] for v in p])
            tex=spec['texture']
            while tex.startswith('#'):tex=m['textures'][tex[1:]]
            im=Image.open(asset(tex,'textures')).convert('RGBA')
            u,v,U,V=spec['uv'];uv=np.array([(u,v),(u,V),(U,V),(U,v)],float)
            uv=np.roll(uv,-spec.get('rotation',0)//90,axis=0)*[im.width/16,im.height/16]
            coeff=np.linalg.solve(np.c_[q[:3],np.ones(3)],uv[:3]).T.flatten()
            tile=im.transform((480,490),Image.Transform.AFFINE,coeff,Image.Resampling.NEAREST)
            mask=Image.new('L',tile.size);ImageDraw.Draw(mask).polygon([tuple(v) for v in q],fill=255)
            mask=np.minimum(np.asarray(mask),np.asarray(tile.getchannel('A')))
            tile.putalpha(Image.fromarray(mask));faces.append((p.mean(axis=0)@camera,tile))
    out=Image.new('RGBA',(480,490),'#e5e8e2')
    for _,tile in sorted(faces,key=lambda f:f[0]):out.alpha_composite(tile)
    return out
out=Image.new('RGBA',(960,550),'#e5e8e2')
out.paste(render('ccd:block/smart_collection_grate'),(0,40))
out.paste(render('ccd:block/smart_collection_grate_powered'),(480,40))
font=ImageFont.truetype(r'C:\Windows\Fonts\msyh.ttc',21)
d=ImageDraw.Draw(out);d.text((100,12),'无红石信号 · 两侧暗红',font=font,fill='#303535');d.text((580,12),'收到红石信号 · 两侧亮红',font=font,fill='#303535')
out.save(root/'outputs/smart-grate-indicators/preview.png')
