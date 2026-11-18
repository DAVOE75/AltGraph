"""
Empaqueta los trozos DEM5 de una provincia en un .zip y actualiza el indice provincias.json.

Uso:
  python tools/convert_dem5.py --src "C:/.../Provincia de Alicante" --out terrain_work/alicante
  python tools/package_province.py alicante "Alicante" terrain_work/alicante

Cada trozo se renombra a dem5_<lat>_<lng>@<id>.bin: dos provincias vecinas comparten trozos de borde y cada paquete solo trae los
datos de sus propias hojas, asi que la app los combina al leer en lugar de sobrescribirlos.
El zip va SIN comprimir (los trozos ya lo estan) y lleva LICENCIA.txt con la atribucion exigida por el CNIG.
Los archivos de terrain_pkg/ se publican como assets de la release 'terrain-v1' en GitHub.
"""
import argparse, json, os, zipfile

ATTRIBUTION = ("Obra derivada de MDT05 CC-BY 4.0 scne.es (Instituto Geografico Nacional). "
               "Modelo Digital del Terreno de 5 m del CNIG, convertido a un formato propio por AltGraph. "
               "Licencia: https://creativecommons.org/licenses/by/4.0/")

ap = argparse.ArgumentParser()
ap.add_argument('id')
ap.add_argument('name')
ap.add_argument('tiles_dir')
ap.add_argument('--out', default=os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'terrain_pkg'))
a = ap.parse_args()

out = os.path.abspath(a.out)
os.makedirs(out, exist_ok=True)
files = sorted(f for f in os.listdir(a.tiles_dir) if f.startswith('dem5_') and f.endswith('.bin') and '@' not in f)
if not files:
    raise SystemExit('No hay trozos dem5_*.bin en ' + a.tiles_dir)

zip_path = os.path.join(out, a.id + '.zip')
with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_STORED) as z:
    z.writestr('LICENCIA.txt', ATTRIBUTION + '\n')
    for f in files:
        z.write(os.path.join(a.tiles_dir, f), f.replace('.bin', '@%s.bin' % a.id))

index_path = os.path.join(out, 'provincias.json')
index = {'version': 1, 'attribution': ATTRIBUTION, 'provinces': []}
if os.path.isfile(index_path):
    index = json.load(open(index_path, encoding='utf-8'))
index['provinces'] = [p for p in index['provinces'] if p['id'] != a.id]
index['provinces'].append({'id': a.id, 'name': a.name, 'url': a.id + '.zip', 'bytes': os.path.getsize(zip_path), 'tiles': len(files)})
index['provinces'].sort(key=lambda p: p['name'])
json.dump(index, open(index_path, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
print('%s: %d trozos, %.1f MB -> %s' % (a.name, len(files), os.path.getsize(zip_path) / 1e6, zip_path))
