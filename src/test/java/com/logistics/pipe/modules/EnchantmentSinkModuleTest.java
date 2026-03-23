package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EnchantmentSinkModule")
class EnchantmentSinkModuleTest extends MinecraftTestEnvironment {

    private EnchantmentSinkModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new EnchantmentSinkModule(2);
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== matchesItem ====================

    @Test
    @DisplayName("matchesItem returns false for a plain (non-enchanted) item")
    void matchesItem_plainItem_returnsFalse() {
        ItemStack plain = new ItemStack(Items.DIAMOND);
        assertThat(module.matchesItem(plain)).isFalse();
    }

    @Test
    @DisplayName("matchesItem returns false for an enchanted book with no enchantments")
    void matchesItem_enchantedBookEmpty_returnsFalse() {
        // Enchanted book with no stored enchantments
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        assertThat(module.matchesItem(book)).isFalse();
    }

    @Test
    @DisplayName("matchesItem returns false for an empty item stack")
    void matchesItem_emptyStack_returnsFalse() {
        assertThat(module.matchesItem(ItemStack.EMPTY)).isFalse();
    }

    @Test
    @DisplayName("matchesItem returns false for a sword with no enchantments applied")
    void matchesItem_swordWithNoEnchantments_returnsFalse() {
        ItemStack enchanted = new ItemStack(Items.DIAMOND_SWORD);
        // We can't add real enchantments in unit tests without full registry lookup.
        // The positive case (has enchantments → true) is covered in integration/game tests.
        assertThat(module.matchesItem(enchanted)).isFalse(); // no enchantments yet
    }

    @Test
    @DisplayName("sword with no enchantments has null or empty ENCHANTMENTS component (precondition)")
    void swordWithNoEnchantments_hasEmptyOrNullComponent() {
        // Confirms the precondition: a freshly-constructed sword has no enchantments,
        // so the matchesItem_swordWithNoEnchantments_returnsFalse test is meaningful.
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        var noEnchants = sword.get(DataComponents.ENCHANTMENTS);
        assertThat(noEnchants == null || noEnchants.isEmpty()).isTrue();
    }

    // ==================== acceptsItem (delegates to matchesItem) ====================

    @Test
    @DisplayName("acceptsItem returns false for a plain item")
    void acceptsItem_plainItem_returnsFalse() {
        assertThat(module.acceptsItem(ctx, new ItemStack(Items.IRON_INGOT))).isFalse();
    }
}
