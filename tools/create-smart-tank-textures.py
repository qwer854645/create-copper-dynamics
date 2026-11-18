"""Rebuild smart tank artwork from native Create tanks and brass casing edges.

Usage: py tools/create-smart-tank-textures.py <ccd-assets-root> <Create jar or extracted root> [--verify|--check]
Never reads the previous CCD textures as input. Geometry, UVs and alpha stay native.
"""
from __future__ import annotations

import io
import sys
from pathlib import Path
from zipfile import ZipFile
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path.home()/'.config/opencode/skills/pixel-art/scripts'))
from pxl import Canvas

NAMES = ('fluid_tank', 'fluid_tank_top', 'fluid_tank_inner',
         'fluid_tank_connected', 'fluid_tank_top_connected', 'fluid_tank_inner_connected',
         'fluid_tank_window', 'fluid_tank_window_single')


def load_create(source, name):
    relative = 'assets/create/textures/block/' + name + '.png'
    if source.is_file():
        with ZipFile(source) as archive:
            return Image.open(io.BytesIO(archive.read(relative))).convert('RGBA')
    return Image.open(source/relative).convert('RGBA')


def derive(source):
    casing = load_create(source, 'brass_casing')
    result = {}
    for name in NAMES:
        native = load_create(source, name)
        canvas = Canvas(native.width, native.height)
        for y in range(native.height):
            for x in range(native.width):
                color = native.getpixel((x, y))
                # Tank frame UVs use the outer four columns and upper/lower
                # bands. Three pixels reproduce the native casing's folded rim.
                # Repeat at native texel scale on CT sheets: every segment has
                # brass edges, while Create's transparent unused cells stay clear.
                if 'window' not in name and color[3] and min(x % 16, y % 16, 15-x % 16, 15-y % 16) < 3:
                    color = casing.getpixel((x % 16, y % 16))[:3] + (color[3],)
                canvas.set(x, y, color)
        # Canvas owns pixel editing; save to bytes to return ordinary Pillow images.
        stream = io.BytesIO()
        canvas.img.save(stream, format='PNG')
        stream.seek(0)
        result[name] = Image.open(stream).convert('RGBA')
    return result


def main(argv):
    assets, source = Path(argv[1]), Path(argv[2])
    if assets.name == 'assets':
        assets /= 'ccd'
    destination = assets/'textures/block/smart_fluid_tank'
    images = derive(source)
    sheet = Image.new('RGBA', (1024, 560), '#e5e8e2')
    draw = ImageDraw.Draw(sheet)
    for index, (name, image) in enumerate(images.items()):
        native = load_create(source, name)
        assert image.size == native.size and image.getchannel('A').tobytes() == native.getchannel('A').tobytes(), name
        x, y = index % 4 * 256, index // 4 * 280
        sheet.alpha_composite(image.resize((224, 224), Image.Resampling.NEAREST), (x+16, y+12))
        draw.text((x+6, y+246), name, fill='black')
        path = destination/(name+'.png')
        if '--verify' in argv:
            with Image.open(path) as current:
                assert current.convert('RGBA').tobytes() == image.tobytes() and current.size == image.size, name
        elif '--check' not in argv:
            image.save(path)
        print(name, image.size)
    if '--verify' not in argv and '--check' not in argv:
        output = Path(__file__).resolve().parents[1]/'outputs/brass-edge-tanks'
        output.mkdir(parents=True, exist_ok=True)
        sheet.save(output/'textures.png')
    return 0


if __name__ == '__main__':
    raise SystemExit(main(sys.argv))
