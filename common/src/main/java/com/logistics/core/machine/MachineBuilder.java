package com.logistics.core.machine;

import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.machine.component.ChunkLoadingComponent;
import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.component.RecipeProcessorComponent;
import com.logistics.core.machine.component.RecipeProcessorComponent.LitController;
import com.logistics.core.machine.component.RecipeResolver;
import com.logistics.core.machine.component.SidedLayout;
import com.logistics.core.machine.component.SlotRole;
import com.logistics.core.machine.control.RedstoneControlComponent;
import com.logistics.core.machine.control.RedstoneMode;
import com.logistics.core.machine.upgrade.UpgradeComponent;
import java.util.function.Predicate;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.item.ItemStack;

/**
 * Fluent assembler for a machine's components. A {@link MachineEntity} hands one of these to
 * {@code configure(...)}; each {@code build()} constructs the component, registers it (and its tick
 * order) with the host, and returns it for wiring into later components. The host's change
 * notification is supplied automatically, so machines never thread {@code this::setChanged} through.
 */
public final class MachineBuilder {

    private final MachineComponentContainer components;
    private final Runnable onChanged;

    public MachineBuilder(MachineComponentContainer components, Runnable onChanged) {
        this.components = components;
        this.onChanged = onChanged;
    }

    public EnergyBuilder energy(String id) {
        return new EnergyBuilder(id);
    }

    public ItemsBuilder items(String id) {
        return new ItemsBuilder(id);
    }

    public FluidBuilder fluids(String id) {
        return new FluidBuilder(id);
    }

    public RecipeProcessorBuilder recipeProcessor(String id) {
        return new RecipeProcessorBuilder(id);
    }

    /** Registers an {@link UpgradeComponent} (no configuration) and returns it for wiring. */
    public UpgradeComponent upgrades(String id) {
        return components.add(new UpgradeComponent(id));
    }

    public ChunkLoaderBuilder chunkLoader(String id) {
        return new ChunkLoaderBuilder(id);
    }

    /** Escape hatch for registering a component that has no fluent builder yet. */
    public <T extends MachineComponent> T add(T component) {
        return components.add(component);
    }

    /** Builds a {@link ChunkLoadingComponent} fed by a machine-supplied area provider. */
    public final class ChunkLoaderBuilder {
        private final String id;
        private TicketType centerType;
        private TicketType boundaryType;
        private ChunkLoadingComponent.AreaProvider area;

        private ChunkLoaderBuilder(String id) {
            this.id = id;
        }

        /** The ticket types to use for the center chunk and the surrounding boundary chunks. */
        public ChunkLoaderBuilder tickets(TicketType center, TicketType boundary) {
            this.centerType = center;
            this.boundaryType = boundary;
            return this;
        }

        /** Supplies the chunks to keep loaded (or {@code null} for none) each tick. */
        public ChunkLoaderBuilder area(ChunkLoadingComponent.AreaProvider area) {
            this.area = area;
            return this;
        }

        public ChunkLoadingComponent build() {
            return components.add(new ChunkLoadingComponent(id, centerType, boundaryType, area));
        }
    }

    /** Builds an {@link EnergyStorageComponent}. */
    public final class EnergyBuilder {
        private final String id;
        private long capacity;
        private long maxInput;
        private long maxOutput;

        private EnergyBuilder(String id) {
            this.id = id;
        }

        public EnergyBuilder capacity(long capacity) {
            this.capacity = capacity;
            return this;
        }

        public EnergyBuilder maxInput(long maxInput) {
            this.maxInput = maxInput;
            return this;
        }

        public EnergyBuilder maxOutput(long maxOutput) {
            this.maxOutput = maxOutput;
            return this;
        }

        public EnergyStorageComponent build() {
            // A buffer that accepts input (maxInput > 0) reports demand and pulls from the power
            // network automatically; no opt-in needed.
            return components.add(new EnergyStorageComponent(id, capacity, maxInput, maxOutput, onChanged));
        }
    }

    /** Builds a single-variant {@link FluidStoreComponent} tank. */
    public final class FluidBuilder {
        private final String id;
        private long capacity;
        private boolean outputOnly;
        private Predicate<IFluidKey> inputFilter;

        private FluidBuilder(String id) {
            this.id = id;
        }

        public FluidBuilder capacity(long capacity) {
            this.capacity = capacity;
            return this;
        }

        /** Exposes the tank to pipes as drain-only (the machine fills it internally). */
        public FluidBuilder outputOnly() {
            this.outputOnly = true;
            return this;
        }

        /**
         * Exposes the tank to pipes as fill-only, accepting just the fluids {@code filter} matches (the
         * machine drains it internally).
         */
        public FluidBuilder inputOnly(Predicate<IFluidKey> filter) {
            this.inputFilter = java.util.Objects.requireNonNull(filter);
            return this;
        }

        public FluidStoreComponent build() {
            if (capacity <= 0) {
                throw new IllegalStateException("fluids(" + id + ") requires a positive capacity, got " + capacity);
            }
            if (inputFilter != null && outputOnly) {
                throw new IllegalStateException("fluids(" + id + ") cannot be both inputOnly and outputOnly");
            }
            return components.add(inputFilter != null
                    ? FluidStoreComponent.inputOnly(id, capacity, onChanged, inputFilter)
                    : new FluidStoreComponent(id, capacity, onChanged, outputOnly));
        }
    }

    /** Builds an {@link ItemStoreComponent} with role-based slots and a sided-access layout. */
    public final class ItemsBuilder {
        private enum AccessMode {
            NONE,
            FURNACE,
            BOTTOM_OUT,
            TOP_IN,
            ANY_FACE
        }

        private record SlotRange(int from, int toExclusive, Predicate<ItemStack> filter) {}

        private final String id;
        private SlotRole[] roles = new SlotRole[0];
        private AccessMode accessMode = AccessMode.NONE;
        private Predicate<ItemStack> insertFilter = stack -> true;
        private final java.util.List<SlotRange> slotFilters = new java.util.ArrayList<>();

        private ItemsBuilder(String id) {
            this.id = id;
        }

        public ItemsBuilder slots(SlotRole... roles) {
            this.roles = roles;
            return this;
        }

        /** Vanilla-furnace access: input from the top, output from the bottom, sides expose nothing. */
        public ItemsBuilder furnaceAccess() {
            return furnaceAccess(stack -> true);
        }

        /** Vanilla-furnace access with an insertion filter on the input slots. */
        public ItemsBuilder furnaceAccess(Predicate<ItemStack> insertFilter) {
            this.accessMode = AccessMode.FURNACE;
            this.insertFilter = insertFilter;
            return this;
        }

        /** Bottom-out access: input from any non-bottom face, output from the bottom. */
        public ItemsBuilder bottomOutAccess() {
            return bottomOutAccess(stack -> true);
        }

        /** Bottom-out access with an insertion filter on the input slots. */
        public ItemsBuilder bottomOutAccess(Predicate<ItemStack> insertFilter) {
            this.accessMode = AccessMode.BOTTOM_OUT;
            this.insertFilter = insertFilter;
            return this;
        }

        /** Top-in access: input from the top only, nothing extractable (product is ejected directly). */
        public ItemsBuilder topInAccess() {
            return topInAccess(stack -> true);
        }

        /** Top-in access with an insertion filter on the input slots. */
        public ItemsBuilder topInAccess(Predicate<ItemStack> insertFilter) {
            this.accessMode = AccessMode.TOP_IN;
            this.insertFilter = insertFilter;
            return this;
        }

        /**
         * Any-face access: input slots insertable and output slots extractable from every face;
         * {@link SlotRole#GENERIC} slots are unreachable. Route items to slots with
         * {@link #slotFilter}.
         */
        public ItemsBuilder anyFaceAccess() {
            this.accessMode = AccessMode.ANY_FACE;
            this.insertFilter = stack -> true;
            return this;
        }

        /**
         * Restricts slots {@code fromSlot} (inclusive) to {@code toSlotExclusive} to items matching
         * {@code filter}, for face insertion and {@code Container#canPlaceItem} alike. A slot covered
         * by several ranges must satisfy all of them.
         */
        public ItemsBuilder slotFilter(int fromSlot, int toSlotExclusive, Predicate<ItemStack> filter) {
            slotFilters.add(new SlotRange(fromSlot, toSlotExclusive, java.util.Objects.requireNonNull(filter)));
            return this;
        }

        public ItemStoreComponent build() {
            // Resolve the layout from the final roles so slot order is independent of call order.
            SidedLayout layout =
                    switch (accessMode) {
                        case FURNACE -> SidedLayout.furnace(
                                indicesOf(SlotRole.INPUT), indicesOf(SlotRole.OUTPUT), insertFilter);
                        case BOTTOM_OUT -> SidedLayout.bottomOut(
                                indicesOf(SlotRole.INPUT), indicesOf(SlotRole.OUTPUT), insertFilter);
                        case TOP_IN -> SidedLayout.topIn(indicesOf(SlotRole.INPUT), insertFilter);
                        case ANY_FACE -> SidedLayout.anyFace(indicesOf(SlotRole.INPUT), indicesOf(SlotRole.OUTPUT));
                        case NONE -> throw new IllegalStateException("items(" + id
                                + ") requires an access mode: furnaceAccess(), bottomOutAccess(), topInAccess()"
                                + " or anyFaceAccess()");
                    };
            if (!slotFilters.isEmpty()) {
                layout = layout.withSlotFilter(composedSlotFilter());
            }
            return components.add(new ItemStoreComponent(id, roles, layout, onChanged));
        }

        private SidedLayout.SlotFilter composedSlotFilter() {
            for (SlotRange range : slotFilters) {
                if (range.from() < 0 || range.toExclusive() <= range.from() || range.toExclusive() > roles.length) {
                    throw new IllegalStateException("items(" + id + ") slotFilter range [" + range.from() + ", "
                            + range.toExclusive() + ") is outside its " + roles.length + " slots");
                }
            }
            SlotRange[] ranges = slotFilters.toArray(new SlotRange[0]);
            return (slot, stack) -> {
                for (SlotRange range : ranges) {
                    if (slot >= range.from() && slot < range.toExclusive() && !range.filter().test(stack)) {
                        return false;
                    }
                }
                return true;
            };
        }

        private int[] indicesOf(SlotRole role) {
            return java.util.stream.IntStream.range(0, roles.length).filter(i -> roles[i] == role).toArray();
        }
    }

    /** Builds a {@link RecipeProcessorComponent}. */
    public final class RecipeProcessorBuilder {
        private final String id;
        private RecipeResolver resolver;
        private long rfPerTick;
        private ItemStoreComponent items;
        private EnergyStorageComponent energy;
        private FluidStoreComponent fluids;
        private FluidStoreComponent inputFluids;
        private LitController lit = (ctx, on) -> {};

        private RecipeProcessorBuilder(String id) {
            this.id = id;
        }

        public RecipeProcessorBuilder resolver(RecipeResolver resolver) {
            this.resolver = resolver;
            return this;
        }

        public RecipeProcessorBuilder rfPerTick(long rfPerTick) {
            this.rfPerTick = rfPerTick;
            return this;
        }

        public RecipeProcessorBuilder items(ItemStoreComponent items) {
            this.items = items;
            return this;
        }

        public RecipeProcessorBuilder energy(EnergyStorageComponent energy) {
            this.energy = energy;
            return this;
        }

        /** Optional fluid output: recipes with a {@code FluidResult} deposit into this tank. */
        public RecipeProcessorBuilder fluids(FluidStoreComponent fluids) {
            this.fluids = fluids;
            return this;
        }

        /** Optional fluid input: recipes with a fluid input drain from this tank. */
        public RecipeProcessorBuilder inputFluids(FluidStoreComponent inputFluids) {
            this.inputFluids = inputFluids;
            return this;
        }

        /** Hook that toggles the machine's working/lit block state. */
        public RecipeProcessorBuilder lit(LitController lit) {
            this.lit = lit;
            return this;
        }

        public RecipeProcessorComponent build() {
            java.util.Objects.requireNonNull(resolver, "recipeProcessor(" + id + ") requires a resolver");
            java.util.Objects.requireNonNull(items, "recipeProcessor(" + id + ") requires items");
            java.util.Objects.requireNonNull(energy, "recipeProcessor(" + id + ") requires energy");
            if (rfPerTick <= 0) {
                throw new IllegalStateException(
                        "recipeProcessor(" + id + ") requires a positive rfPerTick, got " + rfPerTick);
            }
            return components.add(new RecipeProcessorComponent(
                    id, resolver, rfPerTick, items, energy, fluids, inputFluids, lit, onChanged));
        }
    }

    public RedstoneControlBuilder redstoneControl(String id) {
        return new RedstoneControlBuilder(id);
    }

    /** Builds a {@link RedstoneControlComponent}; the default mode is {@link RedstoneMode#IGNORED}. */
    public final class RedstoneControlBuilder {
        private final String id;
        private RedstoneMode defaultMode = RedstoneMode.IGNORED;

        private RedstoneControlBuilder(String id) {
            this.id = id;
        }

        public RedstoneControlBuilder defaultMode(RedstoneMode defaultMode) {
            this.defaultMode = java.util.Objects.requireNonNull(defaultMode);
            return this;
        }

        public RedstoneControlComponent build() {
            return components.add(new RedstoneControlComponent(id, defaultMode, onChanged));
        }
    }
}
