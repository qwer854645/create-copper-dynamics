"""Offline illustration with the actual native container attachment model."""
from pathlib import Path
import importlib.util,json
root=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('preview',root/'tools/preview-smart-grate-indicators.py')
p=importlib.util.module_from_spec(spec);spec.loader.exec_module(p)
body=p.model('ccd:item/variable_flow_pump')
drain=p.model('create:block/fluid_pipe/drain/north')
for element in drain['elements']:
    for f in element['faces'].values():
        tex=f['texture']
        while tex.startswith('#'):tex=drain['textures'][tex[1:]]
        key='attachment_'+str(len(body['textures']));body['textures'][key]=tex;f['texture']='#'+key
    body['elements'].append(element)
p.model=lambda name:body
out=root/'outputs/pump-container-attachment';out.mkdir(parents=True,exist_ok=True)
p.render('preview').save(out/'preview.png')
