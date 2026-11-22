"""World pointers are animated by the renderer; keep the item representation intact."""
from pathlib import Path
import json
p=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd/models/block/variable_flow_pump/body.json'
d=json.loads(p.read_text())
d['elements']=[e for e in d['elements'] if not(e.get('name')=='signal_gauge_head' and all(abs(b-a)<1.2 for a,b in zip(e['from'],e['to'])))]
p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
