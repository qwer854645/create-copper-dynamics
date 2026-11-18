"""Reference Create's existing hatch model without generating artwork."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/ccd/models/block'
for name in ('fluid_hatch_closed', 'smart_fluid_hatch_closed'):
    (root / (name + '.json')).write_text(json.dumps({
        'parent': 'create:block/item_hatch/block_closed'
    }, indent=2) + '\n', encoding='utf-8')
