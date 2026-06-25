package com.logistics.core.machine;

import com.logistics.core.machine.component.EnergyStorageComponent;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.ItemStoreComponent;
import com.logistics.core.machine.component.RecipeProcessorComponent;
import com.logistics.core.machine.component.RecipeProcessorComponent.LitController;
import com.logistics.core.machine.component.RecipeResolver;
import com.logistics.core.machine.component.SidedLayout;
import com.logistics.core.machine.component.SlotRole;
import java.util.function.Predicate;
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

    /** Escape hatch for registering a component that has no fluent builder yet. */
    public <T extends MachineComponent> T add(T component) {
        return components.add(component);
    }

    /** Builds an {@link EnergyStorageComponent}. */
    public final class EnergyBuilder {
        private final String id;
        private long capacity;
        private long maxInput;
        private long maxOutput;
        private boolean providesDemand;

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

        /** Track energy received per tick and report it as logistics-network demand. */
        public EnergyBuilder providesDemand() {
            this.providesDemand = true;
            return this;
        }

        public EnergyStorageComponent build() {
            return components.add(new EnergyStorageComponent(
                    id, capacity, maxInput, maxOutput, providesDemand, providesDemand, onChanged));
        }
    }

    /** Builds a single-variant {@link FluidStoreComponent} tank. */
    public final class FluidBuilder {
        private final String id;
        private long capacity;

        private FluidBuilder(String id) {
            this.id = id;
        }

        public FluidBuilder capacity(long capacity) {
            this.capacity = capacity;
            return this;
        }

        public FluidStoreComponent build() {
            if (capacity <= 0) {
                throw new IllegalStateException("fluids(" + id + ") requires a positive capacity, got " + capacity);
            }
            return components.add(new FluidStoreComponent(id, capacity, onChanged));
        }
    }

    /** Builds an {@link ItemStoreComponent} with role-based slots and a sided-access layout. */
    public final class ItemsBuilder {
        private enum AccessMode {
            NONE,
            FURNACE,
            BOTTOM_OUT
        }

        private final String id;
        private SlotRole[] roles = new SlotRole[0];
        private AccessMode accessMode = AccessMode.NONE;
        private Predicate<ItemStack> insertFilter = stack -> true;

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

        public ItemStoreComponent build() {
            // Resolve the layout from the final roles so slot order is independent of call order.
            SidedLayout layout =
                    switch (accessMode) {
                        case FURNACE -> SidedLayout.furnace(
                                indicesOf(SlotRole.INPUT), indicesOf(SlotRole.OUTPUT), insertFilter);
                        case BOTTOM_OUT -> SidedLayout.bottomOut(
                                indicesOf(SlotRole.INPUT), indicesOf(SlotRole.OUTPUT), insertFilter);
                        case NONE -> null;
                    };
            return components.add(new ItemStoreComponent(id, roles, layout, onChanged));
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

        /** Hook that toggles the machine's working/lit block state. */
        public RecipeProcessorBuilder lit(LitController lit) {
            this.lit = lit;
            return this;
        }

        public RecipeProcessorComponent build() {
            java.util.Objects.requireNonNull(resolver, "recipeProcessor(" + id + ") requires a resolver");
            java.util.Objects.requireNonNull(items, "recipeProcessor(" + id + ") requires items");
            java.util.Objects.requireNonNull(energy, "recipeProcessor(" + id + ") requires energy");
            return components.add(new RecipeProcessorComponent(id, resolver, rfPerTick, items, energy, lit, onChanged));
        }
    }
}
