"""Preview the imported item using actual textures, UVs and element rotations."""
from pathlib import Path
import importlib.util

source=Path(__file__).with_name('preview-smart-grate-indicators.py')
spec=importlib.util.spec_from_file_location('model_preview',source)
preview=importlib.util.module_from_spec(spec)
spec.loader.exec_module(preview)
preview.render('ccd:item/buffer_pump').save(preview.root/'outputs/buffer-pump-revision/preview.png')
