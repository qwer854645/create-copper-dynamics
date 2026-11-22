// Runs at server-script top level, not inside ServerEvents.recipes.
// Arguments: rule ID, fluid family, block ID or #block tag, source/flowing/both,
// replacement at the fluid position, replacement at the touched block position.

// Preserve water and mossify adjacent vanilla moss targets.
CCDFluids.blockInteraction('example:water_moss', 'minecraft:water',
  '#minecraft:moss_replaceable', 'both', null, 'minecraft:moss_block')

// Only source lava reacts: replace the lava, leave the touched glass intact.
CCDFluids.blockInteraction('example:glass_cools_lava', 'minecraft:lava',
  'minecraft:glass', 'source', 'minecraft:obsidian', null)

// Flowing water and the touched block both change. Air explicitly removes fluid.
CCDFluids.blockInteraction('example:flowing_water_clay', 'minecraft:water',
  'minecraft:sand', 'flowing', 'minecraft:air', 'minecraft:clay')

// CCDFluids.remove('example:water_moss')
