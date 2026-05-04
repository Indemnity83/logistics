# Test 1 check: Basic Transport
# Runs 200 ticks after /function logistics:test/run_all
# Passes if any iron ingots have arrived in Chest B at (10, 64, 10)

execute if data block 10 56 10 Items[{id:"minecraft:iron_ingot"}] \
    run scoreboard players set "1 Basic Transport" logistics_tests 1
execute unless data block 10 56 10 Items[{id:"minecraft:iron_ingot"}] \
    run scoreboard players set "1 Basic Transport" logistics_tests 0

execute if score "1 Basic Transport" logistics_tests matches 1 \
    run tellraw @a [{"text":"[PASS] ","color":"green","bold":true},{"text":"Basic Transport: iron ingots reached Chest B","color":"green"}]
execute if score "1 Basic Transport" logistics_tests matches 0 \
    run tellraw @a [{"text":"[FAIL] ","color":"red","bold":true},{"text":"Basic Transport: no items in Chest B after 10s","color":"red"}]
