package com.logistics.forestry.farm.orchard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.logistics.core.lib.resource.ResourceId;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One fruit the Orchard picks, read from {@code data/<namespace>/logistics/orchard/fruit/<name>.json}:
 *
 * <pre>{@code
 * { "leaves": "#logistics:forestry/orchard/apple_leaves", "fruit": "minecraft:apple", "chance": 0.02 }
 * }</pre>
 *
 * <p>{@code leaves} is a block tag ({@code #namespace:path}) or one block id: the leaves that bear
 * the fruit. {@code chance} is the chance, in (0, 1], that one simulated random tick of a tree with
 * such leaves picks one {@code fruit}. The file carries no rate; the rate comes from random ticks.
 */
public record OrchardFruit(ResourceId id, String leaves, Predicate<BlockState> bears, Item fruit, double chance) {

    /** Where fruit definitions live under each namespace's data folder. */
    public static final String DIRECTORY = "logistics/orchard/fruit";

    public OrchardFruit {
        if (!(chance > 0 && chance <= 1)) {
            throw new IllegalArgumentException("chance must be in (0, 1]: " + chance);
        }
    }

    /** Whether {@code state}'s leaves bear this fruit. */
    public boolean bears(BlockState state) {
        return bears.test(state);
    }

    /** One of this fruit. */
    public ItemStack stack() {
        return new ItemStack(fruit);
    }

    /**
     * Parses the definition {@code id} from {@code json}.
     *
     * @throws IllegalArgumentException when a field is missing or invalid, or names an unknown item or block
     */
    public static OrchardFruit parse(ResourceId id, JsonElement json) {
        if (!json.isJsonObject()) {
            throw new IllegalArgumentException("expected an object");
        }
        JsonObject object = json.getAsJsonObject();
        String leaves = string(object, "leaves");
        ResourceId fruitId = parseId(string(object, "fruit"));
        Item fruit = BuiltInRegistries.ITEM
                .getOptional(fruitId.toIdentifier())
                .orElseThrow(() -> new IllegalArgumentException("unknown item " + fruitId));
        if (!object.has("chance") || !object.get("chance").isJsonPrimitive()) {
            throw new IllegalArgumentException("missing number 'chance'");
        }
        double chance;
        try {
            chance = object.get("chance").getAsDouble();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'chance' must be a number");
        }
        return new OrchardFruit(id, leaves, matcher(leaves), fruit, chance);
    }

    /** What {@code leaves} names: a block tag ({@code #namespace:path}) or one block. */
    static Predicate<BlockState> matcher(String leaves) {
        if (leaves.startsWith("#")) {
            TagKey<Block> tag = TagKey.create(Registries.BLOCK, parseId(leaves.substring(1)).toIdentifier());
            return state -> state.is(tag);
        }
        ResourceId blockId = parseId(leaves);
        Block block = BuiltInRegistries.BLOCK
                .getOptional(blockId.toIdentifier())
                .orElseThrow(() -> new IllegalArgumentException("unknown block " + blockId));
        return state -> state.is(block);
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("missing string '" + key + "'");
        }
        return value.getAsString();
    }

    private static ResourceId parseId(String id) {
        ResourceId parsed = ResourceId.tryParse(id);
        if (parsed == null) {
            throw new IllegalArgumentException("invalid id '" + id + "'");
        }
        return parsed;
    }
}
