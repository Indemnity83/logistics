package com.logistics.quarry.entity;

import java.util.List;

import com.logistics.block.PipeBlock;
import com.logistics.block.entity.PipeBlockEntity;
import com.logistics.pipe.runtime.PipeConfig;
import com.logistics.pipe.runtime.TravelingItem;
import com.logistics.quarry.QuarryBlock;
import com.logistics.quarry.QuarryBlockEntities;
import com.logistics.quarry.ui.QuarryScreenHandler;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class QuarryBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos>, Inventory, SidedInventory {
    private static final int CHUNK_SIZE = 16;
    private static final int Y_OFFSET_ABOVE = 4;
    private static final int INVENTORY_SIZE = 9;
    private static final int[] TOOL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);

    // Mining state
    private int miningX = 0;
    private int miningY = 0;
    private int miningZ = 0;
    private float breakProgress = 0f;
    private boolean finished = false;

    // Custom bounds from markers
    private boolean useCustomBounds = false;
    private int customMinX = 0;
    private int customMinZ = 0;
    private int customMaxX = 0;
    private int customMaxZ = 0;
    private int customTopY = 0;

    // Cached values for the current mining target
    private BlockPos currentTarget = null;
    private float currentBreakTime = -1f;

    public QuarryBlockEntity(BlockPos pos, BlockState state) {
        super(QuarryBlockEntities.QUARRY_BLOCK_ENTITY, pos, state);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return pos;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.logistics.quarry");
    }

    @Nullable @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new QuarryScreenHandler(syncId, playerInventory, this);
    }

    public static void tick(World world, BlockPos pos, BlockState state, QuarryBlockEntity entity) {
        if (world.isClient() || entity.finished) {
            return;
        }

        ServerWorld serverWorld = (ServerWorld) world;
        int toolSlot = entity.findFirstToolSlot();

        // No tool available = stop
        if (toolSlot < 0) {
            entity.resetBreakProgress();
            return;
        }

        ItemStack tool = entity.getStack(toolSlot);

        // Calculate the target position
        BlockPos target = entity.calculateTargetPos(state);
        if (target == null) {
            entity.finished = true;
            entity.markDirty();
            return;
        }

        BlockState targetState = world.getBlockState(target);

        // Skip air, fluids, and bedrock
        if (entity.shouldSkipBlock(targetState)) {
            entity.advanceToNextBlock();
            entity.resetBreakProgress();
            return;
        }

        // Calculate break time if needed
        if (!target.equals(entity.currentTarget) || entity.currentBreakTime < 0) {
            entity.currentTarget = target;
            entity.currentBreakTime = entity.calculateBreakTime(targetState, tool, serverWorld);
        }

        // Progress the break
        entity.breakProgress += 1f;

        // Check if block is broken
        if (entity.breakProgress >= entity.currentBreakTime) {
            entity.mineBlock(serverWorld, target, targetState, tool, toolSlot);
            entity.advanceToNextBlock();
            entity.resetBreakProgress();
        }
    }

    /**
     * Find the first slot containing a valid tool.
     * @return slot index, or -1 if no tool found
     */
    private int findFirstToolSlot() {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty() && stack.getMaxDamage() > 0) {
                return i;
            }
        }
        return -1;
    }

    private boolean shouldSkipBlock(BlockState state) {
        // Skip air
        if (state.isAir()) {
            return true;
        }
        // Skip fluids (water, lava)
        if (!state.getFluidState().isEmpty()) {
            return true;
        }
        // Skip bedrock
        if (state.isOf(Blocks.BEDROCK)) {
            return true;
        }
        return false;
    }

    private @Nullable BlockPos calculateTargetPos(BlockState quarryState) {
        if (useCustomBounds) {
            return calculateCustomTargetPos();
        }
        return calculateDefaultTargetPos(quarryState);
    }

    private @Nullable BlockPos calculateCustomTargetPos() {
        int areaWidth = customMaxX - customMinX + 1;
        int areaDepth = customMaxZ - customMinZ + 1;

        // Check if we've finished the current layer
        if (miningX >= areaWidth) {
            miningX = 0;
            miningZ++;
        }
        if (miningZ >= areaDepth) {
            miningZ = 0;
            miningY++;
        }

        // Calculate Y range
        int currentY = customTopY - miningY;

        // Check if we've reached bedrock level or below world
        if (currentY < world.getBottomY()) {
            return null;
        }

        int targetX = customMinX + miningX;
        int targetZ = customMinZ + miningZ;

        return new BlockPos(targetX, currentY, targetZ);
    }

    private @Nullable BlockPos calculateDefaultTargetPos(BlockState quarryState) {
        Direction facing = QuarryBlock.getMiningDirection(quarryState);

        // Calculate the starting position of the mining area
        // The area is a 16x16 chunk "behind" the quarry
        BlockPos quarryPos = getPos();

        // Start position is offset by facing direction
        // For NORTH facing quarry, the area is to the north (negative Z)
        int startX;
        int startZ;

        switch (facing) {
            case NORTH:
                startX = quarryPos.getX() - 8;
                startZ = quarryPos.getZ() - CHUNK_SIZE;
                break;
            case SOUTH:
                startX = quarryPos.getX() - 8;
                startZ = quarryPos.getZ() + 1;
                break;
            case EAST:
                startX = quarryPos.getX() + 1;
                startZ = quarryPos.getZ() - 8;
                break;
            case WEST:
                startX = quarryPos.getX() - CHUNK_SIZE;
                startZ = quarryPos.getZ() - 8;
                break;
            default:
                return null;
        }

        // Calculate Y range
        int startY = quarryPos.getY() + Y_OFFSET_ABOVE;
        int currentY = startY - miningY;

        // Check if we've reached bedrock level or below world
        if (currentY < world.getBottomY()) {
            return null;
        }

        int targetX = startX + miningX;
        int targetZ = startZ + miningZ;

        return new BlockPos(targetX, currentY, targetZ);
    }

    private float calculateBreakTime(BlockState targetState, ItemStack tool, ServerWorld world) {
        // Get the block hardness
        float hardness = targetState.getHardness(world, currentTarget);

        // Unbreakable blocks (like bedrock) return -1 hardness
        if (hardness < 0) {
            return Float.MAX_VALUE;
        }

        // Get tool mining speed
        float toolSpeed = tool.getMiningSpeedMultiplier(targetState);

        // Check if this is the correct tool for the block
        boolean correctTool = tool.isSuitableFor(targetState);

        // Get efficiency enchantment level
        int efficiencyLevel = getEnchantmentLevel(tool, Enchantments.EFFICIENCY);

        // Calculate speed multiplier from efficiency
        float speedMultiplier = toolSpeed;
        if (efficiencyLevel > 0 && toolSpeed > 1.0f) {
            speedMultiplier += (efficiencyLevel * efficiencyLevel + 1);
        }

        // Calculate break time
        // Base formula: hardness * 1.5 if correct tool, * 5 otherwise
        // Divide by speed multiplier
        float baseMultiplier = correctTool ? 1.5f : 5.0f;
        float breakTime = (hardness * baseMultiplier * 20f) / speedMultiplier; // 20 ticks per second

        return Math.max(1f, breakTime);
    }

    private int getEnchantmentLevel(ItemStack stack, net.minecraft.registry.RegistryKey<net.minecraft.enchantment.Enchantment> enchantmentKey) {
        if (world == null) return 0;

        var enchantmentRegistry = world.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT);
        var enchantmentEntry = enchantmentRegistry.getOptional(enchantmentKey);

        if (enchantmentEntry.isEmpty()) return 0;

        return EnchantmentHelper.getLevel(enchantmentEntry.get(), stack);
    }

    private void mineBlock(ServerWorld world, BlockPos target, BlockState targetState, ItemStack tool, int toolSlot) {
        // Fast path for simple blocks (no block entity) - use manual drop handling for Fortune/Silk Touch
        if (world.getBlockEntity(target) == null) {
            List<ItemStack> drops = Block.getDroppedStacks(targetState, world, target, null, null, tool);
            world.breakBlock(target, false);
            for (ItemStack drop : drops) {
                outputItem(world, drop);
            }
        } else {
            // Safe path for complex blocks (containers, etc.) - let them drop naturally and collect
            world.breakBlock(target, true);
            collectNearbyItems(world, target);
        }

        // Damage the tool
        damageTool(tool, toolSlot, 1);
    }

    private void collectNearbyItems(ServerWorld world, BlockPos target) {
        List<ItemEntity> itemEntities = world.getEntitiesByClass(
                ItemEntity.class,
                new net.minecraft.util.math.Box(target).expand(2.0),
                item -> true);

        for (ItemEntity itemEntity : itemEntities) {
            ItemStack stack = itemEntity.getStack();
            if (!stack.isEmpty()) {
                outputItem(world, stack.copy());
                itemEntity.discard();
            }
        }
    }

    private void damageTool(ItemStack tool, int slot, int amount) {
        if (tool.isEmpty()) return;

        // Check for Unbreaking enchantment
        int unbreakingLevel = getEnchantmentLevel(tool, Enchantments.UNBREAKING);

        // Unbreaking has a chance to not use durability
        if (unbreakingLevel > 0) {
            // Probability of not consuming durability = unbreaking / (unbreaking + 1)
            // For tools: 100/(level+1)% chance to consume durability
            if (world != null && world.random.nextFloat() < (1.0f / (unbreakingLevel + 1))) {
                tool.setDamage(tool.getDamage() + amount);
            }
        } else {
            tool.setDamage(tool.getDamage() + amount);
        }

        // Check if tool is broken - clear the slot and the quarry will use the next available tool
        if (tool.getDamage() >= tool.getMaxDamage()) {
            setStack(slot, ItemStack.EMPTY);
        }

        markDirty();
    }

    private void outputItem(ServerWorld world, ItemStack stack) {
        if (stack.isEmpty()) return;

        BlockPos quarryPos = getPos();
        BlockPos abovePos = quarryPos.up();

        // Check if there's a pipe above
        BlockState aboveState = world.getBlockState(abovePos);
        if (aboveState.getBlock() instanceof PipeBlock) {
            BlockEntity aboveEntity = world.getBlockEntity(abovePos);
            if (aboveEntity instanceof PipeBlockEntity pipeEntity) {
                // Use forceAddItem to bypass ingress checks and insert directly into pipe
                TravelingItem travelingItem = new TravelingItem(stack.copy(), Direction.UP, PipeConfig.ITEM_MIN_SPEED);
                if (pipeEntity.forceAddItem(travelingItem, Direction.DOWN)) {
                    stack.setCount(0);
                }
            }
        }

        // Check if there's an inventory above (chest, barrel, etc.)
        if (!stack.isEmpty()) {
            BlockEntity aboveEntity = world.getBlockEntity(abovePos);
            if (aboveEntity instanceof Inventory inv) {
                // Check if it's a sided inventory and respects insertion from below
                if (aboveEntity instanceof SidedInventory sidedInv) {
                    int[] availableSlots = sidedInv.getAvailableSlots(Direction.DOWN);
                    for (int slot : availableSlots) {
                        if (stack.isEmpty()) break;
                        if (!sidedInv.canInsert(slot, stack, Direction.DOWN)) continue;
                        stack = insertIntoSlot(inv, slot, stack);
                    }
                } else {
                    // Regular inventory - try all slots
                    for (int slot = 0; slot < inv.size(); slot++) {
                        if (stack.isEmpty()) break;
                        if (!inv.isValid(slot, stack)) continue;
                        stack = insertIntoSlot(inv, slot, stack);
                    }
                }
            }
        }

        // Drop any remaining items
        if (!stack.isEmpty()) {
            double x = quarryPos.getX() + 0.5;
            double y = quarryPos.getY() + 1.5;
            double z = quarryPos.getZ() + 0.5;

            ItemEntity itemEntity = new ItemEntity(world, x, y, z, stack);
            itemEntity.setVelocity(0, 0.2, 0); // Small upward velocity
            world.spawnEntity(itemEntity);
        }
    }

    /**
     * Try to insert a stack into a specific slot of an inventory.
     * @return the remaining stack (may be empty if fully inserted)
     */
    private ItemStack insertIntoSlot(Inventory inv, int slot, ItemStack stack) {
        ItemStack existing = inv.getStack(slot);

        if (existing.isEmpty()) {
            // Empty slot - insert up to max stack size
            int maxInsert = Math.min(stack.getCount(), inv.getMaxCountPerStack());
            inv.setStack(slot, stack.split(maxInsert));
        } else if (ItemStack.areItemsAndComponentsEqual(existing, stack)) {
            // Same item - try to merge
            int space = Math.min(inv.getMaxCountPerStack(), existing.getMaxCount()) - existing.getCount();
            if (space > 0) {
                int toInsert = Math.min(space, stack.getCount());
                existing.increment(toInsert);
                stack.decrement(toInsert);
            }
        }

        return stack;
    }

    private void advanceToNextBlock() {
        miningX++;
        int maxX = useCustomBounds ? (customMaxX - customMinX + 1) : CHUNK_SIZE;
        int maxZ = useCustomBounds ? (customMaxZ - customMinZ + 1) : CHUNK_SIZE;

        if (miningX >= maxX) {
            miningX = 0;
            miningZ++;
            if (miningZ >= maxZ) {
                miningZ = 0;
                miningY++;
            }
        }
        markDirty();
    }

    /**
     * Set custom mining bounds from markers.
     * The top Y is derived from the quarry's position + offset (same as default mining).
     */
    public void setCustomBounds(int minX, int minZ, int maxX, int maxZ) {
        this.useCustomBounds = true;
        this.customMinX = minX;
        this.customMinZ = minZ;
        this.customMaxX = maxX;
        this.customMaxZ = maxZ;
        this.customTopY = pos.getY() + Y_OFFSET_ABOVE;
        this.miningX = 0;
        this.miningY = 0;
        this.miningZ = 0;
        this.finished = false;
        markDirty();
    }

    public boolean hasCustomBounds() {
        return useCustomBounds;
    }

    private void resetBreakProgress() {
        breakProgress = 0f;
        currentTarget = null;
        currentBreakTime = -1f;
    }

    // Inventory implementation
    @Override
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack stack = inventory.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result;
        if (amount >= stack.getCount()) {
            result = stack;
            inventory.set(slot, ItemStack.EMPTY);
        } else {
            result = stack.split(amount);
        }
        markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack stack = inventory.get(slot);
        inventory.set(slot, ItemStack.EMPTY);
        markDirty();
        return stack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        // Only accept items that can damage blocks (tools)
        return stack.getMaxDamage() > 0;
    }

    // SidedInventory implementation - allow hopper interaction
    @Override
    public int[] getAvailableSlots(Direction side) {
        return TOOL_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    // NBT serialization using the new WriteView/ReadView API
    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);

        // Save inventory (all 9 slots)
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty()) {
                view.put("Tool" + i, ItemStack.CODEC, stack);
            } else {
                view.remove("Tool" + i);
            }
        }

        // Save mining state
        NbtCompound miningState = new NbtCompound();
        miningState.putInt("X", miningX);
        miningState.putInt("Y", miningY);
        miningState.putInt("Z", miningZ);
        miningState.putFloat("Progress", breakProgress);
        miningState.putBoolean("Finished", finished);
        view.put("MiningState", NbtCompound.CODEC, miningState);

        // Save custom bounds
        if (useCustomBounds) {
            NbtCompound customBoundsNbt = new NbtCompound();
            customBoundsNbt.putInt("MinX", customMinX);
            customBoundsNbt.putInt("MinZ", customMinZ);
            customBoundsNbt.putInt("MaxX", customMaxX);
            customBoundsNbt.putInt("MaxZ", customMaxZ);
            customBoundsNbt.putInt("TopY", customTopY);
            view.put("CustomBounds", NbtCompound.CODEC, customBoundsNbt);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);

        // Clear inventory
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.set(i, ItemStack.EMPTY);
        }

        // Load inventory (all 9 slots)
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            final int slotIndex = i;
            view.read("Tool" + i, ItemStack.CODEC).ifPresent(stack -> {
                inventory.set(slotIndex, stack);
            });
        }

        // Legacy: load old single-slot format (stored as just "Tool")
        view.read("Tool", ItemStack.CODEC).ifPresent(tool -> {
            if (!tool.isEmpty() && inventory.get(0).isEmpty()) {
                inventory.set(0, tool);
            }
        });

        // Load mining state
        view.read("MiningState", NbtCompound.CODEC).ifPresent(miningState -> {
            miningX = miningState.getInt("X").orElse(0);
            miningY = miningState.getInt("Y").orElse(0);
            miningZ = miningState.getInt("Z").orElse(0);
            breakProgress = miningState.getFloat("Progress").orElse(0f);
            finished = miningState.getBoolean("Finished").orElse(false);
        });

        // Load custom bounds
        view.read("CustomBounds", NbtCompound.CODEC).ifPresent(customBoundsNbt -> {
            useCustomBounds = true;
            customMinX = customBoundsNbt.getInt("MinX").orElse(0);
            customMinZ = customBoundsNbt.getInt("MinZ").orElse(0);
            customMaxX = customBoundsNbt.getInt("MaxX").orElse(0);
            customMaxZ = customBoundsNbt.getInt("MaxZ").orElse(0);
            customTopY = customBoundsNbt.getInt("TopY").orElse(0);
        });
    }

    @Nullable @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    // Handle item drops when block is broken
    @Override
    public void onBlockReplaced(BlockPos pos, BlockState oldState) {
        super.onBlockReplaced(pos, oldState);

        if (world != null && !world.isClient()) {
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                ItemStack stack = inventory.get(i);
                if (!stack.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(
                            world,
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            stack);
                    itemEntity.setToDefaultPickupDelay();
                    world.spawnEntity(itemEntity);
                }
            }
        }
    }

    // Getters for GUI
    public int getMiningX() {
        return miningX;
    }

    public int getMiningY() {
        return miningY;
    }

    public int getMiningZ() {
        return miningZ;
    }

    public boolean isFinished() {
        return finished;
    }

    public float getBreakProgress() {
        return breakProgress;
    }

    public float getCurrentBreakTime() {
        return currentBreakTime;
    }
}
