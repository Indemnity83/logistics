# Demo 4: Autocrafting
#
# Layout (y=56, z=55):
#   x=5  Provider chest (iron ingots + sticks)
#   x=6  Provider Logistics Pipe
#   x=7  Basic Logistics Pipe
#   x=8  Basic Logistics Pipe
#   x=9  Crafting Logistics Pipe
#   x=10 Process Logistics Pipe  - routes in-progress jobs to the crafter
#   x=11 Basic Logistics Pipe
#   x=12 Requester Logistics Pipe
#   x=13 Output chest (receives crafted items)
#
#   x=10, z=56  Vanilla Crafter block (pre-configured with iron sword recipe)
#
# The crafting logistics pipe auto-crafts iron swords on demand.
# Open the Requester pipe GUI and add iron_sword to trigger crafting.

# Signpost
setblock 2 56 53 minecraft:chiseled_stone_bricks
setblock 2 57 53 minecraft:chiseled_stone_bricks
setblock 3 57 53 minecraft:oak_wall_sign[facing=east]{front_text:{messages:['{"text":"Demo 4","color":"gold","bold":true}','{"text":"Autocrafting","color":"white"}','{"text":"Crafts iron swords","color":"gray"}','{"text":"on demand","color":"gray"}'],color:"black",has_glowing_text:0b}}

# Provider chest with raw materials (iron ingots + sticks for iron sword)
setblock 5 56 55 minecraft:chest[facing=east]
item replace block 5 56 55 container.0 with minecraft:iron_ingot 64
item replace block 5 56 55 container.1 with minecraft:iron_ingot 64
item replace block 5 56 55 container.2 with minecraft:stick 64

# Logistics network
setblock 6 56 55 logistics:pipe/provider_logistics_pipe
setblock 7 56 55 logistics:pipe/basic_logistics_pipe
setblock 8 56 55 logistics:pipe/basic_logistics_pipe
setblock 9 56 55 logistics:pipe/crafting_logistics_pipe
setblock 10 56 55 logistics:pipe/process_logistics_pipe
setblock 11 56 55 logistics:pipe/basic_logistics_pipe
setblock 12 56 55 logistics:pipe/requester_logistics_pipe

# Output chest
setblock 13 56 55 minecraft:chest[facing=west]

# Vanilla Crafter block adjacent to process pipe, pre-configured with iron sword recipe
# Iron sword: [_,I,_], [_,I,_], [_,S,_]  where I=iron_ingot (slots 1,4), S=stick (slot 7)
setblock 10 56 56 minecraft:crafter[facing=north,triggered=false]{Items:[{Slot:1b,id:"minecraft:iron_ingot",count:1},{Slot:4b,id:"minecraft:iron_ingot",count:1},{Slot:7b,id:"minecraft:stick",count:1}]}

# Label signs (oak_sign rotation=8 faces north, readable as player approaches from south)
setblock 5 57 55 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Provider","color":"green"}','{"text":"Iron + Sticks","color":"gray"}','{"text":"","color":"white"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
setblock 9 57 55 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Crafting Pipe","color":"yellow"}','{"text":"","color":"white"}','{"text":"","color":"white"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
setblock 10 57 56 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Crafter","color":"yellow"}','{"text":"Iron Sword recipe","color":"gray"}','{"text":"pre-configured","color":"gray"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
setblock 12 57 55 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Requester","color":"yellow"}','{"text":"Request iron_sword","color":"gray"}','{"text":"to trigger crafting","color":"gray"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
