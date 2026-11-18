"""Optional upstream catalyst tags; no copied recipes or mandatory dependencies."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/data'
def tag(namespace,kind,name,entries):
    p=ROOT/namespace/f'tags/{kind}'/(name+'.json'); p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps({'replace':False,'values':entries},indent=2)+'\n',encoding='utf-8')
tag('c','block','quicksands',['ccd:quicksand'])
for mod in ['create_dragons_plus','dndesires']:
    tag(mod,'block','fan_processing_catalysts/sanding',['ccd:quicksand'])
    tag(mod,'fluid','fan_processing_catalysts/sanding',['ccd:quicksand','ccd:flowing_quicksand'])
for name in ['enriched','soul_stripping']:
    tag('ccd','fluid','fan_media/'+name,[])
