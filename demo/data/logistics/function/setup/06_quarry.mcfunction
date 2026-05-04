# Demo 6: Laser Quarry
#
# Layout:
#   x=30, y=56, z=15  Creative Engine (facing east, provides unlimited RF)
#   x=31, y=56, z=15  Laser Quarry (facing south, mines 16x16 area below)
#
#   Pipe exits from the TOP of the quarry and runs east to a chest:
#   x=31, y=57, z=15  Stone Transport Pipe (on top of quarry)
#   x=32, y=57, z=15  Stone Transport Pipe
#   x=33, y=57, z=15  Stone Transport Pipe
#   x=34, y=57, z=15  Stone Transport Pipe
#   x=35, y=57, z=15  Output chest (on stone pedestal)
#   x=35, y=56, z=15  Stone Bricks (pedestal under chest)
#
# The quarry mines the default 16x16 area below its position (sandstone in this flat world).
# The Creative Engine provides infinite RF so the quarry runs at full speed.
#
# NOTE: The quarry builds a frame first (BUILDING_FRAME phase), then mines (MINING phase).
# Watch the laser arm move as it mines. Output chest fills with sandstone over time.

# Signpost
setblock 27 56 13 minecraft:chiseled_stone_bricks
setblock 27 57 13 minecraft:chiseled_stone_bricks
setblock 28 57 13 minecraft:oak_wall_sign[facing=east]{front_text:{messages:['{"text":"Demo 6","color":"gold","bold":true}','{"text":"Laser Quarry","color":"white"}','{"text":"16x16 auto-mining","color":"gray"}','{"text":"with Creative Engine","color":"gray"}'],color:"black",has_glowing_text:0b}}

# Creative engine for unlimited power
setblock 30 56 15 logistics:power/creative_engine[facing=east]

# Laser quarry
setblock 31 56 15 logistics:automation/laser_quarry[facing=south]

# Pipe on top of quarry, running east to the output chest
setblock 31 57 15 logistics:pipe/stone_transport_pipe
setblock 32 57 15 logistics:pipe/stone_transport_pipe
setblock 33 57 15 logistics:pipe/stone_transport_pipe
setblock 34 57 15 logistics:pipe/stone_transport_pipe

# Output chest on a stone pedestal (chest at pipe level y=57)
setblock 35 56 15 minecraft:stone_bricks
setblock 35 57 15 minecraft:chest[facing=west]

# Labels (oak_sign rotation=8 faces north, readable as player approaches from south)
setblock 30 57 15 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Creative Engine","color":"yellow"}','{"text":"Infinite RF","color":"green"}','{"text":"","color":"white"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
setblock 31 56 14 minecraft:stone_bricks
setblock 31 57 14 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Laser Quarry","color":"yellow"}','{"text":"Mining below...","color":"gray"}','{"text":"","color":"white"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
setblock 35 58 15 minecraft:oak_sign[rotation=8]{front_text:{messages:['{"text":"Output Chest","color":"yellow"}','{"text":"Fills with mined","color":"gray"}','{"text":"blocks over time","color":"gray"}','{"text":"","color":"white"}'],color:"black",has_glowing_text:0b}}
