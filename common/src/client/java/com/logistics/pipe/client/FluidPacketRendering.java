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
 * texture (an opaque frame with a rectangular transparent window). The two things that differ per MC
 * version are the render <em>entry point</em> and how a quad is submitted:
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

    // The frame has a rectangular transparent window — texture pixel columns 6-9 and rows 2-13 inclusive
    // (16px canvas) — with opaque frame everywhere else. A full-slot fluid quad would bleed through that
    // opaque area, so the fluid is drawn as a quad matching just the window; edges sit one pixel past the
    // last inclusive column/row (10, 14). Geometric Y is flipped by the vertex UV (y=0 is the bottom of
    // the quad, mapping to the texture's bottom row), so the pixel-row edges invert: geometric bottom
    // (Y0) = 1 - (bottomRowEdge / 16), geometric top (Y1) = 1 - (topRowEdge / 16).
    public static final float FLUID_WINDOW_X0 = 6.0F / 16.0F;
    public static final float FLUID_WINDOW_X1 = 10.0F / 16.0F;
    public static final float FLUID_WINDOW_Y0 = 1.0F - 14.0F / 16.0F;
    public static final float FLUID_WINDOW_Y1 = 1.0F - 2.0F / 16.0F;

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
