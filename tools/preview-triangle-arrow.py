"""Standalone nine-pixel triangular marking study; does not touch model resources."""
from pathlib import Path
import sys
sys.path.insert(0,r'C:\Users\Administrator\.config\opencode\skills\pixel-art\scripts')
from pxl import Canvas
from PIL import Image
root=Path(__file__).resolve().parents[1]
out=root/'outputs/variable-pump-arrows';out.mkdir(parents=True,exist_ok=True)
arrow=Canvas(3,5)
# Right-pointing solid triangle: 1, 2, 3, 2, 1 pixels. No shaft.
for y,xs in enumerate([[0],[0,1],[0,1,2],[0,1],[0]]):
    for x in xs:arrow.set(x,y,'#713b2b')
arrow.save(str(out/'triangle-arrow.png'))
background=Canvas(9,10,fill='#c87856')
background.blit(arrow,3,3)
background.save(str(out/'triangle-arrow-on-copper.png'))
Image.open(out/'triangle-arrow-on-copper.png').resize((360,400),Image.Resampling.NEAREST).save(out/'triangle-arrow-preview.png')
