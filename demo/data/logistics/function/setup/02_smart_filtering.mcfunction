# Demo 2: Smart Filtering
#
# Layout (y=56, z=25):
#   x=5  Chest A (source: iron ingots + diamonds mixed)
#   x=6  Item Extractor Pipe
#   x=7  Stone Transport Pipe
#   x=8  Item Filter Pipe (T-junction)
#         z=23  Stone Pipe → Chest (iron ingots)
#         z=27  Stone Pipe → Chest (diamonds)
#
#   x=6, z=24  Redstone Engine [facing=south]
#   x=6, z=23  Redstone Block
#
# NOTE: Open the filter pipe GUI to configure:
#   North output (z-): iron_ingot
#   South output (z+): diamond

# Signpost
setblock 2 56 23 minecraft:chiseled_stone_bricks
setblock 2 57 23 minecraft:chiseled_stone_bricks
setblock 3 57 23 minecraft:oak_wall_sign[facing=east]{front_text:{messages:['{"text":"Demo 2","color":"gold","bold":true}','{"text":"Smart Filtering","color":"white"}','{"text":"Configure filter pipe","color":"gray"}','{"text":"to route by item type","color":"gray"}'],color:"black",has_glowing_text:0b}}

# Source chest - mixed items
setblock 5 56 25 minecraft:chest[facing=east]
item replace block 5 56 25 container.0 with minecraft:iron_ingot 32
item replace block 5 56 25 container.1 with minecraft:diamond 32

# Pipe network
setblock 6 56 25 logistics:pipe/item_extractor_pipe
setblock 7 56 25 logistics:pipe/stone_transport_pipe
setblock 8 56 25 logistics:pipe/item_filter_pipe

# North branch (z- direction = iron ingots)
setblock 8 56 24 logistics:pipe/stone_transport_pipe
setblock 8 56 23 logistics:pipe/stone_transport_pipe
setblock 8 56 22 minecraft:chest[facing=south]

# South branch (z+ direction = diamonds)
setblock 8 56 26 logistics:pipe/stone_transport_pipe
setblock 8 56 27 logistics:pipe/stone_transport_pipe
setblock 8 56 28 minecraft:chest[facing=north]

# Redstone engine powering the extractor
setblock 6 56 24 logistics:power/redstone_engine[facing=south]
setblock 6 56 23 minecraft:redstone_block

# Label signs (oak_sign rotation=8 faces north, readable as player approaches from south)
setblock 5 57 25 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Source","color":"yellow"}','{"text":"Iron + Diamonds","color":"gray"}','{"text":"","color":"white"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
setblock 8 57 22 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Iron Ingots","color":"yellow"}','{"text":"(configure filter)","color":"gray"}','{"text":"","color":"white"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
setblock 8 57 28 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Diamonds","color":"aqua"}','{"text":"(configure filter)","color":"gray"}','{"text":"","color":"white"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
