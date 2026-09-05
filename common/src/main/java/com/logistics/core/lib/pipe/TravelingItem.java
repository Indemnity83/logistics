package com.logistics.core.lib.pipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents an item traveling through the pipe network.
 * Items move along pipe edges from one connection point to another.
 */
public class TravelingItem {
    /**
     * Default minimum speed for items in pipes.
     * Used as the CODEC default when no speed is stored (e.g. legacy saves).
     */
    public static final float DEFAULT_MIN_SPEED = 0.02f;


    /**
     * Default TTL for items with a destination (6000 ticks = 5 minutes at 20 TPS).
     * Decremented each tick; when it reaches 0 the destination is cleared so the item
     * falls back to default routing rather than being permanently stuck.
     */
    private static final int ITEM_TTL = 6000;
    /**
     * Progress threshold where routing decisions are made (item reaches pipe center).
     */
    public static final float ROUTE_POINT = 0.5f;

    /**
     * Progress threshold where server transfers item to next pipe/inventory.
     */
    public static final float SERVER_EXIT_THRESHOLD = 1.0f;

    /**
     * Progress threshold where client removes item from rendering (visual buffer to prevent flicker).
     */
    public static final float CLIENT_EXIT_THRESHOLD = 1.3f;
    /**
     * Codec used for saving/loading TravelingItem via ReadView/WriteView (1.21.8+).
     */
    public static final Codec<TravelingItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ItemStack.CODEC.fieldOf("item").forGetter(t -> t.stack),
                    Codec.INT
                            .fieldOf("direction")
                            .xmap(Direction::from3DDataValue, Direction::get3DDataValue)
                            .forGetter(t -> t.direction),
                    Codec.FLOAT
                            .optionalFieldOf("speed", DEFAULT_MIN_SPEED)
                            .forGetter(t -> t.speed),
                    Codec.FLOAT.optionalFieldOf("progress", 0.0f).forGetter(t -> t.progress),
                    Codec.BOOL.optionalFieldOf("routed", false).forGetter(t -> t.routed),
                    BlockPos.CODEC.optionalFieldOf("destination").forGetter(t -> java.util.Optional.ofNullable(t.destination)),
                    Codec.INT.optionalFieldOf("ttl", ITEM_TTL).forGetter(t -> t.remainingTtl))
            .apply(instance, TravelingItem::fromCodec));

    private static TravelingItem fromCodec(
            ItemStack stack, Direction direction, float speed, float progress, boolean routed,
            java.util.Optional<BlockPos> destination, int remainingTtl) {
        TravelingItem item = new TravelingItem(stack, direction, speed);
        item.progress = progress;
        item.routed = routed;
        item.destination = destination.orElse(null);
        item.remainingTtl = remainingTtl;
        return item;
    }

    private ItemStack stack;
    private float progress; // 0.0 = entering pipe, 1.0 = leaving pipe
    private Direction direction; // Direction of travel through current pipe
    private float speed; // Blocks per tick (varies by pipe material)
    private boolean routed; // True once the item has been routed at the center
    @Nullable private BlockPos destination; // Optional destination for network routing
    private int remainingTtl = ITEM_TTL; // Ticks remaining before destination is cleared (serialized)
    // Not serialized — lost on chunk reload; notifyDelivery accounting is best-effort
    @Nullable private UUID deliveryId;

    public TravelingItem(ItemStack stack, Direction direction, float speed) {
        this.stack = stack.copy();
        this.progress = 0.0f;
        this.direction = direction;
        this.speed = speed;
        this.routed = false;
        this.destination = null;
    }

    /**
     * Create a TravelingItem with an explicit destination for network routing.
     */
    public TravelingItem(ItemStack stack, Direction direction, float speed, @Nullable BlockPos destination) {
        this.stack = stack.copy();
        this.progress = 0.0f;
        this.direction = direction;
        this.speed = speed;
        this.routed = false;
        this.destination = destination;
    }

    /**
     * Update the item's position along the pipe.
     * Applies acceleration or drag, and if above max speed, decelerates to hit max at the segment exit.
     * @param accelerationRate How quickly to adjust speed (positive or negative)
     * @param dragCoefficient Fraction of speed lost per tick when not accelerating
     * @param maxSpeed Maximum allowed speed
     * @param minSpeed Speed floor; the pipe's configured minimum, not a constant
     * @return true if item reached the end of this pipe segment
     */
    public boolean tick(float accelerationRate, float dragCoefficient, float maxSpeed, float minSpeed) {
        speed = new TravelingItemPhysics(minSpeed)
                .updateSpeed(speed, progress, accelerationRate, dragCoefficient, maxSpeed);
        progress += speed;
        if (destination != null && remainingTtl > 0) remainingTtl--;
        return progress >= SERVER_EXIT_THRESHOLD;
    }

    /**
     * Get the current position along the pipe (0.0 to 1.0)
     */
    public float getProgress() {
        return progress;
    }

    /**
     * Get the direction this item is traveling
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Update direction without resetting progress (used for mid-pipe direction changes).
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
        this.routed = true;
    }

    /**
     * Get the ItemStack being transported
     */
    public ItemStack getStack() {
        return stack;
    }

    /**
     * Get the movement speed (blocks per tick)
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Set the movement speed (when entering different pipe types)
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * Set the current progress along the pipe (0.0 to 1.0).
     */
    public void setProgress(float progress) {
        this.progress = progress;
    }

    public boolean isRouted() {
        return routed;
    }

    public void setRouted(boolean routed) {
        this.routed = routed;
    }

    /**
     * Get the destination position for network routing.
     * @return Destination BlockPos, or null if not set
     */
    @Nullable
    public BlockPos getDestination() {
        return destination;
    }

    /**
     * Set the destination position for network routing.
     */
    public void setDestination(@Nullable BlockPos destination) {
        this.destination = destination;
    }

    /**
     * Returns true when the TTL has reached zero and the destination should be cleared.
     */
    public boolean isExpired() {
        return remainingTtl <= 0;
    }

    public int getRemainingTtl() {
        return remainingTtl;
    }

    public void setRemainingTtl(int remainingTtl) {
        this.remainingTtl = remainingTtl;
    }

    /**
     * Delivery tracking ID. When non-null, PipeRuntime calls network.notifyDelivery() when
     * this item is physically inserted into an inventory.
     * Not serialized — lost on chunk reload; orderedForRequester will drift but remains bounded.
     */
    @Nullable
    public UUID getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(@Nullable UUID deliveryId) {
        this.deliveryId = deliveryId;
    }
}
