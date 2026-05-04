# Run all automated tests
# Usage: /function logistics:test/run_all
#
# Automated tests (pass/fail verified by command blocks):
#   Test 1  - Basic Transport: iron ingots arrive in Chest B within 10s
#   Test 5a - Kiln:            smelted items arrive in output chest within 30s
#   Test 5b - Macerator:       iron_dust arrives in output chest within 30s
#
# Semi-automated (require manual GUI setup first):
#   Demo 2 - Smart Filtering   (configure filter pipe, then observe routing)
#   Demo 3 - Network Logistics (configure requester pipe, then observe delivery)
#   Demo 4 - Autocrafting      (configure requester pipe, then observe crafting)
#   Demo 6 - Laser Quarry      (observe quarry mining and output over time)

# Reset all scores from previous runs
scoreboard players reset * logistics_tests

# --- Test 1: Basic Transport ---
# Clear destination, refill source, then check after 200 ticks (10s)
data modify block 10 56 10 Items set value []
item replace block 5 56 10 container.0 with minecraft:iron_ingot 64
item replace block 5 56 10 container.1 with minecraft:iron_ingot 64
item replace block 5 56 10 container.2 with minecraft:iron_ingot 64
schedule function logistics:test/check/01_basic_transport 200t

# --- Test 5a: Kiln ---
# Clear output chest, refill input chest, check after 600 ticks (30s)
data modify block 9 56 70 Items set value []
item replace block 5 59 70 container.0 with minecraft:raw_iron 32
item replace block 5 59 70 container.1 with minecraft:sand 32
schedule function logistics:test/check/05_machines 600t

# --- Test 5b: Macerator ---
# Clear output chest, refill input chest, check after 600 ticks (30s)
data modify block 9 56 85 Items set value []
item replace block 5 59 85 container.0 with minecraft:iron_ore 32
schedule function logistics:test/check/05_macerator 600t

tellraw @a [{"text":"Tests started.","color":"yellow"},{"text":" Results in ~10s (transport) and ~30s (machines).","color":"gray"}]
tellraw @a [{"text":"Watch the sidebar scoreboard for live pass/fail.","color":"gray"}]
