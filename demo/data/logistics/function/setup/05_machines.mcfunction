# Demo 5: Machines
#
# Two sub-demos: Kiln (z=70) and Macerator (z=85)
#
# Layout (both at x=5, same structure):
#   x=5, y=59   Input chest (smeltable/grindable items)
#   x=5, y=58   Hopper [facing=down] → feeds machine below
#   x=5, y=57   Machine (Kiln or Macerator) [facing=east]
#   x=6, y=57   Creative Engine [facing=west] → provides infinite RF
#   x=5, y=56   Item Extractor Pipe (pulls output from machine bottom)
#   x=4, y=56   Redstone Engine [facing=east] → powers extractor
#   x=3, y=56   Redstone Block (constant power to engine)
#   x=6-8,y=56  Stone Transport Pipes
#   x=9, y=56   Output Chest (test target)
#
# Kiln (z=70):     raw_iron + sand → iron ingot, glass
# Macerator(z=85): iron_ore → iron_dust x2 (demonstrates ore doubling)

# --- Kiln Signpost ---
setblock 2 56 68 minecraft:chiseled_stone_bricks
setblock 2 57 68 minecraft:chiseled_stone_bricks
setblock 3 57 68 minecraft:oak_wall_sign[facing=east]{front_text:{messages:['{"text":"Demo 5a","color":"gold","bold":true}','{"text":"Kiln","color":"white"}','{"text":"RF-powered smelting","color":"gray"}','{"text":"(Creative Engine)","color":"gray"}'],color:"black",has_glowing_text:0b}}

# Kiln input chest (y=59, items that can be smelted)
setblock 5 59 70 minecraft:chest[facing=east]
item replace block 5 59 70 container.0 with minecraft:raw_iron 32
item replace block 5 59 70 container.1 with minecraft:sand 32

# Hopper feeds down into kiln
setblock 5 58 70 minecraft:hopper[facing=down]

# Kiln block
setblock 5 57 70 logistics:automation/kiln[facing=east]

# Creative engine adjacent to kiln (provides infinite RF)
setblock 6 57 70 logistics:power/creative_engine[facing=west]

# Item extractor pipe below kiln, pulls output
setblock 5 56 70 logistics:pipe/item_extractor_pipe

# Redstone engine powers the extractor
setblock 4 56 70 logistics:power/redstone_engine[facing=east]
setblock 3 56 70 minecraft:redstone_block

# Transport pipes to output chest
setblock 6 56 70 logistics:pipe/stone_transport_pipe
setblock 7 56 70 logistics:pipe/stone_transport_pipe
setblock 8 56 70 logistics:pipe/stone_transport_pipe

# Output chest (test target)
setblock 9 56 70 minecraft:chest[facing=west]

# Labels (oak_sign rotation=8 faces north, readable as player approaches from south)
setblock 5 60 70 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Kiln Input","color":"yellow"}','{"text":"Raw Iron + Sand","color":"gray"}','{"text":"","color":"white"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
setblock 9 57 70 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Kiln Output","color":"yellow"}','{"text":"(test target)","color":"gray"}','{"text":"","color":"white"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}

# --- Macerator Signpost ---
setblock 2 56 83 minecraft:chiseled_stone_bricks
setblock 2 57 83 minecraft:chiseled_stone_bricks
setblock 3 57 83 minecraft:oak_wall_sign[facing=east]{front_text:{messages:['{"text":"Demo 5b","color":"gold","bold":true}','{"text":"Macerator","color":"white"}','{"text":"Ore doubling","color":"gray"}','{"text":"iron_ore -> 2 dust","color":"gray"}'],color:"black",has_glowing_text:0b}}

# Macerator input chest (iron ore for doubling demo)
setblock 5 59 85 minecraft:chest[facing=east]
item replace block 5 59 85 container.0 with minecraft:iron_ore 32

# Hopper feeds down into macerator
setblock 5 58 85 minecraft:hopper[facing=down]

# Macerator block
setblock 5 57 85 logistics:core/macerator[facing=east]

# Creative engine adjacent to macerator (provides infinite RF)
setblock 6 57 85 logistics:power/creative_engine[facing=west]

# Item extractor pipe below macerator, pulls output
setblock 5 56 85 logistics:pipe/item_extractor_pipe

# Redstone engine powers the extractor
setblock 4 56 85 logistics:power/redstone_engine[facing=east]
setblock 3 56 85 minecraft:redstone_block

# Transport pipes to output chest
setblock 6 56 85 logistics:pipe/stone_transport_pipe
setblock 7 56 85 logistics:pipe/stone_transport_pipe
setblock 8 56 85 logistics:pipe/stone_transport_pipe

# Output chest (test target)
setblock 9 56 85 minecraft:chest[facing=west]

# Labels (oak_sign rotation=8 faces north, readable as player approaches from south)
setblock 5 60 85 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Macerator Input","color":"yellow"}','{"text":"Iron Ore","color":"gray"}','{"text":"","color":"white"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
setblock 9 57 85 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Macerator Output","color":"yellow"}','{"text":"2x iron_dust","color":"green"}','{"text":"(test target)","color":"gray"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
