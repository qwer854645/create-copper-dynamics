// Directional rule: first fluid is replaced on side/upper contact with the second.
// Arguments: rule ID, first fluid, second fluid, source result, flowing result, downward result.
// Downward result replaces the SECOND fluid when the first flows down into it.
// Empty result '' leaves that interaction unchanged. Register at script top level.
CCDFluids.interaction('example:lava_water',
  'minecraft:lava', 'minecraft:water',
  'minecraft:obsidian', 'minecraft:cobblestone', 'minecraft:stone')

// Custom example (enable after registering kubejs:brine in startup_scripts):
// CCDFluids.interaction('example:brine_lava',
//   'kubejs:brine', 'minecraft:lava',
//   'minecraft:calcite', 'minecraft:stone', 'minecraft:basalt')
// CCDFluids.remove('example:brine_lava') // Removes only a CCD script rule, not vanilla rules.
