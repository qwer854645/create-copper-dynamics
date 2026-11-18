"""Pixel-art copper study using the vanilla bucket pixel layout.

Vanilla silhouettes/shading remain Mojang-derived artwork, not original CCD art.
Only writes offline studies; does not modify runtime assets.
"""
from io import BytesIO
from pathlib import Path
import sys
from zipfile import ZipFile

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path.home() / '.config/opencode/skills/pixel-art/scripts'))
from pxl import Canvas

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'outputs/copper-bucket/vanilla-copper'
# Dark outline -> lit copper; preserve each original shading region exactly.
RAMP = {
    53: '#573327',
    84: '#75422e',
    95: '#8d4d34',
    114: '#a35b3e',
    150: '#bc6c4b',
    168: '#cd7b56',
    216: '#e6a17a',
    255: '#f5c29b',
}


def repaint(source, vanilla_shell):
    result = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            pixel = source.getpixel((x, y))
            # Filled variants share the shell, but replace the opening with liquid.
            if pixel[3] and pixel == vanilla_shell.getpixel((x, y)):
                result.set(x, y, RAMP[pixel[0]])
            else:
                result.set(x, y, pixel)
    return result.img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    jar = next((ROOT / 'build/moddev/artifacts').glob('*minecraft-resources.jar'))
    with ZipFile(jar) as assets:
        originals = [Image.open(BytesIO(assets.read(
            f'assets/minecraft/textures/item/{name}.png'))).convert('RGBA')
            for name in ('bucket', 'water_bucket', 'lava_bucket')]
    copper = [repaint(image, originals[0]) for image in originals]
    for name, image in zip(('empty', 'water', 'lava'), copper):
        image.save(OUT / f'{name}.png')
    preview = Image.new('RGBA', (720, 294), '#e5e7e1')
    draw = ImageDraw.Draw(preview)
    font = ImageFont.truetype(r'C:\Windows\Fonts\msyh.ttc', 17)
    draw.text((360, 26), '铜桶 · 原版轮廓与明暗 / CCD 铜色',
              fill='#303732', font=font, anchor='mm')
    for index, (image, label) in enumerate(zip(
            [originals[0], *copper], ('原版铁桶', '铜桶 · 空', '铜桶 · 水', '铜桶 · 熔岩'))):
        center = 90 + index * 180
        preview.alpha_composite(image.resize((160, 160), Image.Resampling.NEAREST), (center - 80, 48))
        draw.rectangle((center - 14, 216, center + 14, 244), fill='#b9bbb5')
        preview.alpha_composite(image, (center - 8, 222))
        draw.text((center, 266), label, fill='#303732', font=font, anchor='mm')
    preview.save(OUT / 'preview.png')


if __name__ == '__main__':
    main()
