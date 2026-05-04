# Test 5a check: Kiln
# Runs 600 ticks after /function logistics:test/run_all
# Passes if the kiln has smelted items and piped output to chest at (9, 56, 70)
#
# Chain: Input Chest → Hopper → Kiln (RF from Creative Engine) → Extractor Pipe → Output Chest

execute if data block 9 56 70 Items[0] \
    run scoreboard players set "5a Kiln" logistics_tests 1
execute unless data block 9 56 70 Items[0] \
    run scoreboard players set "5a Kiln" logistics_tests 0

execute if score "5a Kiln" logistics_tests matches 1 \
    run tellraw @a [{"text":"[PASS] ","color":"green","bold":true},{"text":"Kiln: smelted items arrived in output chest","color":"green"}]
execute if score "5a Kiln" logistics_tests matches 0 \
    run tellraw @a [{"text":"[FAIL] ","color":"red","bold":true},{"text":"Kiln: no items in output chest after 30s","color":"red"}]
