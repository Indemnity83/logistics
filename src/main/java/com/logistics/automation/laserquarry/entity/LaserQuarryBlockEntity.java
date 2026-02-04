package com.logistics.automation.laserquarry.entity;

import com.logistics.api.EnergyStorage;
import com.logistics.api.LogisticsApi;
import com.logistics.api.TransportApi;
import com.logistics.automation.laserquarry.LaserQuarryBlock;
import com.logistics.automation.laserquarry.LaserQuarryConfig;
import com.logistics.automation.laserquarry.LaserQuarryFrameBlock;
import com.logistics.automation.registry.AutomationBlockEntities;
import com.logistics.automation.registry.AutomationBlocks;
import com.logistics.automation.render.ClientRenderCacheHooks;
import com.logistics.core.lib.pipe.PipeConnection;
import com.logistics.core.lib.support.ProbeResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class LaserQuarryBlockEntity extends BlockEntity implements EnergyStorage, PipeConnection {
    private static final long REGISTRY_TTL_TICKS = 200L;
    private static final Map<ResourceKey<Level>, Map<Long, Long>> ACTIVE_QUARRIES = new HashMap<>();

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
    private long energy = 0;
    private long lastSyncedEnergy = 0; // For client sync

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
    private boolean useCustomBounds = false;
    private int customMinX = 0;
    private int customMinZ = 0;
    private int customMaxX = 0;
    private int customMaxZ = 0;

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
    private float syncedArmSpeed = LaserQuarryConfig.ARM_SPEED; // Speed synced to clients for interpolation

    public LaserQuarryBlockEntity(BlockPos pos, BlockState state) {
        super(AutomationBlockEntities.LASER_QUARRY_BLOCK_ENTITY, pos, state);
        // Use position hash as unique entity ID for breaking animation
        this.breakingEntityId = pos.hashCode();
    }

    public static void tick(Level world, BlockPos pos, BlockState state, LaserQuarryBlockEntity entity) {
        if (world.isClientSide()) {
            return;
        }

        registerActiveQuarry((ServerLevel) world, pos);

        if (entity.finished) {
            return;
        }

        ServerLevel serverWorld = (ServerLevel) world;

        switch (entity.currentPhase) {
            case CLEARING -> tickClearing(serverWorld, pos, state, entity);
            case BUILDING_FRAME -> tickBuildingFrame(serverWorld, pos, state, entity);
            case MINING -> tickMining(serverWorld, pos, state, entity);
            default -> {}
        }

        // Sync energy and arm speed to clients when energy changes
        // Check at end of tick after operations may have consumed energy
        if (entity.energy != entity.lastSyncedEnergy) {
            entity.lastSyncedEnergy = entity.energy;
            entity.syncedArmSpeed = entity.getEffectiveArmSpeed();
            entity.syncToClients();
        }
    }

    /**
     * Sync arm state to clients. Called on arm state transitions.
     */
    private void syncToClients() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    private static void tickClearing(ServerLevel world, BlockPos pos, BlockState state, LaserQuarryBlockEntity entity) {
        // Need at least some energy to operate
        if (entity.energy == 0) {
            entity.resetBreakProgress();
            return;
        }

        // Skip quickly through blocks at or above quarry level
        BlockPos target = null;
        BlockState targetState = null;
        for (int skipped = 0; skipped < LaserQuarryConfig.MAX_SKIP_PER_TICK; skipped++) {
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
            entity.currentBreakTime = (float) (LaserQuarryConfig.BREAK_ENERGY_MULTIPLIER * (hardness + 1));
            entity.breakProgress = 0;
        }

        // Consume as much energy as possible towards breaking (like BC)
        long energyNeeded = (long) Math.ceil(entity.currentBreakTime - entity.breakProgress);
        long energyToUse = Math.min(entity.energy, energyNeeded);
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
        if (!entity.hasEnergy(LaserQuarryConfig.FRAME_BUILD_COST)) {
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
        entity.consumeEnergy(LaserQuarryConfig.FRAME_BUILD_COST);

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
        for (int skipped = 0; skipped < LaserQuarryConfig.MAX_SKIP_PER_TICK; skipped++) {
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
                entity.currentBreakTime = (float) (LaserQuarryConfig.BREAK_ENERGY_MULTIPLIER * (hardness + 1));
                entity.breakProgress = 0;
            }

            // Consume as much energy as possible towards breaking (like BC)
            long energyNeeded = (long) Math.ceil(entity.currentBreakTime - entity.breakProgress);
            long energyToUse = Math.min(entity.energy, energyNeeded);
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
        // Skip air
        if (state.isAir()) {
            return true;
        }
        // Skip fluids (water, lava)
        if (!state.getFluidState().isEmpty()) {
            return true;
        }
        // Skip unbreakable blocks (bedrock, barriers, etc.)
        float hardness = state.getDestroySpeed(world, pos);
        if (hardness < 0) {
            return true;
        }
        return false;
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
        int maxX = useCustomBounds ? (customMaxX - customMinX + 1) : LaserQuarryConfig.CHUNK_SIZE;
        int maxZ = useCustomBounds ? (customMaxZ - customMinZ + 1) : LaserQuarryConfig.CHUNK_SIZE;

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

    /**
     * Calculate target position for clearing phase (area at/above quarry level).
     */
    private @Nullable BlockPos calculateClearingTargetPos(BlockState quarryState) {
        BlockPos quarryPos = getBlockPos();

        int startX;
        int startZ;
        if (useCustomBounds) {
            startX = customMinX;
            startZ = customMinZ;
        } else {
            Direction facing = LaserQuarryBlock.getMiningDirection(quarryState);
            switch (facing) {
                case NORTH:
                    startX = quarryPos.getX() - 8;
                    startZ = quarryPos.getZ() - LaserQuarryConfig.CHUNK_SIZE;
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
                    startX = quarryPos.getX() - LaserQuarryConfig.CHUNK_SIZE;
                    startZ = quarryPos.getZ() - 8;
                    break;
                default:
                    return null;
            }
        }

        // Clearing phase only works above and at quarry level
        int startY = quarryPos.getY() + LaserQuarryConfig.Y_OFFSET_ABOVE;
        int currentY = startY - miningY;

        // Stop when we go below quarry level
        if (currentY < quarryPos.getY()) {
            return null;
        }

        int targetX = startX + miningX;
        int targetZ = startZ + miningZ;

        return new BlockPos(targetX, currentY, targetZ);
    }

    /**
     * Calculate target position for mining phase (area below quarry level).
     * The area is inset 1 block from the frame to stay within it.
     */
    private @Nullable BlockPos calculateMiningTargetPos(BlockState quarryState) {
        BlockPos quarryPos = getBlockPos();

        int startX;
        int startZ;
        int innerSizeX;
        int innerSizeZ;
        if (useCustomBounds) {
            // Custom bounds: mining area is inset 1 block from frame
            startX = customMinX + 1;
            startZ = customMinZ + 1;
            innerSizeX = customMaxX - customMinX - 1; // frame size - 2 for inset
            innerSizeZ = customMaxZ - customMinZ - 1;
        } else {
            Direction facing = LaserQuarryBlock.getMiningDirection(quarryState);
            switch (facing) {
                case NORTH:
                    startX = quarryPos.getX() - 8 + 1; // Inset 1 from frame
                    startZ = quarryPos.getZ() - LaserQuarryConfig.CHUNK_SIZE + 1;
                    break;
                case SOUTH:
                    startX = quarryPos.getX() - 8 + 1;
                    startZ = quarryPos.getZ() + 1 + 1;
                    break;
                case EAST:
                    startX = quarryPos.getX() + 1 + 1;
                    startZ = quarryPos.getZ() - 8 + 1;
                    break;
                case WEST:
                    startX = quarryPos.getX() - LaserQuarryConfig.CHUNK_SIZE + 1;
                    startZ = quarryPos.getZ() - 8 + 1;
                    break;
                default:
                    return null;
            }
            innerSizeX = LaserQuarryConfig.INNER_SIZE;
            innerSizeZ = LaserQuarryConfig.INNER_SIZE;
        }
        if (innerSizeX <= 0 || innerSizeZ <= 0) {
            return null;
        }

        // Mining phase starts 1 block below quarry level
        int startY = quarryPos.getY() - 1;
        int currentY = startY - miningY;

        // Stop at bedrock or world bottom
        if (currentY < level.getMinY()) {
            return null;
        }

        // 3D zigzag pattern: continuous movement across layers
        // Z direction reverses each layer (even layers go forward, odd layers go backward)
        int targetZ;
        if (miningY % 2 == 0) {
            targetZ = startZ + miningZ;
        } else {
            targetZ = startZ + (innerSizeZ - 1 - miningZ);
        }

        // X direction based on total rows traversed (continuous zigzag)
        int totalRows = miningY * innerSizeZ + miningZ;
        int targetX;
        if (totalRows % 2 == 0) {
            targetX = startX + miningX;
        } else {
            targetX = startX + (innerSizeX - 1 - miningX);
        }

        return new BlockPos(targetX, currentY, targetZ);
    }

    /**
     * Advance mining position for the mining area.
     */
    private void advanceMiningPosition() {
        int innerSizeX;
        int innerSizeZ;
        if (useCustomBounds) {
            innerSizeX = customMaxX - customMinX - 1;
            innerSizeZ = customMaxZ - customMinZ - 1;
        } else {
            innerSizeX = LaserQuarryConfig.INNER_SIZE;
            innerSizeZ = LaserQuarryConfig.INNER_SIZE;
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
        for (int skipped = 0; skipped < LaserQuarryConfig.MAX_SKIP_PER_TICK; skipped++) {
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

    /**
     * Get the next frame position to build based on frameBuildIndex.
     * Frame consists of:
     * - Bottom ring at quarryY
     * - Middle pillars at corners from quarryY+1 to quarryY+3 (4 corners × 3 = 12 blocks)
     * - Top ring at quarryY+4
     */
    private @Nullable BlockPos getNextFramePosition(BlockState quarryState) {
        BlockPos quarryPos = getBlockPos();

        // Calculate frame bounds
        int startX;
        int startZ;
        int endX;
        int endZ;
        if (useCustomBounds) {
            startX = customMinX;
            startZ = customMinZ;
            endX = customMaxX;
            endZ = customMaxZ;
        } else {
            Direction facing = LaserQuarryBlock.getMiningDirection(quarryState);
            switch (facing) {
                case NORTH:
                    startX = quarryPos.getX() - 8;
                    startZ = quarryPos.getZ() - LaserQuarryConfig.CHUNK_SIZE;
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
                    startX = quarryPos.getX() - LaserQuarryConfig.CHUNK_SIZE;
                    startZ = quarryPos.getZ() - 8;
                    break;
                default:
                    return null;
            }
            endX = startX + LaserQuarryConfig.CHUNK_SIZE - 1;
            endZ = startZ + LaserQuarryConfig.CHUNK_SIZE - 1;
        }

        int bottomY = quarryPos.getY();
        int topY = quarryPos.getY() + LaserQuarryConfig.Y_OFFSET_ABOVE;

        // Calculate ring size: perimeter of rectangle = 2*width + 2*depth - 4 corners
        int width = endX - startX + 1;
        int depth = endZ - startZ + 1;
        int ringSize = 2 * width + 2 * depth - 4;

        // Phase 1: Bottom ring
        if (frameBuildIndex < ringSize) {
            return getRingPosition(frameBuildIndex, startX, startZ, endX, endZ, bottomY);
        }

        // Phase 2: Middle pillars (12 blocks)
        int pillarIndex = frameBuildIndex - ringSize;
        if (pillarIndex < 12) {
            int cornerIndex = pillarIndex / 3;
            int yOffset = (pillarIndex % 3) + 1; // Y+1, Y+2, Y+3
            int y = bottomY + yOffset;

            return switch (cornerIndex) {
                case 0 -> new BlockPos(startX, y, startZ);
                case 1 -> new BlockPos(endX, y, startZ);
                case 2 -> new BlockPos(endX, y, endZ);
                case 3 -> new BlockPos(startX, y, endZ);
                default -> null;
            };
        }

        // Phase 3: Top ring
        int topRingIndex = frameBuildIndex - ringSize - 12;
        if (topRingIndex < ringSize) {
            return getRingPosition(topRingIndex, startX, startZ, endX, endZ, topY);
        }

        // Done building frame
        return null;
    }

    /**
     * Get a position on a frame ring given an index.
     * Iterates around the perimeter: north edge, east edge, south edge, west edge.
     */
    private BlockPos getRingPosition(int index, int startX, int startZ, int endX, int endZ, int y) {
        int width = endX - startX + 1;
        int depth = endZ - startZ + 1;

        // North edge: width blocks
        if (index < width) {
            return new BlockPos(startX + index, y, startZ);
        }
        index -= width;

        // East edge: depth-1 blocks excluding NE corner
        int eastEdgeSize = depth - 1;
        if (index < eastEdgeSize) {
            return new BlockPos(endX, y, startZ + 1 + index);
        }
        index -= eastEdgeSize;

        // South edge: width-1 blocks excluding SE corner
        int southEdgeSize = width - 1;
        if (index < southEdgeSize) {
            return new BlockPos(endX - 1 - index, y, endZ);
        }
        index -= southEdgeSize;

        // West edge: depth-2 blocks excluding SW and NW corners
        int westEdgeSize = depth - 2;
        if (index < westEdgeSize) {
            return new BlockPos(startX, y, endZ - 1 - index);
        }

        return null;
    }

    /**
     * Calculate the frame block state with appropriate arm connections.
     */
    private BlockState calculateFrameState(BlockState quarryState, BlockPos framePos) {
        BlockPos quarryPos = getBlockPos();

        // Calculate frame bounds
        int startX;
        int startZ;
        int endX;
        int endZ;
        if (useCustomBounds) {
            startX = customMinX;
            startZ = customMinZ;
            endX = customMaxX;
            endZ = customMaxZ;
        } else {
            Direction facing = LaserQuarryBlock.getMiningDirection(quarryState);
            switch (facing) {
                case NORTH:
                    startX = quarryPos.getX() - 8;
                    startZ = quarryPos.getZ() - LaserQuarryConfig.CHUNK_SIZE;
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
                    startX = quarryPos.getX() - LaserQuarryConfig.CHUNK_SIZE;
                    startZ = quarryPos.getZ() - 8;
                    break;
                default:
                    return AutomationBlocks.LASER_QUARRY_FRAME.getDefaultState();
            }
            endX = startX + LaserQuarryConfig.CHUNK_SIZE - 1;
            endZ = startZ + LaserQuarryConfig.CHUNK_SIZE - 1;
        }
        int bottomY = quarryPos.getY();
        int topY = quarryPos.getY() + LaserQuarryConfig.Y_OFFSET_ABOVE;

        int x = framePos.getX();
        int y = framePos.getY();
        int z = framePos.getZ();

        // Determine which arms to enable based on position in frame
        boolean north = false;
        boolean south = false;
        boolean east = false;
        boolean west = false;
        boolean up = false;
        boolean down = false;

        // Check if this is a corner position
        boolean isCornerX = (x == startX || x == endX);
        boolean isCornerZ = (z == startZ || z == endZ);
        boolean isCorner = isCornerX && isCornerZ;

        // Vertical connections (corners only, not at very top/bottom if ring exists)
        if (isCorner) {
            if (y > bottomY) down = true;
            if (y < topY) up = true;
        }

        // Horizontal connections on edges
        if (y == bottomY || y == topY) {
            // We're on a ring - connect horizontally
            if (z == startZ) { // North edge
                if (x > startX) west = true;
                if (x < endX) east = true;
            }
            if (z == endZ) { // South edge
                if (x > startX) west = true;
                if (x < endX) east = true;
            }
            if (x == startX) { // West edge
                if (z > startZ) north = true;
                if (z < endZ) south = true;
            }
            if (x == endX) { // East edge
                if (z > startZ) north = true;
                if (z < endZ) south = true;
            }
        }

        LaserQuarryFrameBlock frameBlock = (LaserQuarryFrameBlock) AutomationBlocks.LASER_QUARRY_FRAME;
        return frameBlock.withArms(north, south, east, west, up, down);
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

    // ==================== EnergyStorage Implementation ====================

    @Override
    public long getAmount() {
        return energy;
    }

    @Override
    public long getCapacity() {
        return LaserQuarryConfig.ENERGY_CAPACITY;
    }

    @Override
    public long insert(long maxAmount, boolean simulate) {
        long space = LaserQuarryConfig.ENERGY_CAPACITY - energy;
        long accepted = Math.min(maxAmount, Math.min(space, LaserQuarryConfig.MAX_ENERGY_INPUT));
        if (!simulate && accepted > 0) {
            energy += accepted;
            setChanged();
        }
        return accepted;
    }

    @Override
    public long extract(long maxAmount, boolean simulate) {
        // Quarry doesn't allow external extraction
        return 0;
    }

    @Override
    public boolean canInsert() {
        return true;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    /**
     * Consumes energy from the buffer if available.
     *
     * @param amount the amount of energy to consume
     * @return true if the energy was consumed, false if insufficient energy
     */
    private boolean consumeEnergy(long amount) {
        if (energy >= amount) {
            energy -= amount;
            return true;
        }
        return false;
    }

    /**
     * Checks if the quarry has at least the specified amount of energy.
     *
     * @param amount the amount to check
     * @return true if enough energy is available
     */
    private boolean hasEnergy(long amount) {
        return energy >= amount;
    }

    /**
     * Gets the energy level as a ratio from 0.0 to 1.0.
     */
    public double getEnergyLevel() {
        return (double) energy / LaserQuarryConfig.ENERGY_CAPACITY;
    }

    /**
     * Gets the current move cost based on buffer level.
     * Formula: ceil(20 + buffer/10) RF/tick
     */
    private long getMoveCost() {
        return (long) Math.ceil(
                LaserQuarryConfig.BASE_MOVE_COST + (double) energy / LaserQuarryConfig.MOVE_COST_BUFFER_DIVISOR);
    }

    /**
     * Gets the effective arm speed based on energy consumption.
     * Formula: 0.1 + (energyUsed / 2000) blocks/tick
     * Applies rain penalty if exposed to rain.
     */
    private float getEffectiveArmSpeed() {
        long moveCost = getMoveCost();
        float speed = LaserQuarryConfig.BASE_MOVE_SPEED + (moveCost / LaserQuarryConfig.SPEED_ENERGY_DIVISOR);

        // Apply rain penalty if quarry is exposed to rain
        if (level != null && level.isRainingAt(worldPosition.above())) {
            speed *= LaserQuarryConfig.RAIN_SPEED_MULTIPLIER;
        }

        return speed;
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
                String.format("%,d / %,d RF (%.1f%%)", energy, LaserQuarryConfig.ENERGY_CAPACITY, energyPercent),
                energyColor);

        // Power consumption and speed (only during active phases)
        if (!finished) {
            long moveCost = getMoveCost();
            builder.entry("Consumption", String.format("%,d RF/t", moveCost), ChatFormatting.GOLD);

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
        if (energy == 0 && !finished) {
            builder.warning("No power!");
        }

        return builder.build();
    }

    // NBT serialization using the new WriteView/ReadView API
    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);

        // Save energy
        CompoundTag energyState = new CompoundTag();
        energyState.putLong("Amount", energy);
        view.store("Energy", CompoundTag.CODEC, energyState);

        // Save mining state
        CompoundTag miningState = new CompoundTag();
        miningState.putInt("X", miningX);
        miningState.putInt("Y", miningY);
        miningState.putInt("Z", miningZ);
        miningState.putFloat("Progress", breakProgress);
        miningState.putBoolean("Finished", finished);
        miningState.putString("Phase", currentPhase.name());
        miningState.putInt("FrameBuildIndex", frameBuildIndex);
        // Arm state
        miningState.putString("ArmState", armState.name());
        miningState.putFloat("ArmX", armX);
        miningState.putFloat("ArmY", armY);
        miningState.putFloat("ArmZ", armZ);
        miningState.putBoolean("ArmInitialized", armInitialized);
        miningState.putInt("SettlingTicks", settlingTicksRemaining);
        miningState.putInt("ExpectedTravelTicks", expectedTravelTicks);
        miningState.putFloat("SyncedArmSpeed", syncedArmSpeed);
        view.store("MiningState", CompoundTag.CODEC, miningState);

        // Save custom bounds
        if (useCustomBounds) {
            CompoundTag customBoundsNbt = new CompoundTag();
            customBoundsNbt.putInt("MinX", customMinX);
            customBoundsNbt.putInt("MinZ", customMinZ);
            customBoundsNbt.putInt("MaxX", customMaxX);
            customBoundsNbt.putInt("MaxZ", customMaxZ);
            view.store("CustomBounds", CompoundTag.CODEC, customBoundsNbt);
        } else {
            view.discard("CustomBounds");
        }
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);

        useCustomBounds = false;
        customMinX = 0;
        customMinZ = 0;
        customMaxX = 0;
        customMaxZ = 0;

        // Load energy
        energy = 0;
        view.read("Energy", CompoundTag.CODEC).ifPresent(energyState -> {
            energy = energyState.getLong("Amount").orElse(0L);
        });

        // Load mining state
        view.read("MiningState", CompoundTag.CODEC).ifPresent(miningState -> {
            miningX = miningState.getInt("X").orElse(0);
            miningY = miningState.getInt("Y").orElse(0);
            miningZ = miningState.getInt("Z").orElse(0);
            breakProgress = miningState.getFloat("Progress").orElse(0f);
            finished = miningState.getBoolean("Finished").orElse(false);
            frameBuildIndex = miningState.getInt("FrameBuildIndex").orElse(0);
            miningState.getString("Phase").ifPresent(phaseName -> {
                try {
                    currentPhase = Phase.valueOf(phaseName);
                } catch (IllegalArgumentException e) {
                    currentPhase = Phase.CLEARING;
                }
            });
            // Load arm state
            miningState.getString("ArmState").ifPresent(armStateName -> {
                try {
                    armState = ArmState.valueOf(armStateName);
                } catch (IllegalArgumentException e) {
                    armState = ArmState.MOVING;
                }
            });
            armX = miningState.getFloat("ArmX").orElse(0f);
            armY = miningState.getFloat("ArmY").orElse(0f);
            armZ = miningState.getFloat("ArmZ").orElse(0f);
            armInitialized = miningState.getBoolean("ArmInitialized").orElse(false);
            settlingTicksRemaining = miningState.getInt("SettlingTicks").orElse(0);
            expectedTravelTicks = miningState.getInt("ExpectedTravelTicks").orElse(0);
            syncedArmSpeed = miningState.getFloat("SyncedArmSpeed").orElse(LaserQuarryConfig.ARM_SPEED);
        });

        // Load custom bounds
        view.read("CustomBounds", CompoundTag.CODEC).ifPresent(customBoundsNbt -> {
            useCustomBounds = true;
            customMinX = customBoundsNbt.getInt("MinX").orElse(0);
            customMinZ = customBoundsNbt.getInt("MinZ").orElse(0);
            customMaxX = customBoundsNbt.getInt("MaxX").orElse(0);
            customMaxZ = customBoundsNbt.getInt("MaxZ").orElse(0);
        });
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return saveWithoutMetadata(registryLookup);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);

        if (level != null && level.isClientSide()) {
            ClientRenderCacheHooks.clearQuarryInterpolationCache(pos);
        }

        if (level != null && !level.isClientSide()) {
            unregisterActiveQuarry((ServerLevel) level, pos);
            // Clear any active breaking animation
            if (currentTarget != null) {
                ((ServerLevel) level).destroyBlockProgress(breakingEntityId, currentTarget, -1);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide()) {
            ClientRenderCacheHooks.clearQuarryInterpolationCache(worldPosition);
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

    // Custom bounds getters for frame decay logic
    public boolean hasCustomBounds() {
        return useCustomBounds;
    }

    public int getCustomMinX() {
        return customMinX;
    }

    public int getCustomMinZ() {
        return customMinZ;
    }

    public int getCustomMaxX() {
        return customMaxX;
    }

    public int getCustomMaxZ() {
        return customMaxZ;
    }

    public static List<BlockPos> getActiveQuarries(ServerLevel world) {
        ResourceKey<Level> key = world.dimension();
        Map<Long, Long> entries = ACTIVE_QUARRIES.get(key);
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        long now = world.getGameTime();
        java.util.Iterator<java.util.Map.Entry<Long, Long>> iterator =
                entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Long> entry = iterator.next();
            if (now - entry.getValue() > REGISTRY_TTL_TICKS) {
                iterator.remove();
            }
        }

        if (entries.isEmpty()) {
            ACTIVE_QUARRIES.remove(key);
            return List.of();
        }

        List<BlockPos> positions = new ArrayList<>(entries.size());
        for (Long posLong : entries.keySet()) {
            positions.add(BlockPos.of(posLong));
        }
        return positions;
    }

    public static void clearActiveQuarries(ServerLevel world) {
        ACTIVE_QUARRIES.remove(world.dimension());
    }

    private static void registerActiveQuarry(ServerLevel world, BlockPos pos) {
        ResourceKey<Level> key = world.dimension();
        Map<Long, Long> entries = ACTIVE_QUARRIES.computeIfAbsent(key, unused -> new HashMap<>());
        entries.put(pos.asLong(), world.getGameTime());
    }

    private static void unregisterActiveQuarry(ServerLevel world, BlockPos pos) {
        ResourceKey<Level> key = world.dimension();
        Map<Long, Long> entries = ACTIVE_QUARRIES.get(key);
        if (entries == null) {
            return;
        }
        entries.remove(pos.asLong());
        if (entries.isEmpty()) {
            ACTIVE_QUARRIES.remove(key);
        }
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
