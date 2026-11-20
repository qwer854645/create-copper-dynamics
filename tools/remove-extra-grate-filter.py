"""Use the imported grate's existing lower filter slot; remove added upper panels."""
import json
from pathlib import Path
root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/ccd'
path = root / 'blockstates/smart_collection_grate.json'
data = json.loads(path.read_text())
data['multipart'] = [part for part in data['multipart']
                     if part['apply']['model'] != 'ccd:block/smart_collection_grate_filter']
path.write_text(json.dumps(data, indent=2) + '\n')
path = root / 'models/item/smart_collection_grate.json'
data = json.loads(path.read_text())
data['elements'] = [e for e in data['elements'] if not e.get('name', '').startswith('filter_')]
data['textures'].pop('filter', None)
path.write_text(json.dumps(data, indent=2) + '\n')
