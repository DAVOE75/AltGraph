import os
import fitdecode

T = os.environ['TEMP']
pts = []
with fitdecode.FitReader(T + r'\fit1\r1.fit') as fr:
    for fm in fr:
        if isinstance(fm, fitdecode.FitDataMessage) and fm.name == 'record':
            la = fm.get_value('position_lat', fallback=None); lo = fm.get_value('position_long', fallback=None)
            al = fm.get_value('enhanced_altitude', fallback=None)
            if al is None:
                al = fm.get_value('altitude', fallback=None)
            if la is None or lo is None:
                continue
            pts.append('<trkpt lat="%.6f" lon="%.6f"><ele>%.1f</ele></trkpt>' % (la * 180 / 2**31, lo * 180 / 2**31, al if al is not None else 0))
open(T + r'\fit1\replay.gpx', 'w').write('<?xml version="1.0"?><gpx><trk><trkseg>' + ''.join(pts) + '</trkseg></trk></gpx>')
print(len(pts))
