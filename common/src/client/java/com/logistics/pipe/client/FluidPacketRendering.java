package com.logistics.pipe.client;

import com.logistics.LogisticsMod;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.data.PipeDataComponents.FluidPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * Version-agnostic core of fluid-packet item rendering, shared by every branch's item-render adapter.
 *
 * <p>The packet draws the carried fluid's still sprite (16px, animated) as a flat quad behind the frame
 * texture (a bubble ring with a transparent center window). The two things that differ per MC version are
 * the render <em>entry point</em> and how a quad is submitted:
 * <ul>
 *   <li>26.2: {@code SpecialModelRenderer.submit(...)} via {@code SubmitNodeCollector.submitCustomGeometry}.</li>
 *   <li>26.1 / 1.21.11: the special-model system exists but draws through the classic
 *       {@code render(...)} path (no {@code submit}).</li>
 *   <li>1.21.1: no data-driven item models — a {@code BuiltinItemRendererRegistry}/BEWLR renderer.</li>
 * </ul>
 * Everything else — reading the fluid from the stack, resolving its sprite/tint (through the already
 * version-shimmed {@link FluidBoxRenderer}), and the two-quad geometry — lives here so each per-version
 * adapter is a thin wrapper that only performs the submit/draw calls. Keep this class free of any
 * version-specific render types or submit APIs.
 */
public final class FluidPacketRendering {
    private FluidPacketRendering() {}

    /**
     * Special-model-renderer id referenced by {@code assets/logistics/items/pipe/fluid_packet.json} and
     * registered per loader. Plain {@code logistics:fluid_packet} (a free-form renderer id, NOT the
     * pipe-domain {@code logistics:pipe/...} path) so it matches the id in the item model JSON.
     */
    public static final ResourceId ID = LogisticsMod.modId("fluid_packet");

    /**
     * The frame texture, drawn in front of the fluid. Lives under {@code textures/block/} (sprite id
     * {@code logistics:block/pipe/fluid_packet}) so vanilla's default {@code block/} atlas source stitches it
     * onto the BLOCKS atlas — the frame is drawn with a {@code *MovingBlock} render type (see
     * {@code FluidPacketSpecialRenderer}), which binds that atlas. An {@code item/} texture would only reach
     * the items atlas and render magenta under the blocks-atlas lookup.
     */
    public static final ResourceId FRAME_TEXTURE = LogisticsMod.modId("block/pipe/fluid_packet");

    // item/generated occupies z 7.5..8.5 (of 16); +z faces the viewer. The fluid sits in the middle with a
    // frame plane on EACH side (front + back), so whichever face you view, the near frame is in front of the
    // fluid and its transparent window reveals it. A single frame plane would be occluded by the fluid from
    // the opposite side.
    public static final float FRAME_Z = 8.5F / 16.0F;       // front frame plane
    public static final float FRAME_BACK_Z = 7.5F / 16.0F;  // back frame plane
    public static final float FLUID_Z = 8.0F / 16.0F;       // fluid, sandwiched between the two frames

    // The frame is a small round bubble: an opaque glass ring (~13px circle) with a transparent hole inside
    // AND transparent slot outside it. A full-slot fluid quad would bleed through that outside, so the fluid
    // is drawn as a disc matching the bubble — centred on the ring, radius tucking its rim under the glass.
    // Coords are geometric (y already flipped by the vertex UV), so centre lands at 8.5/16 on both axes.
    public static final float FLUID_CENTER_X = 8.5F / 16.0F;
    public static final float FLUID_CENTER_Y = 8.5F / 16.0F;
    public static final float FLUID_RADIUS = 5.5F / 16.0F;
    public static final int FLUID_DISC_SEGMENTS = 32;

    /** The fluid a packet stack carries, or {@code null} if it has no packet component. */
    @Nullable
    public static Fluid fluidOf(ItemStack stack) {
        FluidPacket packet = stack.get(LogisticsPipe.DATA.FLUID_PACKET);
        return packet != null ? packet.fluid() : null;
    }

    /** The still sprite + opaque GUI-tint to draw for {@code fluid}, or {@code null} if unavailable. */
    @Nullable
    public static FluidBoxRenderer.Appearance fluidAppearance(Fluid fluid) {
        return FluidBoxRenderer.resolveForGui(fluid);
    }
}
