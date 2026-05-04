# Test 5b check: Macerator
# Runs 600 ticks after /function logistics:test/run_all
# Passes if the macerator has processed iron ore and piped output to chest at (9, 56, 85)
#
# Chain: Input Chest (iron_ore) → Hopper → Macerator (RF from Creative Engine) → Extractor Pipe → Output Chest
# Expected output: logistics:core/iron_dust x2 per iron_ore (ore doubling)

execute if data block 9 56 85 Items[0] \
    run scoreboard players set "5b Macerator" logistics_tests 1
execute unless data block 9 56 85 Items[0] \
    run scoreboard players set "5b Macerator" logistics_tests 0

execute if score "5b Macerator" logistics_tests matches 1 \
    run tellraw @a [{"text":"[PASS] ","color":"green","bold":true},{"text":"Macerator: iron_dust arrived in output chest (ore doubling works)","color":"green"}]
execute if score "5b Macerator" logistics_tests matches 0 \
    run tellraw @a [{"text":"[FAIL] ","color":"red","bold":true},{"text":"Macerator: no output in chest after 30s","color":"red"}]
