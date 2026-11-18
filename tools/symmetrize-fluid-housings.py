"""Balance rotatable copper panels; preserve native apertures, filters and flow marks."""
import sys
from pathlib import Path
from PIL import Image
sys.path.insert(0,str(Path.home()/'.config/opencode/skills/pixel-art/scripts'))
from pxl import Canvas
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'src/main/resources/assets/ccd/textures/block'
# One triangular eighth of the material. Reflect across both axes and diagonals:
# irregular joined clusters remain, but no fixed bright-top/dark-bottom direction.
PATCH=['277777','765566','754455','765445','765554','654455']
PUMP=[(70,32,24),(144,73,49),(154,80,56),(167,90,64),(178,98,71),(194,107,76),(200,116,86),(214,123,91),(227,130,108)]
TRANSFER=[(96,46,31),(144,73,49),(154,80,56),(167,90,64),(178,98,71),(194,107,76),(214,123,91),(227,130,108)]
def material(palette,full):
    canvas=Canvas(16,16)
    for y in range(16):
        for x in range(16):
            a,b=sorted((min(x,15-x),min(y,15-y)))
            if a<2:
                index=(1 if a==0 else 6) if full else 5
            else:
                index=int(PATCH[a-2][b-2])
            canvas.set(x,y,palette[min(index,len(palette)-1)])
    return canvas.img
def main():
    selected=sys.argv[1] if len(sys.argv)>1 else None
    for folder,palette,full in [('booster_pump',PUMP,False),('fluid_transmitter',TRANSFER,True)]:
        if selected and folder!=selected: continue
        body=material(palette,full)
        body.save(BASE/folder/'body.png')
        if folder=='booster_pump':
            path=BASE/folder/'shaft_face.png'; old=Image.open(path).convert('RGBA')
            # Native 8x8 shaft recess is kept verbatim, including its own light shading.
            body.paste(old.crop((4,4,12,12)),(4,4)); body.save(path)
            names=['body','shaft_face']
        else:
            for name in ['side','top']:
                path=BASE/folder/(name+'.png'); old=Image.open(path).convert('RGBA')
                panel=material(palette,full).resize((32,32),Image.Resampling.NEAREST)
                panel.paste(old.crop((8,8,24,24)),(8,8))
                # Preserve only the actual five-row arrow mask, not its old background.
                arrow=['0000100','0000110','1111111','0000110','0000100']
                for y,row in enumerate(arrow):
                    for x,value in enumerate(row):
                        if value=='1':
                            u,v=(12+x,2+y) if name=='side' else (2+y,12+x)
                            panel.putpixel((u,v),old.getpixel((u,v)))
                panel.save(path)
            names=['body','side','top']
        output=ROOT/'outputs'/('booster-pump' if folder=='booster_pump' else 'fluid-transmitter')
        sheet=Image.new('RGBA',(len(names)*272-16,256),'#e5e8e2')
        for i,name in enumerate(names):
            sheet.alpha_composite(Image.open(BASE/folder/(name+'.png')).resize((256,256),Image.Resampling.NEAREST),(272*i,0))
        sheet.save(output/'textures.png')
    for folder in ['booster_pump','fluid_transmitter']:
        if selected and folder!=selected: continue
        image=Image.open(BASE/folder/'body.png')
        assert image.tobytes()==image.transpose(Image.Transpose.FLIP_LEFT_RIGHT).tobytes()
        assert image.tobytes()==image.transpose(Image.Transpose.ROTATE_90).tobytes()
    print('Both copper body textures: mirror and quarter-turn symmetry verified. Native details preserved.')
if __name__=='__main__': main()
