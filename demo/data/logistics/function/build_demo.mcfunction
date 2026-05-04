# Logistics Demo World Builder
# Usage: /function logistics:build_demo
#
# Coordinate layout (all demos at y=56, 15-block intervals along z-axis):
#   Demo 1  - Basic Transport:    x=5-10,  z=10
#   Demo 2  - Smart Filtering:    x=5-12,  z=25
#   Demo 3  - Network Logistics:  x=5-13,  z=40
#   Demo 4  - Autocrafting:       x=5-15,  z=55
#   Demo 5a - Kiln:               x=3-9,   z=70
#   Demo 5b - Macerator:          x=3-9,   z=85
#   Demo 6  - Laser Quarry:       x=27-35, z=10-30

# World settings for stable demo environment
gamerule doDaylightCycle false
gamerule doWeatherCycle false
gamerule doMobSpawning false
gamerule keepInventory true
time set 6000
weather clear

# Initialise test scoreboard (safe to re-run)
scoreboard objectives add logistics_tests dummy "Logistics Tests"
scoreboard objectives setdisplay sidebar logistics_tests
scoreboard players reset * logistics_tests

# Clear the full demo corridor so re-runs start clean
fill 0 56 0 50 72 135 minecraft:air replace

# Build each demo area
function logistics:setup/01_basic_transport
function logistics:setup/02_smart_filtering
function logistics:setup/03_network_logistics
function logistics:setup/04_autocrafting
function logistics:setup/05_machines
function logistics:setup/06_quarry

# Spawn point signpost at the origin
setblock 0 56 0 minecraft:chiseled_stone_bricks
setblock 0 57 0 minecraft:chiseled_stone_bricks
setblock 1 57 0 minecraft:oak_wall_sign[facing=east]{front_text:{messages:['{"text":"LOGISTICS","color":"gold","bold":true}','{"text":"DEMO WORLD","color":"white"}','{"text":"/function logistics:","color":"aqua"}','{"text":"test/run_all","color":"aqua"}'],color:"black",has_glowing_text:0b}}

# Move player to the start of the demo road
tp @s 2 57 5

tellraw @a [{"text":"=== Logistics Demo World Built ===","color":"gold","bold":true}]
tellraw @a [{"text":"Walk south (+Z) to explore each demo.","color":"white"}]
tellraw @a [{"text":"Run ","color":"white"},{"text":"/function logistics:test/run_all","color":"aqua"},{"text":" to execute automated tests.","color":"white"}]
