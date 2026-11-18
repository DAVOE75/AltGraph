"""
Convierte hojas MDT05 del CNIG (GeoTIFF, UTM 25830/25831) a trozos propios "DEM5":
  - un archivo por trozo de TILE grados (lat/lng), malla fija de ~5 m
  - alturas en decimetros, int16 (void = -32768), comprimidas con zlib
Formato de archivo (little endian):
  magic 'DEM5' | int32 rows | int32 cols | float64 lat_top | float64 lng_left | float64 step_lat | float64 step_lng | zlib(int16[rows*cols])
Nombre: dem5_<lat sur*10>_<lng oeste*10>.bin con prefijo m/p para el signo (p. ej. dem5_p384_m005 = lat 38.4..38.5, lng -0.5..-0.4)
"""
import glob, os, sys, struct, zlib, math
import numpy as np
import rasterio
from rasterio.warp import reproject, Resampling
from rasterio.transform import from_origin
from rasterio.crs import CRS
from pyproj import Transformer

SRC = r'C:\ALTGRAPH\MDT5x5_Spain\Provincia de Alicante'
OUT = r'C:\ALTGRAPH\terrain_out'
TILE = 0.1
STEP_LAT = 5.0 / 111320.0
os.makedirs(OUT, exist_ok=True)

def tile_name(lat_s, lng_w):
    def f(v):
        return ('m' if v < 0 else 'p') + '%03d' % int(round(abs(v) * 10))
    return 'dem5_%s_%s.bin' % (f(lat_s), f(lng_w))

def build(files, only_tile=None):
    srcs = [rasterio.open(f) for f in files]
    lat_min, lat_max, lng_min, lng_max = 90, -90, 180, -180
    for r in srcs:
        t = Transformer.from_crs(r.crs, 'EPSG:4326', always_xy=True)
        b = r.bounds
        for x, y in ((b.left, b.bottom), (b.right, b.top), (b.left, b.top), (b.right, b.bottom)):
            lng, lat = t.transform(x, y)
            lat_min, lat_max = min(lat_min, lat), max(lat_max, lat)
            lng_min, lng_max = min(lng_min, lng), max(lng_max, lng)
    lat_mid = (lat_min + lat_max) / 2
    step_lng = 5.0 / (111320.0 * math.cos(math.radians(lat_mid)))
    la0 = math.floor(lat_min / TILE) * TILE
    ln0 = math.floor(lng_min / TILE) * TILE
    tiles = []
    la = la0
    while la < lat_max:
        ln = ln0
        while ln < lng_max:
            tiles.append((round(la, 1), round(ln, 1)))
            ln += TILE
        la += TILE
    if only_tile:
        tiles = [t for t in tiles if t == only_tile]
    total = 0; written = 0
    for (la, ln) in tiles:
        # +2 celdas: el trozo cubre algo mas que sus 0,1 grados para poder interpolar hasta el mismo limite (sin hueco en las costuras)
        rows = int(math.ceil(TILE / STEP_LAT)) + 2; cols = int(math.ceil(TILE / step_lng)) + 2
        dst = np.full((rows, cols), np.nan, dtype=np.float32)
        dst_tf = from_origin(ln, la + TILE, step_lng, STEP_LAT)
        for r in srcs:
            tmp = np.full((rows, cols), np.nan, dtype=np.float32)
            reproject(source=rasterio.band(r, 1), destination=tmp, src_transform=r.transform, src_crs=r.crs,
                      src_nodata=r.nodata, dst_transform=dst_tf, dst_crs=CRS.from_epsg(4326),
                      dst_nodata=np.nan, resampling=Resampling.bilinear)
            m = ~np.isnan(tmp) & np.isnan(dst)
            dst[m] = tmp[m]
        valid = ~np.isnan(dst)
        if valid.sum() < 0.02 * dst.size:
            continue
        arr = np.where(valid, np.clip(np.round(dst * 10), -30000, 30000), -32768).astype('<i2')
        blob = zlib.compress(arr.tobytes(), 6)
        head = b'DEM5' + struct.pack('<ii', rows, cols) + struct.pack('<dddd', la + TILE, ln, STEP_LAT, step_lng)
        with open(os.path.join(OUT, tile_name(la, ln)), 'wb') as fh:
            fh.write(head + blob)
        total += len(head) + len(blob); written += 1
        print(tile_name(la, ln), '%.1f MB' % ((len(head) + len(blob)) / 1e6), 'valid %.0f %%' % (100 * valid.mean()), flush=True)
    print('trozos:', written, ' total %.1f MB' % (total / 1e6))

if __name__ == '__main__':
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument('--src', default=SRC, help='carpeta con las hojas .tif de la provincia')
    ap.add_argument('--out', default=OUT, help='carpeta de salida de los trozos')
    ap.add_argument('--tile', nargs=2, type=float, help='solo este trozo: lat_sur lng_oeste')
    a = ap.parse_args()
    OUT = a.out
    os.makedirs(OUT, exist_ok=True)
    files = sorted(glob.glob(a.src + r'\**\*.tif', recursive=True))
    build(files, tuple(a.tile) if a.tile else None)