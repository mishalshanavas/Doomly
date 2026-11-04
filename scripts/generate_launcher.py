"""Render launcher densities from Doomly's native board-pawn vector.
Requires cairosvg and Pillow; these are asset-generation tools, not app dependencies.
"""
from pathlib import Path
import io
import xml.etree.ElementTree as ET
import cairosvg
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
res = ROOT / 'app/src/main/res'
ns = '{http://schemas.android.com/apk/res/android}'
pawn = ET.parse(res / 'drawable/board_pawn.xml').getroot()
paths = ''.join(f'<path d="{p.get(ns+"pathData")}" fill="{p.get(ns+"fillColor")}" stroke="#292929" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>' for p in pawn)
foreground = f'<g transform="translate(28 27) scale(.8)">{paths}</g>'
svg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108"><rect width="108" height="108" fill="#0acf70"/>{foreground}</svg>'
(ROOT/'store-assets/doomly-icon.svg').write_text(svg)
cairosvg.svg2png(bytestring=svg.encode(),write_to=str(ROOT/'store-assets/playstore-icon.png'),output_width=512,output_height=512)
for density, size in [('ldpi',36),('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
    folder=res/f'mipmap-{density}'; folder.mkdir(exist_ok=True)
    for rounded,name in [(False,'ic_launcher'),(True,'ic_launcher_round')]:
        bg='<circle cx="54" cy="54" r="54" fill="#0acf70"/>' if rounded else '<rect width="108" height="108" fill="#0acf70"/>'
        picture=f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">{bg}{foreground}</svg>'
        im=Image.open(io.BytesIO(cairosvg.svg2png(bytestring=picture.encode(),output_width=size,output_height=size)))
        im.save(folder/f'{name}.webp',lossless=True)
# Android vector foreground, placed inside the adaptive icon safe zone.
vector=f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108"><group android:translateX="28" android:translateY="27" android:scaleX="0.8" android:scaleY="0.8">'
for p in pawn:
    vector+=f'<path android:pathData="{p.get(ns+"pathData")}" android:fillColor="{p.get(ns+"fillColor")}" android:strokeColor="#292929" android:strokeWidth="1.6" android:strokeLineJoin="round" android:strokeLineCap="round"/>'
vector+='</group></vector>\n'
(res/'drawable/ic_launcher_foreground.xml').write_text(vector)
shape=pawn[0].get(ns+'pathData')
(res/'drawable/ic_launcher_monochrome.xml').write_text(f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108"><group android:translateX="28" android:translateY="27" android:scaleX="0.8" android:scaleY="0.8"><path android:pathData="{shape}" android:fillColor="#000000"/></group></vector>\n')
(res/'drawable/ic_stat_doomly.xml').write_text(f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><group android:translateX="0.5" android:translateY="0.2" android:scaleX="0.36" android:scaleY="0.36"><path android:pathData="{shape}" android:fillColor="#FFFFFF"/></group></vector>\n')
for name in ['ic_launcher','ic_launcher_round']:
    (res/f'mipmap-anydpi-v26/{name}.xml').write_text('<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@color/ic_launcher_background"/><foreground android:drawable="@drawable/ic_launcher_foreground"/><monochrome android:drawable="@drawable/ic_launcher_monochrome"/></adaptive-icon>\n')
(res/'values/ic_launcher_background.xml').write_text('<resources><color name="ic_launcher_background">#0ACF70</color></resources>\n')
(res/'drawable/ic_launcher_background.xml').write_text('<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="#0ACF70"/></shape>\n')
