"""Render Doomly's eyes icon into Android launcher and notification assets.
Source: store-assets/doomly-icon.svg. Requires CairoSVG and Pillow for generation.
"""
from pathlib import Path
import io
import xml.etree.ElementTree as ET
import cairosvg
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
res = ROOT / 'app/src/main/res'
source = ROOT / 'store-assets/doomly-icon.svg'
svg = source.read_text()
ns = {'svg': 'http://www.w3.org/2000/svg'}
root = ET.fromstring(svg)
background = root.find('svg:rect', ns).get('fill')
eyes = root.find('svg:path', ns)
path = eyes.get('d')
fill = eyes.get('fill')
cairosvg.svg2png(bytestring=svg.encode(), write_to=str(ROOT / 'store-assets/playstore-icon.png'), output_width=512, output_height=512)
for density, size in [('ldpi', 36), ('mdpi', 48), ('hdpi', 72), ('xhdpi', 96), ('xxhdpi', 144), ('xxxhdpi', 192)]:
    folder = res / f'mipmap-{density}'
    folder.mkdir(exist_ok=True)
    for rounded, name in [(False, 'ic_launcher'), (True, 'ic_launcher_round')]:
        bg = f'<circle cx="54" cy="54" r="54" fill="{background}"/>' if rounded else f'<rect width="108" height="108" fill="{background}"/>'
        picture = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">{bg}<path d="{path}" fill="{fill}" fill-rule="evenodd"/></svg>'
        im = Image.open(io.BytesIO(cairosvg.svg2png(bytestring=picture.encode(), output_width=size, output_height=size)))
        im.save(folder / f'{name}.webp', lossless=True)

def vector(name, color, viewport, group=''):
    content = f'<path android:pathData="{path}" android:fillColor="{color}" android:fillType="evenOdd"/>'
    if group:
        content = f'<group {group}>{content}</group>'
    (res / f'drawable/{name}.xml').write_text(f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="{viewport}dp" android:height="{viewport}dp" android:viewportWidth="{viewport}" android:viewportHeight="{viewport}">{content}</vector>\n')

# Full mark fits within the adaptive icon's central safe zone. Holes stay transparent.
vector('ic_launcher_foreground', fill, 108)
vector('ic_launcher_monochrome', '#000000', 108)
vector('ic_stat_doomly', '#FFFFFF', 24,
       'android:translateX="-9.6" android:translateY="-10" android:scaleX="0.4" android:scaleY="0.4"')
for name in ['ic_launcher', 'ic_launcher_round']:
    (res / f'mipmap-anydpi-v26/{name}.xml').write_text('<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@color/ic_launcher_background"/><foreground android:drawable="@drawable/ic_launcher_foreground"/><monochrome android:drawable="@drawable/ic_launcher_monochrome"/></adaptive-icon>\n')
(res / 'values/ic_launcher_background.xml').write_text(f'<resources><color name="ic_launcher_background">{background}</color></resources>\n')
(res / 'drawable/ic_launcher_background.xml').write_text(f'<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="{background}"/></shape>\n')
