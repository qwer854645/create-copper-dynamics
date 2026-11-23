"""Import the latest user slime bucket icon as an independent static item sprite."""
from pathlib import Path
import json,shutil
from PIL import Image
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/ccd'
source=Path(r'C:\Users\Administrator\Downloads\史莱姆 (1).png')
out=root/'outputs/slime-bucket-user-art';out.mkdir(parents=True,exist_ok=True)
model=assets/'models/item/slime_bucket.json'
if not (out/'slime_bucket-model-before.json').exists():shutil.copy2(model,out/'slime_bucket-model-before.json')
image=Image.open(source);assert image.size==(16,16)
shutil.copy2(source,assets/'textures/item/slime_bucket.png')
model.write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'ccd:item/slime_bucket'}},indent=2)+'\n')
image.resize((256,256),Image.Resampling.NEAREST).save(out/'preview.png')
assert source.read_bytes()==(assets/'textures/item/slime_bucket.png').read_bytes()
print('Imported exact latest 16x16 slime bucket artwork; item now uses a static sprite.')
