package com.logistics.automation.laserquarry.entity;

import com.logistics.LogisticsAutomation;
import com.logistics.api.LogisticsApi;
import com.logistics.api.TransportApi;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.block.behavior.ProbeResult;
import java.util.List;
import com.logistics.core.lib.power.EnergyDemandProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.logistics.core.lib.energy.IEnergyStorage;

public class LaserQuarryBlockEntity extends BaseBlockEntity implements PipeConnection, HasEnergyStorage, EnergyDemandProvider {
    private static final long FRAME_BUILD_COST = 240L;
    private static final long MOVE_COST_BUFFER_DIVISOR = 10L;

    /**
     * Quarry operation phases.
     */
    public enum Phase {
        CLEARING, // Clearing the area above and at quarry level
        BUILDING_FRAME, // Building the frame around the quarry area
        MINING // Mining below quarry level
    }

    /**
     * Arm movement sub-states during mining phase.
     */
    public enum ArmState {
        MOVING, // Arm is moving to target position
        SETTLING, // Arm reached target, waiting for client to catch up
        BREAKING // Arm is at target, breaking the block
    }

    // Energy storage
    private final EnergyComponent energy = new EnergyComponent(
            LogisticsConfig.get().quarry.energyCapacity(),
            LogisticsConfig.get().quarry.maxEnergyInput(),
            0,
            this::setChanged
    );
    private long lastSyncedEnergy = 0; // For client sync
    private long energyReceivedLastTick = 0;
    private long energyReceivedThisTick = 0;
    private final IEnergyStorage trackingStorage = new IEnergyStorage() {
        @Override
        public long insert(long maxAmount, boolean simulate) {
            long inserted = energy.insert(maxAmount, simulate);
            if (!simulate && inserted > 0) energyReceivedThisTick += inserted;
            return inserted;
        }

        @Override
        public long extract(long maxAmount, boolean simulate) { return energy.extract(maxAmount, simulate); }

        @Override
        public long getAmount() { return energy.getAmount(); }

        @Override
        public long getCapacity() { return energy.getCapacity(); }

        @Override
        public boolean canInsert() { return energy.canInsert(); }

        @Override
        public boolean canExtract() { return energy.canExtract(); }
    };
    private boolean consumedEnergyThisTick = false; // For LED when buffer is low

    // Phase state
    private Phase currentPhase = Phase.CLEARING;
    private int frameBuildIndex = 0;

    // Mining state
    private int miningX = 0;
    private int miningY = 0;
    private int miningZ = 0;
    private float breakProgress = 0f;
    private boolean finished = false;

    // Custom bounds from markers
    private final QuarryBounds bounds = new QuarryBounds();

    // Cached values for the current mining target
    private BlockPos currentTarget = null;
    private float currentBreakTime = -1f;

    // Block breaking animation entity ID (use position hash for uniqueness)
    private int breakingEntityId = -1;

    // Arm position tracking for smooth movement
    private ArmState armState = ArmState.MOVING;
    private float armX = 0f; // Current arm X position (absolute world coords)
    private float armY = 0f; // Current arm Y position (absolute world coords)
    private float armZ = 0f; // Current arm Z position (absolute world coords)
    private boolean armInitialized = false; // Whether arm position has been set
    private int settlingTicksRemaining = 0; // Countdown for SETTLING state
    private int expectedTravelTicks = 0; // Expected ticks to reach target (for settling calculation)
    private float syncedArmSpeed = 0.0f; // Pre-sync default; overwritten each tick by getEffectiveArmSpeed()

    public LaserQuarryBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsAutomation.ENTITY.LASER_QUARRY_BLOCK_ENTITY, pos, state);
        // Use position hash as unique entity ID for breaking animation
        this.breakingEntityId = pos.hashCode();
    }

    // ==================== HasEnergyStorage ====================

    @Override
    public IEnergyStorage energyStorage(@Nullable Direction side) {
        // Quarry accepts energy from all sides
        return trackingStorage;
    }

    public static void tick(Level world, BlockPos pos, BlockState state, LaserQuarryBlockEntity entity) {
        if (world.isClientSide()) {
            return;
        }

        ActiveQuarryRegistry.register((ServerLevel) world, pos);

        entity.energyReceivedLastTick = entity.energyReceivedThisTick;
        entity.energyReceivedThisTick = 0;

        if (entity.finished) {
            return;
        }

        ServerLevel serverWorld = (ServerLevel) world;

        // Track previous state for sync detection
        boolean wasConsumedEnergy = entity.consumedEnergyThisTick;

        // Reset consumption flag at start of tick - will be set by consumeEnergy()
        entity.consumedEnergyThisTick = false;

        switch (entity.currentPhase) {
            case CLEARING -> tickClearing(serverWorld, pos, state, entity);
            case BUILDING_FRAME -> tickBuildingFrame(serverWorld, pos, state, entity);
            case MINING -> tickMining(serverWorld, pos, state, entity);
            default -> {}
        }

        // Idle power consumption: 1 RF every 4 ticks (5 RF/second) to slowly drain buffer
        if (world.getGameTime() % 4 == 0 && entity.energy.getAmount() > 0) {
            entity.energy.setAmount(Math.max(0, entity.energy.getAmount() - 1));
            entity.setChanged(); // Mark chunk dirty for persistence
        }

        // Sync when energy OR consumption flag changes
        boolean needsSync = entity.energy.getAmount() != entity.lastSyncedEnergy
                || entity.consumedEnergyThisTick != wasConsumedEnergy;

        if (needsSync) {
            entity.lastSyncedEnergy = entity.energy.getAmount();
            entity.syncedArmSpeed = entity.getEffectiveArmSpeed();
            entity.syncToClients();
        }
    }

    /**
     * Sync arm state to clients. Called on arm state transitions.
     *
     * Note: Does not call setChanged() - chunk dirty is managed separately by tick(),
     * which marks dirty when energy is consumed or mining advances. This avoids
     * redundant chunk dirty marks on every arm position update (per-tick).
     */
    private void syncToClients() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    private static void tickClearing(ServerLevel world, BlockPos pos, BlockState state, LaserQuarryBlockEntity entity) {
        // Need at least some energy to operate
        if (entity.energy.getAmount() == 0) {
            entity.resetBreakProgress();
            return;
        }

        // Skip quickly through blocks at or above quarry level
        BlockPos target = null;
        BlockState targetState = null;
        for (int skipped = 0; skipped < LogisticsConfig.get().quarry.scanRate; skipped++) {
            target = entity.calculateClearingTargetPos(state);
            if (target == null) {
                // Finished clearing, move to building phase
                entity.currentPhase = Phase.BUILDING_FRAME;
                entity.frameBuildIndex = 0;
                entity.setChanged();
                return;
            }

            targetState = world.getBlockState(target);

            if (!entity.shouldSkipBlock(world, target, targetState)) {
                break;
            }

            entity.advanceToNextBlock();
            entity.resetBreakProgress();
        }

        if (entity.shouldSkipBlock(world, target, targetState)) {
            return;
        }

        // Calculate energy required if target changed
        if (!target.equals(entity.currentTarget) || entity.currentBreakTime < 0) {
            entity.currentTarget = target;
            float hardness = targetState.getDestroySpeed(world, target);
            entity.currentBreakTime = (float) (LogisticsConfig.get().quarry.energyPerBlockMultiplier() * (hardness + 1));
            entity.breakProgress = 0;
        }

        // Consume as much energy as possible towards breaking (like BC)
        long energyNeeded = (long) Math.ceil(entity.currentBreakTime - entity.breakProgress);
        long energyToUse = Math.min(entity.energy.getAmount(), energyNeeded);
        if (energyToUse > 0) {
            entity.consumeEnergy(energyToUse);
            entity.breakProgress += energyToUse;
        }

        if (entity.breakProgress >= entity.currentBreakTime) {
            entity.mineBlock(world, target, targetState);
            entity.advanceToNextBlock();
            entity.resetBreakProgress();
        }
    }

    private static void tickBuildingFrame(
            ServerLevel world, BlockPos pos, BlockState state, LaserQuarryBlockEntity entity) {
        // Check for energy before building
        if (!entity.hasEnergy(FRAME_BUILD_COST)) {
            return;
        }

        // Build one frame block per tick
        BlockPos framePos = entity.getNextFramePosition(state);
        if (framePos == null) {
            // Finished building frame, move to mining phase
            entity.currentPhase = Phase.MINING;
            entity.miningX = 0;
            entity.miningY = 0;
            entity.miningZ = 0;
            entity.armInitialized = false; // Will be initialized on first mining tick
            entity.armState = ArmState.MOVING;
            entity.syncToClients();
            entity.setChanged();
            return;
        }

        // Consume energy for frame building
        entity.consumeEnergy(FRAME_BUILD_COST);

        // Only place frame if the position is air or replaceable
        BlockState existingState = world.getBlockState(framePos);
        if (existingState.isAir() || existingState.canBeReplaced()) {
            BlockState frameState = entity.calculateFrameState(state, framePos);
            world.setBlockAndUpdate(framePos, frameState);
        }

        entity.frameBuildIndex++;
        entity.setChanged();
    }

    private static void tickMining(ServerLevel world, BlockPos pos, BlockState state, LaserQuarryBlockEntity entity) {
        // Energy is consumed per-state:
        // - MOVING: move cost per tick
        // - SETTLING: no cost (waiting for client sync)
        // - BREAKING: break cost once when block breaks

        // Skip air/fluid/bedrock blocks without moving the arm there
        BlockPos target = null;
        boolean skippedAny = false;
        for (int skipped = 0; skipped < LogisticsConfig.get().quarry.scanRate; skipped++) {
            target = entity.calculateMiningTargetPos(state);
            if (target == null) {
                entity.clearBreakingAnimation(world);
                entity.finished = true;
                entity.setChanged();
                entity.syncToClients();
                return;
            }

            BlockState targetState = world.getBlockState(target);
            if (!entity.shouldSkipBlock(world, target, targetState)) {
                break; // Found a block to mine
            }

            entity.advanceMiningPosition();
            target = null; // Mark as skipped
            skippedAny = true;
        }

        if (target == null) {
            // All blocks this tick were skippable, continue next tick
            return;
        }

        // If we skipped blocks, sync so client knows about the new target
        if (skippedAny) {
            entity.syncToClients();
        }

        // Target position (center of block, just above it for the drill tip)
        float targetX = target.getX() + 0.5f;
        float targetY = target.getY() + 1.0f;
        float targetZ = target.getZ() + 0.5f;

        // Initialize arm position if needed
        if (!entity.armInitialized) {
            entity.armX = targetX;
            entity.armY = targetY;
            entity.armZ = targetZ;
            entity.armInitialized = true;
            entity.armState = ArmState.MOVING;
            entity.expectedTravelTicks = 0; // Already at target
            entity.syncToClients();
        }

        if (entity.armState == ArmState.MOVING) {
            // Consume move cost while arm is moving
            long moveCost = entity.getMoveCost();
            if (!entity.hasEnergy(moveCost)) {
                return; // Wait for energy
            }
            entity.consumeEnergy(moveCost);

            // Calculate expected travel time on first tick of movement (for settling)
            if (entity.expectedTravelTicks == 0 && !entity.isAtTarget(targetX, targetY, targetZ)) {
                entity.expectedTravelTicks = entity.calculateTravelTicks(targetX, targetY, targetZ);
            }

            // Move arm towards target
            boolean reachedTarget = entity.moveArmTowards(targetX, targetY, targetZ);

            if (reachedTarget) {
                // Start settling - wait for client interpolation to catch up
                // Use the pre-calculated travel time so client has enough time to animate
                entity.armState = ArmState.SETTLING;
                entity.settlingTicksRemaining = Math.max(1, entity.expectedTravelTicks);
                entity.expectedTravelTicks = 0; // Reset for next movement
                entity.syncToClients();
                entity.currentTarget = target;
            }
        } else if (entity.armState == ArmState.SETTLING) {
            // Wait for client interpolation to catch up
            entity.settlingTicksRemaining--;
            if (entity.settlingTicksRemaining <= 0) {
                // Start breaking - energy requirement will be calculated in BREAKING state
                entity.armState = ArmState.BREAKING;
            }
        } else if (entity.armState == ArmState.BREAKING) {
            BlockState targetState = world.getBlockState(target);

            // Calculate energy required if target changed
            if (!target.equals(entity.currentTarget) || entity.currentBreakTime < 0) {
                entity.currentTarget = target;
                float hardness = targetState.getDestroySpeed(world, target);
                // BC formula: BREAK_ENERGY * miningMultiplier * ((hardness + 1) * 2)
                entity.currentBreakTime = (float) (LogisticsConfig.get().quarry.energyPerBlockMultiplier() * (hardness + 1));
                entity.breakProgress = 0;
            }

            // Consume as much energy as possible towards breaking (like BC)
            long energyNeeded = (long) Math.ceil(entity.currentBreakTime - entity.breakProgress);
            long energyToUse = Math.min(entity.energy.getAmount(), energyNeeded);
            if (energyToUse > 0) {
                entity.consumeEnergy(energyToUse);
                entity.breakProgress += energyToUse;
            }

            // Update block breaking animation (0-9 progress stages)
            int breakStage = (int) ((entity.breakProgress / entity.currentBreakTime) * 10f);
            breakStage = Math.min(breakStage, 9);
            world.destroyBlockProgress(entity.breakingEntityId, target, breakStage);

            if (entity.breakProgress >= entity.currentBreakTime) {
                // Clear breaking animation
                world.destroyBlockProgress(entity.breakingEntityId, target, -1);

                entity.mineBlock(world, target, targetState);
                entity.advanceMiningPosition();
                entity.resetBreakProgress();

                // Skip air blocks immediately to find the next real target
                entity.skipToNextSolidBlock(world, state);

                // Calculate the new target position and set arm there immediately
                // The client handles smooth interpolation, so we can set the target directly
                BlockPos nextTarget = entity.calculateMiningTargetPos(state);
                if (nextTarget != null) {
                    float oldArmX = entity.armX;
                    float oldArmY = entity.armY;
                    float oldArmZ = entity.armZ;

                    // Set arm to new target position
                    entity.armX = nextTarget.getX() + 0.5f;
                    entity.armY = nextTarget.getY() + 1.0f;
                    entity.armZ = nextTarget.getZ() + 0.5f;

                    // Calculate travel time for settling (client interpolation catch-up)
                    float dx = entity.armX - oldArmX;
                    float dy = entity.armY - oldArmY;
                    float dz = entity.armZ - oldArmZ;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    entity.expectedTravelTicks = (int) Math.ceil(distance / entity.getEffectiveArmSpeed());

                    // Go directly to SETTLING to wait for client interpolation
                    entity.armState = ArmState.SETTLING;
                    entity.settlingTicksRemaining = Math.max(1, entity.expectedTravelTicks);
                    entity.currentTarget = nextTarget;
                } else {
                    // No more targets - finished
                    entity.armState = ArmState.MOVING;
                }
                entity.syncToClients();
            }
        }
    }

    /**
     * Move the arm towards the target position.
     * @return true if the arm has reached the target
     */
    private boolean moveArmTowards(float targetX, float targetY, float targetZ) {
        float dx = targetX - armX;
        float dy = targetY - armY;
        float dz = targetZ - armZ;

        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float speed = getEffectiveArmSpeed();

        if (distance <= speed) {
            // Close enough, snap to target
            armX = targetX;
            armY = targetY;
            armZ = targetZ;
            return true;
        }

        // Normalize and move at effective speed
        float factor = speed / distance;
        armX += dx * factor;
        armY += dy * factor;
        armZ += dz * factor;

        return false;
    }

    /**
     * Check if the arm is at the target position.
     */
    private boolean isAtTarget(float targetX, float targetY, float targetZ) {
        float dx = targetX - armX;
        float dy = targetY - armY;
        float dz = targetZ - armZ;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return distance <= getEffectiveArmSpeed();
    }

    /**
     * Calculate how many ticks it will take to reach the target at current speed.
     */
    private int calculateTravelTicks(float targetX, float targetY, float targetZ) {
        float dx = targetX - armX;
        float dy = targetY - armY;
        float dz = targetZ - armZ;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return (int) Math.ceil(distance / getEffectiveArmSpeed());
    }

    private boolean shouldSkipBlock(Level world, BlockPos pos, BlockState state) {
        return GridScanner.shouldSkip(world, pos, state);
    }

    private void mineBlock(ServerLevel world, BlockPos target, BlockState targetState) {
        // Get drops before breaking the block
        BlockEntity blockEntity = world.getBlockEntity(target);
        List<ItemStack> drops = Block.getDrops(targetState, world, target, blockEntity, null, ItemStack.EMPTY);

        // Break the block without natural drops (we handle drops manually)
        world.destroyBlock(target, false);

        // Output the calculated drops
        for (ItemStack drop : drops) {
            outputItem(world, drop);
        }

        // Fallback: if getDroppedStacks returned nothing but this wasn't air,
        // or if this was a container (block entity), collect any spawned items
        if (drops.isEmpty() || blockEntity != null) {
            collectNearbyItems(world, target);
        }
    }

    private void collectNearbyItems(ServerLevel world, BlockPos target) {
        List<ItemEntity> itemEntities = world.getEntitiesOfClass(
                ItemEntity.class, new net.minecraft.world.phys.AABB(target).inflate(2.0), item -> true);

        for (ItemEntity itemEntity : itemEntities) {
            ItemStack stack = itemEntity.getItem();
            if (!stack.isEmpty()) {
                outputItem(world, stack.copy());
                itemEntity.discard();
            }
        }
    }

    private void outputItem(ServerLevel world, ItemStack stack) {
        if (stack.isEmpty()) return;

        BlockPos quarryPos = getBlockPos();
        BlockPos abovePos = quarryPos.above();

        // Check if there's a transport block above
        BlockState aboveState = world.getBlockState(abovePos);
        TransportApi transportApi = LogisticsApi.Registry.transport();
        if (transportApi.isTransportBlock(aboveState)) {
            transportApi.forceInsert(world, abovePos, stack.copy(), Direction.UP);
            return;
        }

        // Check if there's an inventory above (chest, barrel, etc.)
        if (!stack.isEmpty()) {
            BlockEntity aboveEntity = world.getBlockEntity(abovePos);
            if (aboveEntity instanceof Container inv) {
                // Check if it's a sided inventory and respects insertion from below
                if (aboveEntity instanceof WorldlyContainer sidedInv) {
                    int[] availableSlots = sidedInv.getSlotsForFace(Direction.DOWN);
                    for (int slot : availableSlots) {
                        if (stack.isEmpty()) break;
                        if (!sidedInv.canPlaceItemThroughFace(slot, stack, Direction.DOWN)) continue;
                        stack = insertIntoSlot(inv, slot, stack);
                    }
                } else {
                    // Regular inventory - try all slots
                    for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                        if (stack.isEmpty()) break;
                        if (!inv.canPlaceItem(slot, stack)) continue;
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
            itemEntity.setDeltaMovement(0, 0.2, 0); // Small upward velocity
            world.addFreshEntity(itemEntity);
        }
    }

    /**
     * Try to insert a stack into a specific slot of an inventory.
     * @return the remaining stack (may be empty if fully inserted)
     */
    private ItemStack insertIntoSlot(Container inv, int slot, ItemStack stack) {
        ItemStack existing = inv.getItem(slot);

        if (existing.isEmpty()) {
            // Empty slot - insert up to max stack size
            int maxInsert = Math.min(stack.getCount(), Math.min(inv.getMaxStackSize(), stack.getMaxStackSize()));
            inv.setItem(slot, stack.split(maxInsert));
        } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
            // Same item - try to merge
            int space = Math.min(inv.getMaxStackSize(), existing.getMaxStackSize()) - existing.getCount();
            if (space > 0) {
                int toInsert = Math.min(space, stack.getCount());
                existing.grow(toInsert);
                stack.shrink(toInsert);
            }
        }

        return stack;
    }

    private void advanceToNextBlock() {
        miningX++;
        int maxX = bounds.isCustom() ? (bounds.getMaxX() - bounds.getMinX() + 1) : LogisticsConfig.get().quarry.area;
        int maxZ = bounds.isCustom() ? (bounds.getMaxZ() - bounds.getMinZ() + 1) : LogisticsConfig.get().quarry.area;

        if (miningX >= maxX) {
            miningX = 0;
            miningZ++;
            if (miningZ >= maxZ) {
                miningZ = 0;
                miningY++;
            }
        }
        setChanged();
    }

    private @Nullable BlockPos calculateClearingTargetPos(BlockState quarryState) {
        return GridScanner.clearingTarget(
                LaserQuarryBlock.getMiningDirection(quarryState),
                getBlockPos(),
                bounds,
                LogisticsConfig.get().quarry.area,
                miningX,
                miningY,
                miningZ);
    }

    private @Nullable BlockPos calculateMiningTargetPos(BlockState quarryState) {
        return GridScanner.miningTarget(
                LaserQuarryBlock.getMiningDirection(quarryState),
                getBlockPos(),
                bounds,
                LogisticsConfig.get().quarry.area,
                level.getMinBuildHeight(),
                miningX,
                miningY,
                miningZ);
    }

    /**
     * Advance mining position for the mining area.
     */
    private void advanceMiningPosition() {
        int innerSizeX;
        int innerSizeZ;
        if (bounds.isCustom()) {
            innerSizeX = bounds.getMaxX() - bounds.getMinX() - 1;
            innerSizeZ = bounds.getMaxZ() - bounds.getMinZ() - 1;
        } else {
            innerSizeX = LogisticsConfig.get().quarry.area - 2;
            innerSizeZ = LogisticsConfig.get().quarry.area - 2;
        }
        if (innerSizeX <= 0 || innerSizeZ <= 0) {
            return;
        }

        miningX++;
        if (miningX >= innerSizeX) {
            miningX = 0;
            miningZ++;
            if (miningZ >= innerSizeZ) {
                miningZ = 0;
                miningY++;
            }
        }
        setChanged();
    }

    /**
     * Skip past air/fluid/bedrock blocks to find the next solid target.
     * Called after mining a block to immediately find the next real target
     * before syncing to clients (prevents arm hiccup).
     */
    private void skipToNextSolidBlock(ServerLevel world, BlockState quarryState) {
        for (int skipped = 0; skipped < LogisticsConfig.get().quarry.scanRate; skipped++) {
            BlockPos target = calculateMiningTargetPos(quarryState);
            if (target == null) {
                // Reached end of mining area
                finished = true;
                return;
            }

            BlockState targetState = world.getBlockState(target);
            if (!shouldSkipBlock(world, target, targetState)) {
                // Found a solid block to mine next
                return;
            }

            advanceMiningPosition();
        }
    }

    /**
     * Clear any active block breaking animation.
     * Called when the quarry stops or changes target.
     */
    private void clearBreakingAnimation(ServerLevel world) {
        if (currentTarget != null) {
            world.destroyBlockProgress(breakingEntityId, currentTarget, -1);
        }
    }

    private @Nullable BlockPos getNextFramePosition(BlockState quarryState) {
        return FrameLayout.nextFramePosition(
                LaserQuarryBlock.getMiningDirection(quarryState),
                getBlockPos(),
                bounds,
                LogisticsConfig.get().quarry.area,
                frameBuildIndex);
    }

    private BlockState calculateFrameState(BlockState quarryState, BlockPos framePos) {
        return FrameLayout.frameBlockState(
                LaserQuarryBlock.getMiningDirection(quarryState),
                getBlockPos(),
                framePos,
                bounds,
                LogisticsConfig.get().quarry.area);
    }

    /**
     * Set custom mining bounds from markers.
     * The top Y is derived from the quarry's position + offset (same as default mining).
     */
    public void setCustomBounds(int minX, int minZ, int maxX, int maxZ) {
        bounds.setCustom(minX, minZ, maxX, maxZ);
        this.miningX = 0;
        this.miningY = 0;
        this.miningZ = 0;
        this.finished = false;
        setChanged();
    }

    private void resetBreakProgress() {
        breakProgress = 0f;
        currentTarget = null;
        currentBreakTime = -1f;
    }

    // ==================== Energy Storage Access ====================

    /**
     * Consumes energy from the buffer if available.
     * Sets consumedEnergyThisTick flag for LED rendering.
     */
    private void consumeEnergy(long amount) {
        if (energy.getAmount() >= amount) {
            energy.consume(amount);
            consumedEnergyThisTick = true; // Flag for LED when buffer is low
            setChanged();
        }
    }

    /**
     * Checks if the quarry has at least the specified amount of energy.
     *
     * @param amount the amount to check
     * @return true if enough energy is available
     */
    private boolean hasEnergy(long amount) {
        return energy.getAmount() >= amount;
    }

    /**
     * Gets the energy level as a ratio from 0.0 to 1.0.
     */
    public double getEnergyLevel() {
        return (double) energy.getAmount() / LogisticsConfig.get().quarry.energyCapacity();
    }

    /**
     * Gets the current move cost based on buffer level.
     * Formula: ceil(20 + buffer/10) RF/tick
     */
    private long getMoveCost() {
        return (long) Math.ceil(
                LogisticsConfig.get().quarry.armEnergy + (double) energy.getAmount() / MOVE_COST_BUFFER_DIVISOR);
    }

    private long getCurrentBufferDraw() {
        if (finished) {
            return 0;
        }

        return switch (currentPhase) {
            case CLEARING -> currentBreakTime < 0
                    ? LogisticsConfig.get().quarry.maxEnergyInput()
                    : currentBreakTime > breakProgress
                            ? (long) Math.ceil(currentBreakTime - breakProgress)
                            : 0;
            case BUILDING_FRAME -> FRAME_BUILD_COST;
            case MINING -> switch (armState) {
                case MOVING -> getMoveCost();
                case SETTLING -> 0;
                case BREAKING -> currentBreakTime < 0
                        ? LogisticsConfig.get().quarry.maxEnergyInput()
                        : currentBreakTime > breakProgress
                                ? (long) Math.ceil(currentBreakTime - breakProgress)
                                : 0;
            };
        };
    }

    /**
     * Gets the effective arm speed based on energy consumption.
     * Formula: 0.1 + (energyUsed / 2000) blocks/tick
     * Applies rain penalty if exposed to rain.
     */
    private float getEffectiveArmSpeed() {
        long moveCost = getMoveCost();
        float speed = LogisticsConfig.get().quarry.armSpeed + (moveCost / LogisticsConfig.get().quarry.armSpeedScaling);

        // Apply rain penalty if quarry is exposed to rain
        if (level != null && level.isRainingAt(worldPosition.above())) {
            speed *= LogisticsConfig.get().quarry.rainPenalty;
        }

        return speed;
    }

    @Override
    public long networkDemandPerTick() {
        long storageRoom = Math.max(0, LogisticsConfig.get().quarry.energyCapacity() - energy.getAmount());
        long remainingInput = Math.max(0, LogisticsConfig.get().quarry.maxEnergyInput() - energyReceivedThisTick);
        return Math.min(remainingInput, storageRoom);
    }

    public long getEnergyReceivedLastTick() {
        return energyReceivedLastTick;
    }

    // ==================== Probe Support ====================

    /**
     * Creates probe result with quarry diagnostic information.
     */
    public ProbeResult getProbeResult() {
        ProbeResult.Builder builder = ProbeResult.builder("Laser Quarry Status");

        // Phase with required energy
        String phaseName =
                switch (currentPhase) {
                    case CLEARING -> "Clearing";
                    case BUILDING_FRAME -> "Building Frame";
                    case MINING -> "Mining";
                };
        if (finished) {
            builder.entry("Phase", "Finished", ChatFormatting.AQUA);
        } else if (currentPhase == Phase.BUILDING_FRAME) {
            builder.entry("Phase", phaseName + " (need 240 RF)", ChatFormatting.AQUA);
        } else {
            builder.entry("Phase", phaseName, ChatFormatting.AQUA);
        }

        // Energy
        double energyPercent = getEnergyLevel() * 100;
        ChatFormatting energyColor =
                energyPercent > 50 ? ChatFormatting.GREEN : energyPercent > 20 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        builder.entry(
                "Energy",
                String.format("%,d / %,d RF (%.1f%%)", energy.getAmount(), LogisticsConfig.get().quarry.energyCapacity(), energyPercent),
                energyColor);

        // Power consumption and speed (only during active phases)
        if (!finished) {
            builder.entry("Power In", String.format("%,d RF/t", energyReceivedLastTick), ChatFormatting.GREEN);
            builder.entry("Buffer Draw", String.format("%,d RF/t", getCurrentBufferDraw()), ChatFormatting.GOLD);

            if (currentPhase == Phase.MINING) {
                float speed = getEffectiveArmSpeed();
                String speedText = String.format("%.2f blocks/tick", speed);
                if (level != null && level.isRainingAt(worldPosition.above())) {
                    speedText += " (rain)";
                }
                builder.entry("Arm Speed", speedText, ChatFormatting.LIGHT_PURPLE);
            }
        }

        // Warnings
        if (energy.getAmount() == 0 && !finished) {
            builder.warning("No power!");
        }

        return builder.build();
    }

    // NBT serialization
    @Override
    protected void saveLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveLogisticsData(nbt, registries);

        // Save energy
        energy.writeNbt(nbt, "Energy");

        // Save mining state
        nbt.putInt("MiningX", miningX);
        nbt.putInt("MiningY", miningY);
        nbt.putInt("MiningZ", miningZ);
        nbt.putFloat("BreakProgress", breakProgress);
        nbt.putBoolean("MiningFinished", finished);
        nbt.putString("CurrentPhase", currentPhase.name());
        nbt.putInt("FrameBuildIndex", frameBuildIndex);

        // Save arm state
        nbt.putString("ArmState", armState.name());
        nbt.putFloat("ArmX", armX);
        nbt.putFloat("ArmY", armY);
        nbt.putFloat("ArmZ", armZ);
        nbt.putBoolean("ArmInitialized", armInitialized);
        nbt.putInt("ArmSettlingTicks", settlingTicksRemaining);
        nbt.putInt("ArmExpectedTravelTicks", expectedTravelTicks);
        nbt.putFloat("ArmSyncedSpeed", syncedArmSpeed);

        // Save custom bounds
        bounds.save(nbt);
    }

    @Override
    protected void loadLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadLogisticsData(nbt, registries);

        // Load energy (try new key first, fall back to legacy)
        if (nbt.contains("Energy")) {
            energy.readNbt(nbt, "Energy");
        } else {
            energy.setAmount(NbtCompat.getLong(nbt, "StoredEnergy", 0L));
        }

        // Load mining state
        miningX = NbtCompat.getInt(nbt, "MiningX", 0);
        miningY = NbtCompat.getInt(nbt, "MiningY", 0);
        miningZ = NbtCompat.getInt(nbt, "MiningZ", 0);
        breakProgress = NbtCompat.getFloat(nbt, "BreakProgress", 0f);
        finished = NbtCompat.getBoolean(nbt, "MiningFinished", false);
        frameBuildIndex = NbtCompat.getInt(nbt, "FrameBuildIndex", 0);

        String phaseName = NbtCompat.getString(nbt, "CurrentPhase", "CLEARING");
        try {
            currentPhase = Phase.valueOf(phaseName);
        } catch (IllegalArgumentException e) {
            currentPhase = Phase.CLEARING;
        }

        // Load arm state
        String armStateName = NbtCompat.getString(nbt, "ArmState", "MOVING");
        try {
            armState = ArmState.valueOf(armStateName);
        } catch (IllegalArgumentException e) {
            armState = ArmState.MOVING;
        }
        armX = NbtCompat.getFloat(nbt, "ArmX", 0f);
        armY = NbtCompat.getFloat(nbt, "ArmY", 0f);
        armZ = NbtCompat.getFloat(nbt, "ArmZ", 0f);
        armInitialized = NbtCompat.getBoolean(nbt, "ArmInitialized", false);
        settlingTicksRemaining = NbtCompat.getInt(nbt, "ArmSettlingTicks", 0);
        expectedTravelTicks = NbtCompat.getInt(nbt, "ArmExpectedTravelTicks", 0);
        syncedArmSpeed = NbtCompat.getFloat(nbt, "ArmSyncedSpeed", 0.0f);

        // Load custom bounds
        bounds.load(nbt);
    }

    @Override
    protected void loadLegacyData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadLegacyData(nbt, registries);

        bounds.clear();

        // Load energy from old "Energy" tag
        if (nbt.contains("Energy")) {
            CompoundTag energyState = nbt.getCompound("Energy");
            energy.setAmount(NbtCompat.getLong(energyState, "Amount", 0L));
        }

        // Load mining state from old "MiningState" tag
        if (nbt.contains("MiningState")) {
            CompoundTag miningState = nbt.getCompound("MiningState");
            miningX = NbtCompat.getInt(miningState, "X", 0);
            miningY = NbtCompat.getInt(miningState, "Y", 0);
            miningZ = NbtCompat.getInt(miningState, "Z", 0);
            breakProgress = NbtCompat.getFloat(miningState, "Progress", 0f);
            finished = NbtCompat.getBoolean(miningState, "Finished", false);
            frameBuildIndex = NbtCompat.getInt(miningState, "FrameBuildIndex", 0);

            String phaseName = NbtCompat.getString(miningState, "Phase", "CLEARING");
            try {
                currentPhase = Phase.valueOf(phaseName);
            } catch (IllegalArgumentException e) {
                currentPhase = Phase.CLEARING;
            }

            // Load arm state
            String armStateName = NbtCompat.getString(miningState, "ArmState", "MOVING");
            try {
                armState = ArmState.valueOf(armStateName);
            } catch (IllegalArgumentException e) {
                armState = ArmState.MOVING;
            }
            armX = NbtCompat.getFloat(miningState, "ArmX", 0f);
            armY = NbtCompat.getFloat(miningState, "ArmY", 0f);
            armZ = NbtCompat.getFloat(miningState, "ArmZ", 0f);
            armInitialized = NbtCompat.getBoolean(miningState, "ArmInitialized", false);
            settlingTicksRemaining = NbtCompat.getInt(miningState, "SettlingTicks", 0);
            expectedTravelTicks = NbtCompat.getInt(miningState, "ExpectedTravelTicks", 0);
            syncedArmSpeed = NbtCompat.getFloat(miningState, "SyncedArmSpeed", LogisticsConfig.get().quarry.armSpeed);
        }

        // Load custom bounds from old "CustomBounds" tag
        if (nbt.contains("CustomBounds")) {
            CompoundTag customBoundsNbt = nbt.getCompound("CustomBounds");
            bounds.loadLegacy(
                    NbtCompat.getInt(customBoundsNbt, "MinX", 0),
                    NbtCompat.getInt(customBoundsNbt, "MinZ", 0),
                    NbtCompat.getInt(customBoundsNbt, "MaxX", 0),
                    NbtCompat.getInt(customBoundsNbt, "MaxZ", 0));
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        if (level != null && level.isClientSide()) {
            ClientRenderCacheHooks.clearQuarryInterpolationCache(worldPosition);
        }

        if (level != null && !level.isClientSide()) {
            ActiveQuarryRegistry.unregister((ServerLevel) level, worldPosition);
            // Clear any active breaking animation
            if (currentTarget != null) {
                ((ServerLevel) level).destroyBlockProgress(breakingEntityId, currentTarget, -1);
            }
        }
    }

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    // Arm position getters for renderer
    public float getArmX() {
        return armX;
    }

    public float getArmY() {
        return armY;
    }

    public float getArmZ() {
        return armZ;
    }

    public ArmState getArmState() {
        return armState;
    }

    public float getSyncedArmSpeed() {
        return syncedArmSpeed;
    }

    public boolean isArmInitialized() {
        return armInitialized;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean consumedEnergyThisTick() {
        return consumedEnergyThisTick;
    }

    // Custom bounds getters for frame decay logic
    public boolean hasCustomBounds() {
        return bounds.isCustom();
    }

    public int getCustomMinX() {
        return bounds.getMinX();
    }

    public int getCustomMinZ() {
        return bounds.getMinZ();
    }

    public int getCustomMaxX() {
        return bounds.getMaxX();
    }

    public int getCustomMaxZ() {
        return bounds.getMaxZ();
    }

    /** True if the quarry has been placed but hasn't started any clearing yet. */
    public boolean isFreshlyPlaced() {
        return currentPhase == Phase.CLEARING
                && miningX == 0 && miningY == 0 && miningZ == 0 && breakProgress == 0;
    }

    public static List<BlockPos> getActiveQuarries(ServerLevel world) {
        return ActiveQuarryRegistry.getAll(world);
    }

    public static void clearActiveQuarries(ServerLevel world) {
        ActiveQuarryRegistry.clear(world);
    }

    // PipeConnection interface implementation

    /**
     * Quarry accepts pipe connections from above.
     * Returns PIPE connection type so pipes render arms to the quarry.
     * Returns NONE for all other directions.
     */
    @Override
    public PipeConnection.Type getConnectionType(Direction direction) {
        return direction == Direction.UP ? PipeConnection.Type.PIPE : PipeConnection.Type.NONE;
    }

    /**
     * Quarry does not accept items from pipes.
     * It only pushes items out.
     */
    @Override
    public boolean addItem(Direction from, ItemStack stack) {
        return false;
    }

    /**
     * Quarry never accepts items from any direction.
     */
    @Override
    public boolean canAcceptFrom(Direction from, ItemStack stack) {
        return false;
    }
}
