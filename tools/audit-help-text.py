"""Check bilingual help coverage against registered content and storyboard source."""
import json,re
from pathlib import Path
root=Path(__file__).resolve().parents[1]
langs={locale:json.loads((root/f'src/main/resources/assets/ccd/lang/{locale}.json').read_text(encoding='utf-8')) for locale in ['zh_cn','en_us']}
source=(root/'src/main/java/dev/ccd/CopperDynamics.java').read_text(encoding='utf-8')
ids=re.findall(r'ITEMS.register\("([^"]+)',source)
media=['seething_reagent','enriching_reagent','soul_stripping_reagent','explosive_slurry','resonant_solution','sculk_suspension','purifying_solution']
ids += [id+'_bucket' for id in media]
errors=[]
for locale,d in langs.items():
    for id in ids:
        if not any(f'{prefix}.ccd.{id}.tooltip.summary' in d for prefix in ['item','block']):errors.append((locale,'missing summary',id))
    for key,value in d.items():
        if '.tooltip.' in key or '.ponder.' in key:
            if '\ufffd' in value or '??' in value or value.count('_')%2:errors.append((locale,'malformed',key))
        if '.tooltip.condition' in key and key.replace('.condition','.behaviour') not in d:errors.append((locale,'missing behaviour',key))
        if '.tooltip.control' in key and key.replace('.control','.action') not in d:errors.append((locale,'missing action',key))
    plugin=(root/'src/main/java/dev/ccd/client/ponder/CCDPonderPlugin.java').read_text(encoding='utf-8')
    for id in ids:
        if '"'+id+'"' not in plugin:errors.append((locale,'missing ponder registration',id))
    scenes=(root/'src/main/java/dev/ccd/client/ponder/CCDPonderScenes.java').read_text(encoding='utf-8')
    starts=list(re.finditer(r'begin\(builder,"([^"]+)","[^"]+"\)',scenes))
    for i,match in enumerate(starts):
        id=match.group(1);body=scenes[match.end():starts[i+1].start() if i+1<len(starts) else len(scenes)]
        count=len(re.findall(r'\btext\((?:s|scene),',body))
        for suffix in ['header']+[f'text_{n+1}' for n in range(count)]:
            if f'ccd.ponder.{id}.{suffix}' not in d:errors.append((locale,'missing ponder',id,suffix))
assert not errors,errors
help_keys=lambda d:{k for k in d if '.tooltip.' in k}
assert help_keys(langs['zh_cn'])==help_keys(langs['en_us']),'Tooltip keys differ between languages'
for locale,d in langs.items():
    for prefix in {k.split('.tooltip.')[0] for k in help_keys(d)}:
        for group in ['condition','control']:
            indexes=sorted(int(k.rsplit(group,1)[1]) for k in d if k.startswith(prefix+'.tooltip.'+group))
            assert indexes==list(range(1,len(indexes)+1)),(locale,prefix,group,'non-contiguous numbering')
print(f'{len(ids)} directly registered items have bilingual Shift summaries; all storyboard lines and tooltip pairs resolve.')
