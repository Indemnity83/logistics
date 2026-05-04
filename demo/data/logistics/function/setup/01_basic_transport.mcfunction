# Demo 1: Basic Transport
#
# Layout (y=56, z=10):
#   x=5  Chest A (source, pre-filled with iron ingots)
#   x=6  Item Extractor Pipe
#   x=7  Stone Transport Pipe
#   x=8  Stone Transport Pipe
#   x=9  Stone Transport Pipe
#   x=10 Chest B (destination, empty)
#
#   x=6, z=9  Redstone Engine (facing south, toward pipe at z=10)
#   x=6, z=8  Redstone Block (keeps engine powered)

# Signpost
setblock 2 56 8 minecraft:chiseled_stone_bricks
setblock 2 57 8 minecraft:chiseled_stone_bricks
setblock 3 57 8 minecraft:oak_wall_sign[facing=east]{front_text:{messages:['{"text":"Demo 1","color":"gold","bold":true}','{"text":"Basic Transport","color":"white"}','{"text":"Iron ingots flow","color":"gray"}','{"text":"Chest A to Chest B","color":"gray"}'],color:"black",has_glowing_text:0b}}

# Source chest (Chest A) - 3 stacks of iron ingots
setblock 5 56 10 minecraft:chest[facing=east]
item replace block 5 56 10 container.0 with minecraft:iron_ingot 64
item replace block 5 56 10 container.1 with minecraft:iron_ingot 64
item replace block 5 56 10 container.2 with minecraft:iron_ingot 64

# Pipe network
setblock 6 56 10 logistics:pipe/item_extractor_pipe
setblock 7 56 10 logistics:pipe/stone_transport_pipe
setblock 8 56 10 logistics:pipe/stone_transport_pipe
setblock 9 56 10 logistics:pipe/stone_transport_pipe

# Destination chest (Chest B) - empty, receives transported items
setblock 10 56 10 minecraft:chest[facing=west]

# Redstone Engine - powers the extractor pipe
setblock 6 56 9 logistics:power/redstone_engine[facing=south]
# Redstone block provides a constant signal to the engine
setblock 6 56 8 minecraft:redstone_block

# Label signs above chests (oak_sign rotation=8 faces north, readable as player approaches from south)
setblock 5 57 10 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Chest A","color":"yellow"}','{"text":"(source)","color":"gray"}','{"text":"","color":"white"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
setblock 10 57 10 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Chest B","color":"yellow"}','{"text":"(destination)","color":"gray"}','{"text":"","color":"white"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
