"""Import the user's five November 22 bucket icons without recoloring or resizing."""
from pathlib import Path
import shutil
from PIL import Image, ImageDraw, ImageFont

root=Path(__file__).resolve().parents[1]
downloads=Path(r'C:\Users\Administrator\Downloads')
textures=root/'src/main/resources/assets/ccd/textures/item'
out=root/'outputs/media-bucket-art-nov22'
backup=out/'backup'
backup.mkdir(parents=True,exist_ok=True)
entries=[
    ('催泪.png','tear_solution_bucket.png','催泪溶液'),
    ('苔藓.png','moss_suspension_bucket.png','苔藓悬浊液'),
    ('enriching_reagent_bucket.png','enriching_reagent_bucket.png','富集介质'),
    ('purifying_solution_bucket.png','purifying_solution_bucket.png','净化液'),
    ('soul_stripping_reagent_bucket.png','soul_stripping_reagent_bucket.png','剥魂液'),
]
preview=Image.new('RGB',(1000,240),'#e5e8e2')
draw=ImageDraw.Draw(preview)
font=ImageFont.truetype(r'C:\Windows\Fonts\msyh.ttc',20)
for i,(source,name,label) in enumerate(entries):
    original=downloads/source
    image=Image.open(original).convert('RGBA')
    assert image.size==(16,16),source
    target=textures/name
    if not (backup/name).exists():shutil.copy2(target,backup/name)
    shutil.copy2(original,target)
    assert target.read_bytes()==original.read_bytes()
    preview.paste(image.resize((128,128),Image.Resampling.NEAREST),(i*200+36,32),image.resize((128,128),Image.Resampling.NEAREST))
    draw.text((i*200+100,185),label,font=font,fill='#303535',anchor='mt')
preview.save(out/'preview.png')
print('Imported five exact 16x16 icons; originals backed up and byte equality verified.')
