# Demo 3: Network Logistics
#
# Layout (y=56, z=40):
#   x=5  Storage chest (pre-filled with iron ingots)
#   x=6  Provider Logistics Pipe  - advertises chest contents to the network
#   x=7  Basic Logistics Pipe     - network backbone
#   x=8  Basic Logistics Pipe
#   x=9  Basic Logistics Pipe
#   x=10 Basic Logistics Pipe
#   x=11 Requester Logistics Pipe - requests items from the network
#   x=12 Request chest (destination, receives requested items)
#
# NOTE: Open the Requester Logistics Pipe GUI and add iron_ingot to request.

# Signpost
setblock 2 56 38 minecraft:chiseled_stone_bricks
setblock 2 57 38 minecraft:chiseled_stone_bricks
setblock 3 57 38 minecraft:oak_wall_sign[facing=east]{front_text:{messages:['{"text":"Demo 3","color":"gold","bold":true}','{"text":"Network Logistics","color":"white"}','{"text":"Provider to Requester","color":"gray"}','{"text":"via pipe network","color":"gray"}'],color:"black",has_glowing_text:0b}}

# Storage chest with items to provide
setblock 5 56 40 minecraft:chest[facing=east]
item replace block 5 56 40 container.0 with minecraft:iron_ingot 64
item replace block 5 56 40 container.1 with minecraft:gold_ingot 64
item replace block 5 56 40 container.2 with minecraft:diamond 16
item replace block 5 56 40 container.3 with minecraft:emerald 16

# Logistics network
setblock 6 56 40 logistics:pipe/provider_logistics_pipe
setblock 7 56 40 logistics:pipe/basic_logistics_pipe
setblock 8 56 40 logistics:pipe/basic_logistics_pipe
setblock 9 56 40 logistics:pipe/basic_logistics_pipe
setblock 10 56 40 logistics:pipe/basic_logistics_pipe
setblock 11 56 40 logistics:pipe/requester_logistics_pipe

# Destination chest (receives requested items)
setblock 12 56 40 minecraft:chest[facing=west]

# Label signs (oak_sign rotation=8 faces north, readable as player approaches from south)
setblock 5 57 40 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Storage","color":"yellow"}','{"text":"(Provider)","color":"green"}','{"text":"","color":"white"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
setblock 11 57 40 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Requester","color":"yellow"}','{"text":"Open GUI to","color":"gray"}','{"text":"add request","color":"gray"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
setblock 12 57 40 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Destination","color":"yellow"}','{"text":"(receives items)","color":"gray"}','{"text":"","color":"white"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
