package com.logistics.pipe.ui;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.ProcessModule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Ghost inventory for the Process Logistics Pipe GUI.
 * Slots 0-3: input items (up to 4). Slots 4-5: output items (up to 2).
 */
public class ProcessInventory extends SimpleContainer {
    public static final int INPUT_SLOTS = ProcessModule.MAX_INPUTS;
    public static final int OUTPUT_SLOTS = ProcessModule.MAX_OUTPUTS;
    public static final int TOTAL_SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;

    @Nullable private final PipeBlockEntity pipeEntity;
    @Nullable private final String targetModuleStateKey;

    public ProcessInventory(@Nullable PipeBlockEntity pipeEntity) {
        this(pipeEntity, null);
    }

    public ProcessInventory(@Nullable PipeBlockEntity pipeEntity, @Nullable String targetModuleStateKey) {
        super(TOTAL_SLOTS);
        this.pipeEntity = pipeEntity;
        this.targetModuleStateKey = targetModuleStateKey;
        if (pipeEntity != null) {
            loadFromModule();
        }
    }

    /** Refresh display slots from the module's current NBT state. */
    public void loadFromModule() {
        if (pipeEntity == null) return;
        PipeModuleHelper.withModule(pipeEntity, ProcessModule.class, targetModuleStateKey, (ctx, module) -> {
            for (int i = 0; i < INPUT_SLOTS; i++) {
                setItem(i, resolveDisplayStack(module.getInputItem(ctx, i), module.getInputCount(ctx, i)));
            }
            for (int i = 0; i < OUTPUT_SLOTS; i++) {
                setItem(INPUT_SLOTS + i,
                        resolveDisplayStack(module.getOutputItem(ctx, i), module.getOutputCount(ctx, i)));
            }
        });
    }

    private static ItemStack resolveDisplayStack(String itemId, int count) {
        if (itemId == null || itemId.isEmpty()) return ItemStack.EMPTY;
        ResourceId rid = ResourceId.tryParse(itemId);
        if (rid == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(rid.toIdentifier());
        if (item == null || item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item, Math.max(1, count));
    }
}
