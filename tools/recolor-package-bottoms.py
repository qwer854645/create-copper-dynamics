"""Recolor only the lower rim and underside; keep the original iron closures above."""
import json
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/ccd'
STEEL=[(53,52,56),(63,62,66),(79,79,79),(89,88,88),(103,97,97),(152,151,151)]
PALETTES={
    'fluid_canister':[(121,59,43),(144,73,49),(167,90,64),(194,107,76),(214,123,91),(227,130,108)],
    'brass_box':[(114,71,49),(164,107,71),(199,151,88),(215,170,94),(247,203,108),(251,220,125)],
}
for name,palette in PALETTES.items():
    target=ASSETS/f'textures/item/{name}'
    for part in ['band','flat']:
        image=Image.open(ASSETS/f'textures/item/fluid_canister/{part}.png').convert('RGBA')
        colors=dict(zip(STEEL,palette))
        pixels=image.load()
        for y in range(image.height):
            for x in range(image.width):
                r,g,b,a=pixels[x,y]
                pixels[x,y]=(*colors.get((r,g,b),(r,g,b)),a)
        image.save(target/f'bottom_{part}.png')
    model_path=ASSETS/f'models/item/{"fluid_package" if name=="fluid_canister" else "composite_package"}.json'
    model=json.loads(model_path.read_text(encoding='utf-8'))
    for part in ['band','flat']: model['textures']['bottom_'+part]=f'ccd:item/{name}/bottom_{part}'
    for element in model['elements']:
        if element.get('name')=='bottom_rim':
            for face in element['faces'].values(): face['texture']='#bottom_flat' if face['texture'] in ['#flat','#bottom_flat'] else '#bottom_band'
    model_path.write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
