"""Pixel-art access devices: CCD panels, stock-link inspired sockets, native Create pipe mouths."""
import json
import math
import sys
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0,str(Path.home()/'.config/opencode/skills/pixel-art/scripts'))
from pxl import Canvas

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/ccd'
OUT=ROOT/'outputs/fluid-access'
JAR=next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
COPPER=['#703a29','#904931','#a75a40','#b26247','#c26b4c','#c87456','#d67b5b','#e3826c']
BRASS=['#724731','#a46b47','#af794f','#c79758','#d7aa5e','#e4b763','#f7cb6c','#fbdc7d']
IRON=['#353438','#4f4f4f','#686d65','#818783','#b4b5a7']
# Original panel clusters. Edge folds and small broad highlights, no tiled noise.
PANEL=[
 '0122222222222210','1677666666655661','1655544556655541','1655444555665541',
 '1554445555665441','1544455554455441','1544556654455541','1555566555445541',
 '1555565544455541','1544555444556441','1544455555666541','1554445555665441',
 '1655444555555441','1555544445555541','1233333333333321','0011111111111100']

def save(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def texture(name,ramp,socket=False):
    c=Canvas(16,16)
    for y,row in enumerate(PANEL):
        for x,index in enumerate(row): c.set(x,y,ramp[int(index)])
    if socket:
        # A stepped, grey-green metal seat recalls stock_link, with no redstone center.
        for lo,hi,index in [(1,14,0),(2,13,2),(3,12,1)]:
            c.rect(lo,lo,hi-lo+1,hi-lo+1,IRON[index])
        c.line_h(2,13,2,IRON[4]); c.line_v(2,3,13,IRON[3])
        c.line_h(3,13,13,IRON[1]); c.line_v(13,3,13,IRON[1])
    path=ASSETS/f'textures/block/fluid_access/{name}.png'
    path.parent.mkdir(parents=True,exist_ok=True); c.save(str(path))

def cube(name,lo,hi,material):
    x,y,z=lo; X,Y,Z=hi
    uvs={'north':[16-X,16-Y,16-x,16-y],'south':[x,16-Y,X,16-y],
         'east':[16-Z,16-Y,16-z,16-y],'west':[z,16-Y,Z,16-y],
         'up':[x,z,X,Z],'down':[x,16-Z,X,16-z]}
    return {'name':name,'from':lo,'to':hi,'faces':{s:{'texture':'#'+material,'uv':uv} for s,uv in uvs.items()}}

def pipe(z,reverse,archive):
    result=[]
    for part in ['rim_connector','rim']:
        model=json.loads(archive.read(f'assets/create/models/block/fluid_pipe/{part}/south.json'))
        for e in model['elements']:
            e['name']='native_'+part
            e.pop('rotation',None)
            for key in ['from','to']:
                e[key][2]+=z-12
                if reverse: e[key][2]=16-e[key][2]
            e['from'][2],e['to'][2]=sorted([e['from'][2],e['to'][2]])
            if reverse:
                e['faces']={({'north':'south','south':'north'}.get(s,s)):f for s,f in e['faces'].items()}
            for f in e['faces'].values():
                f['texture']='#pipe'; f.pop('cullface',None)
            result.append(e)
    return result

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    for material,ramp in [('copper',COPPER),('brass',BRASS)]:
        texture(material,ramp); texture(material+'_socket',ramp,True)
    models=[]
    with ZipFile(JAR) as archive:
        for name,bridge,material in [('fluid_access_port',False,'copper'),('fluid_bridge',True,'brass')]:
            elements=[cube('casing',[0,0,0],[16,16,16],'body')]
            # Recessed sockets at source ends, kept inside a single block.
            ends=['south','north'] if bridge else ['south']
            for end in ends:
                elements[0]['faces'][end]['texture']='#socket'
                # Native pipe mouth at the surface; the narrow collar is inset into the case.
                elements.extend(pipe(12.2,end=='north',archive))
            if bridge:
                # Small native smart-pipe filter panel at the existing interaction position.
                source=json.loads(archive.read('assets/create/models/block/smart_fluid_pipe/block.json'))
                panel=source['elements'][2]
                for end in ends:
                    e=json.loads(json.dumps(panel)); e['name']='native_filter_'+end
                    e.pop('rotation',None)
                    # Fit the original panel to 5x5, centered on y=12, aligned to the native filter transform.
                    original_lo=panel['from']; original_hi=panel['to']
                    for key in ['from','to']:
                        p=panel[key]
                        e[key]=[6+(p[0]-original_lo[0])*4/(original_hi[0]-original_lo[0]),
                                11+(p[1]-original_lo[1])*4/(original_hi[1]-original_lo[1]),
                                16.25+(p[2]-original_lo[2])*.5/(original_hi[2]-original_lo[2])]
                    for f in e['faces'].values(): f['texture']='#filter'; f.pop('cullface',None)
                    e['faces']['south']['uv']=[8,8,16,16]
                    if end=='north':
                        for key in ['from','to']: e[key][2]=16-e[key][2]
                        e['from'][2],e['to'][2]=sorted([e['from'][2],e['to'][2]])
                        e['faces']={({'north':'south','south':'north'}.get(s,s)):f for s,f in e['faces'].items()}
                    elements.append(e)
            textures={'body':f'ccd:block/fluid_access/{material}','socket':f'ccd:block/fluid_access/{material}_socket',
                      'pipe':'create:block/pipes','filter':'create:block/smart_pipe_2','particle':'#body'}
            model={'parent':'minecraft:block/block','ambientocclusion':False,'textures':textures,'elements':elements}
            # Remove the underlying casing faces behind actual open copper mouths.
            for end in ends:
                del elements[0]['faces'][end]
                z=0 if end=='north' else 16
                # Open middle 6x6, visible stock-link styled seat surrounding the native ring.
                for lo,hi in [([0,0,z],[16,4,z]),([0,12,z],[16,16,z]),([0,4,z],[4,12,z]),([12,4,z],[16,12,z])]:
                    e=cube('socket_frame_'+end,lo,hi,'socket')
                    e['faces']={end:e['faces'][end]}; elements.append(e)
                e=cube('dark_pipe_recess',[5,5,z+(.12 if end=='north' else -.12)],[11,11,z+(.12 if end=='north' else -.12)],'dark')
                e['faces']={end:e['faces'][end]}; elements.append(e)
            model['textures']['dark']='ccd:item/fluid_canister/fluid'
            save(ASSETS/f'models/block/{name}.json',model)
            save(ASSETS/f'models/item/{name}.json',{'parent':f'ccd:block/{name}','display':{'gui':{'rotation':[30,45,0],'scale':[.65,.65,.65]}}})
            models.append(model)
        render_preview(models,archive)
    atlas=Image.new('RGBA',(64,16))
    for i,name in enumerate(['copper','copper_socket','brass','brass_socket']):
        atlas.alpha_composite(Image.open(ASSETS/f'textures/block/fluid_access/{name}.png'),(i*16,0))
    atlas.save(OUT/'textures-source.png'); atlas.resize((1024,256),Image.Resampling.NEAREST).save(OUT/'textures.png')
    save(OUT/'model.meta.json',{'reference':'Create stock link socket structure; native fluid pipe rim and smart pipe filter',
         'textures':'CCD hand-drawn 16x16 copper/brass panels and metal socket seats',
         'upstreamResources':['create:block/pipes','create:block/smart_pipe_2'],
         'connectedAssetsCopied':False,'preview':'actual model geometry, offline lighting; no client validation'})

def render_preview(models,archive):
    def texture(id):
        ns,p=id.split(':')
        return Image.open(ASSETS/f'textures/{p}.png').convert('RGBA') if ns=='ccd' else Image.open(BytesIO(archive.read(f'assets/{ns}/textures/{p}.png'))).convert('RGBA')
    def render(model,view):
        def dot(a,b): return sum(x*y for x,y in zip(a,b))
        camera=[v/math.sqrt(dot(view,view)) for v in view]
        r=math.hypot(camera[0],camera[2]); right=[camera[2]/r,0,-camera[0]/r]; up=[-camera[1]*camera[0]/r,r,-camera[1]*camera[2]/r]
        def project(p):
            p=[v-8 for v in p]; return (210+dot(p,right)*15,205-dot(p,up)*15)
        atlas={k:texture(v) for k,v in model['textures'].items() if not v.startswith('#')}
        tiles=[]
        for e in model['elements']:
            x,y,z=e['from']; X,Y,Z=e['to']
            vertices={'north':([[X,Y,z],[X,y,z],[x,y,z],[x,Y,z]],[0,0,-1]),'south':([[x,Y,Z],[x,y,Z],[X,y,Z],[X,Y,Z]],[0,0,1]),
                      'east':([[X,Y,Z],[X,y,Z],[X,y,z],[X,Y,z]],[1,0,0]),'west':([[x,Y,z],[x,y,z],[x,y,Z],[x,Y,Z]],[-1,0,0]),
                      'up':([[x,Y,z],[x,Y,Z],[X,Y,Z],[X,Y,z]],[0,1,0]),'down':([[x,y,Z],[x,y,z],[X,y,z],[X,y,Z]],[0,-1,0])}
            for side,f in e['faces'].items():
                points,n=vertices[side]
                if dot(camera,n)<=0: continue
                image=atlas[f['texture'][1:]]; u,v,U,V=f['uv']; cols=max(1,round(abs(U-u)*image.width/16)); rows=max(1,round(abs(V-v)*image.height/16))
                rotation=f.get('rotation',0)//90
                for row in range(rows):
                    for col in range(cols):
                        def point(a,b): return [points[0][i]+(points[3][i]-points[0][i])*a+(points[1][i]-points[0][i])*b for i in range(3)]
                        poly=[point(col/cols,row/rows),point(col/cols,(row+1)/rows),point((col+1)/cols,(row+1)/rows),point((col+1)/cols,row/rows)]
                        a,b=(col+.5)/cols,(row+.5)/rows
                        for _ in range(rotation): a,b=1-b,a
                        tx=min(image.width-1,max(0,int((u+(U-u)*a)*image.width/16))); ty=min(image.height-1,max(0,int((v+(V-v)*b)*image.height/16)))
                        color=image.getpixel((tx,ty))
                        if color[3]<128: continue
                        light={'up':1,'down':.6,'north':.88,'south':.85,'east':.73,'west':.78}[side]
                        tiles.append((sum(dot(p,camera) for p in poly)/4,[project(p) for p in poly],tuple(round(c*light) for c in color[:3])+(255,)))
        image=Image.new('RGBA',(420,410)); draw=ImageDraw.Draw(image)
        for _,poly,color in sorted(tiles,key=lambda t:t[0]): draw.polygon(poly,fill=color)
        return image
    sheet=Image.new('RGBA',(1260,960),'#e5e8e2'); draw=ImageDraw.Draw(sheet)
    font=ImageFont.truetype(r'C:\Windows\Fonts\msyh.ttc',20)
    for row,model in enumerate(models):
        draw.text((630,row*480+24),'流体扩展端口 · 铜壳' if row==0 else '流体桥接器 · 黄铜壳与双端过滤',font=font,fill='#303535',anchor='mm')
        for col,(view,label) in enumerate([([25,17,34],'连接面'),([-30,18,-32],'另一侧'),([0,0,40],'端面')]):
            sheet.alpha_composite(render(model,view),(col*420,row*480+35))
            draw.text((col*420+210,row*480+456),label,font=font,fill='#303535',anchor='mm')
    sheet.save(OUT/'preview.png')

if __name__=='__main__': main()
