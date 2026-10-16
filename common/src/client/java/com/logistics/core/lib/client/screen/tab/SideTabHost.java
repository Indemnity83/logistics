package com.logistics.core.lib.client.screen.tab;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Controller for one screen's side tabs: layout, animation, accordion, hit-testing and input.
 * Created once per screen instance; {@link #attach()} is called from every {@code init()}.
 *
 * <p>The player's <em>intent</em> (which tab they opened on each side) changes only on clicks and is
 * mirrored to {@link TabMemory}. The <em>effective</em> state is recomputed from live geometry on
 * every layout: a tab is open iff it is intended, it fits, and its side is visible. Animations
 * follow the effective state, so a tab squeezed shut by a resize reopens when room returns, while a
 * tab the player closed stays closed.
 *
 * <p>The host is interactive only while it is being drawn: if {@link #render} has not run within
 * {@link #INTERACTIVE_WINDOW_MS}, it ignores clicks, reports no hover and no exclusion areas.
 */
public final class SideTabHost {
    /** Size of a collapsed tab. */
    public static final int COLLAPSED = 24;
    /** Height of the open tab's header strip; clicking it collapses the tab. */
    public static final int HEADER = 22;
    /** Offset of the first tab below the GUI image's top edge. */
    public static final int TOP_OFFSET = 4;
    /** Vertical gap between stacked tabs. */
    public static final int GAP = 1;
    /** How far a collapsed tab reaches over the GUI image: none, so the GUI's outline stays in front. */
    public static final int COLLAPSED_OVERLAP = 0;
    /** How far a fully open tab reaches over the GUI image: its outline and both bevel columns. */
    public static final int OPEN_OVERLAP = 3;
    /** Minimum distance kept from the screen edges. */
    public static final int SCREEN_MARGIN = 2;
    /** Horizontal content inset on each side. */
    public static final int CONTENT_PAD = 6;
    /** Space below the content. */
    public static final int BOTTOM_PAD = 6;
    /** Icon inset from the tab's outer corner. */
    public static final int ICON_INSET = 4;
    public static final int ICON_SIZE = 16;
    /** Title x in the open header, from the tab's left edge (after the icon, on either side). */
    public static final int TITLE_X = ICON_INSET + ICON_SIZE + 4;
    /** Narrowest title space an open tab keeps: about two glyphs and an ellipsis. */
    public static final int MIN_TITLE_WIDTH = 18;
    public static final int TITLE_Y = 8;
    public static final int TITLE_COLOR = 0xFFFFD34A;
    /** How long after the last {@link #render} the host keeps accepting input. */
    public static final long INTERACTIVE_WINDOW_MS = 250L;
    /** Tooltip line shown on a collapsed tab that has no room to open. */
    public static final String NO_ROOM_KEY = "gui.logistics.tab.no_room";

    private final String screenKey;
    private final TabAnchor anchor;
    private final TabText text;
    private final LongSupplier clock;
    private final Supplier<Component> noRoomLine;
    private final EnumMap<TabSide, List<Entry>> tabs = new EnumMap<>(TabSide.class);
    private final EnumMap<TabSide, String> openIntent = new EnumMap<>(TabSide.class);
    private final EnumMap<TabSide, BooleanSupplier> hideWhen = new EnumMap<>(TabSide.class);

    private boolean attached;
    private boolean rendered;
    private long lastRenderMs;
    private boolean pendingClickSound;
    private @Nullable Object view;

    public SideTabHost(String screenKey, TabAnchor anchor, TabText text) {
        this(screenKey, anchor, text, TabAnimation.SYSTEM_CLOCK);
    }

    public SideTabHost(String screenKey, TabAnchor anchor, TabText text, LongSupplier clock) {
        this(screenKey, anchor, text, clock, () -> Component.translatable(NO_ROOM_KEY));
    }

    public SideTabHost(
            String screenKey, TabAnchor anchor, TabText text, LongSupplier clock, Supplier<Component> noRoomLine) {
        this.screenKey = screenKey;
        this.anchor = anchor;
        this.text = text;
        this.clock = clock;
        this.noRoomLine = noRoomLine;
        for (TabSide side : TabSide.values()) {
            tabs.put(side, new ArrayList<>());
            String remembered = TabMemory.get(screenKey, side);
            if (remembered != null) {
                openIntent.put(side, remembered);
            }
        }
    }

    // ---- configuration ----

    /** Adds a tab below the ones already on its side. Ids must be unique within the host. */
    public SideTabHost add(SideTab tab) {
        for (List<Entry> entries : tabs.values()) {
            for (Entry entry : entries) {
                if (entry.tab.id().equals(tab.id())) {
                    throw new IllegalArgumentException("Duplicate side tab id: " + tab.id());
                }
            }
        }
        tabs.get(tab.side()).add(new Entry(tab, new TabAnimation(clock)));
        return this;
    }

    /** Hides {@code side} while {@code condition} holds; repeated calls combine with OR. */
    public SideTabHost hideSideWhen(TabSide side, BooleanSupplier condition) {
        BooleanSupplier existing = hideWhen.get(side);
        hideWhen.put(side, existing == null ? condition : () -> existing.getAsBoolean() || condition.getAsBoolean());
        return this;
    }

    public String screenKey() {
        return screenKey;
    }

    // ---- lifecycle ----

    /**
     * Call from every {@code init()}. Geometry is valid from here on; animations snap to the current
     * effective state, so a remembered tab appears open with no opening animation. Idempotent.
     */
    public SideTabHost attach() {
        attached = true;
        layout();
        for (List<Entry> entries : tabs.values()) {
            for (Entry entry : entries) {
                entry.animation.snap(entry.animation.targetOpen());
            }
        }
        return this;
    }

    /**
     * {@link #attach()}, then returns this host's view (for example its widget), created by
     * {@code viewFactory} on the first call and returned unchanged on every later call.
     */
    @SuppressWarnings("unchecked")
    public <T> T attach(Function<? super SideTabHost, ? extends T> viewFactory) {
        attach();
        if (view == null) {
            view = Objects.requireNonNull(viewFactory.apply(this), "side tab view");
        }
        return (T) view;
    }

    /** Draws the tabs and stamps the frame; input is accepted only shortly after a render. */
    public void render(TabCanvas canvas, int mouseX, int mouseY) {
        if (!attached) {
            return;
        }
        rendered = true;
        lastRenderMs = clock.getAsLong();
        if (pendingClickSound) {
            pendingClickSound = false;
            canvas.clickSound();
        }

        Placed hovered = null;
        for (Placed placed : layout()) {
            draw(canvas, placed, mouseX, mouseY);
            if (placed.rect.contains(mouseX, mouseY)) {
                hovered = placed;
            }
        }
        if (hovered != null) {
            List<Component> tooltip = tooltip(hovered, mouseX, mouseY);
            if (!tooltip.isEmpty()) {
                canvas.tooltip(tooltip, mouseX, mouseY);
            }
        }
    }

    // ---- input ----

    /**
     * Routes a click. Every click inside a tab is consumed; only primary clicks act. A collapsed tab
     * opens (closing the side's other tab), the header of an open tab closes it, and the body of a
     * fully open tab goes to {@link SideTab#clickContent}. A tab with no room consumes the click and
     * does nothing.
     */
    public boolean mouseClicked(double mouseX, double mouseY, boolean primary) {
        if (!isInteractive()) {
            return false;
        }
        Placed hit = hit(layout(), mouseX, mouseY);
        if (hit == null) {
            return false;
        }
        if (!primary || !hit.measure.fits) {
            return true;
        }
        SideTab tab = hit.entry.tab;
        if (!hit.open) {
            setIntent(tab.side(), tab.id());
        } else if (mouseY - hit.rect.y() < HEADER) {
            setIntent(tab.side(), null);
        } else if (hit.fullyOpen) {
            double localX = mouseX - (hit.rect.x() + CONTENT_PAD);
            double localY = mouseY - (hit.rect.y() + HEADER);
            if (tab.clickContent(localX, localY)) {
                pendingClickSound = true;
            }
        }
        return true;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return isInteractive() && hit(layout(), mouseX, mouseY) != null;
    }

    /** Vanilla's "clicked outside the GUI" verdict, with the tab rects counted as inside. */
    public boolean clickedOutside(boolean vanillaOutside, double mouseX, double mouseY) {
        return vanillaOutside && !isMouseOver(mouseX, mouseY);
    }

    /** Current animated rects of the visible tabs; empty when the host is not interactive. */
    public List<TabRect> exclusionAreas() {
        if (!isInteractive()) {
            return List.of();
        }
        List<TabRect> areas = new ArrayList<>();
        for (Placed placed : layout()) {
            areas.add(placed.rect);
        }
        return areas;
    }

    /**
     * One rect per visible side bounding that side's stack in its largest reachable layout (each
     * tab that fits opened in turn, the rest collapsed). Unaffected by animation; empty when the
     * host is not interactive.
     */
    public List<TabRect> envelope() {
        if (!isInteractive()) {
            return List.of();
        }
        List<TabRect> envelope = new ArrayList<>();
        for (TabSide side : TabSide.values()) {
            List<Entry> entries = tabs.get(side);
            if (entries.isEmpty() || !sideVisible(side)) {
                continue;
            }
            int margin = TabLayout.margin(side, anchor);
            int count = entries.size();
            int collapsedHeight = count * COLLAPSED + (count - 1) * GAP;
            int overlap = COLLAPSED_OVERLAP;
            int outward = COLLAPSED - COLLAPSED_OVERLAP;
            int height = collapsedHeight;
            for (int i = 0; i < count; i++) {
                Measure measure = measure(entries.get(i).tab, margin, i);
                if (measure.fits) {
                    overlap = OPEN_OVERLAP;
                    outward = Math.max(outward, measure.width - OPEN_OVERLAP);
                    height = Math.max(height, collapsedHeight + measure.height - COLLAPSED);
                }
            }
            int width = overlap + outward;
            envelope.add(new TabRect(
                    TabLayout.tabX(side, anchor, width, overlap), TabLayout.stackTop(anchor), width, height));
        }
        return envelope;
    }

    // ---- queries ----

    /** Whether {@code id} is effectively open and its animation has finished. */
    public boolean isFullyOpen(String id) {
        Placed placed = find(id);
        return placed != null && placed.fullyOpen;
    }

    /** The tab's current animated rect, regardless of interactivity; null when its side is hidden. */
    public @Nullable TabRect tabRect(String id) {
        Placed placed = find(id);
        return placed == null ? null : placed.rect;
    }

    /** The tab the player last opened on {@code side}, whether or not it currently fits. */
    public @Nullable String openIntent(TabSide side) {
        return openIntent.get(side);
    }

    public boolean anyAnimating() {
        if (!attached) {
            return false;
        }
        layout();
        for (List<Entry> entries : tabs.values()) {
            for (Entry entry : entries) {
                if (entry.animation.isAnimating()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isInteractive() {
        return attached && rendered && clock.getAsLong() - lastRenderMs <= INTERACTIVE_WINDOW_MS;
    }

    // ---- internals ----

    private void setIntent(TabSide side, @Nullable String id) {
        if (id == null) {
            openIntent.remove(side);
        } else {
            openIntent.put(side, id);
        }
        TabMemory.set(screenKey, side, id);
        pendingClickSound = true;
        layout();
    }

    private boolean sideVisible(TabSide side) {
        BooleanSupplier hidden = hideWhen.get(side);
        return TabLayout.sideFits(TabLayout.margin(side, anchor)) && (hidden == null || !hidden.getAsBoolean());
    }

    /**
     * Places every tab on the visible sides at its current animated size, and points each
     * animation at its effective state. Tabs on hidden sides head closed and are left out.
     */
    private List<Placed> layout() {
        List<Placed> placed = new ArrayList<>();
        if (!attached) {
            return placed;
        }
        for (TabSide side : TabSide.values()) {
            List<Entry> entries = tabs.get(side);
            if (entries.isEmpty()) {
                continue;
            }
            if (!sideVisible(side)) {
                for (Entry entry : entries) {
                    entry.animation.setTarget(false);
                }
                continue;
            }
            int margin = TabLayout.margin(side, anchor);
            String intended = openIntent.get(side);
            int y = TabLayout.stackTop(anchor);
            for (int i = 0; i < entries.size(); i++) {
                Entry entry = entries.get(i);
                Measure measure = measure(entry.tab, margin, i);
                boolean open = measure.fits && entry.tab.id().equals(intended);
                entry.animation.setTarget(open);
                double progress = entry.animation.progress();
                double eased = TabAnimation.ease(progress);
                int w = TabLayout.lerp(COLLAPSED, Math.max(COLLAPSED, measure.width), eased);
                int h = TabLayout.lerp(COLLAPSED, Math.max(COLLAPSED, measure.height), eased);
                TabRect rect = new TabRect(TabLayout.tabX(side, anchor, w, TabLayout.overlap(eased)), y, w, h);
                placed.add(new Placed(entry, rect, measure, open, open && progress == 1.0));
                y += h + GAP;
            }
        }
        return placed;
    }

    /** Open size of the {@code index}-th tab on a side, and whether it fits with the tabs above collapsed. */
    private Measure measure(SideTab tab, int margin, int index) {
        int width = TabLayout.openWidth(tab.preferredWidth(), margin);
        int contentWidth = width - 2 * CONTENT_PAD;
        int minWidth = Math.max(tab.minOpenWidth(), TITLE_X + MIN_TITLE_WIDTH + CONTENT_PAD);
        if (width < minWidth || contentWidth <= 0) {
            return new Measure(width, COLLAPSED, contentWidth, false);
        }
        int height = HEADER + tab.contentHeight(text, contentWidth) + BOTTOM_PAD;
        int openTop = TabLayout.stackTop(anchor) + index * (COLLAPSED + GAP);
        boolean fits = openTop + height <= TabLayout.bottomLimit(anchor);
        return new Measure(width, height, contentWidth, fits);
    }

    private void draw(TabCanvas canvas, Placed placed, int mouseX, int mouseY) {
        SideTab tab = placed.entry.tab;
        TabRect rect = placed.rect;
        canvas.panel(tab.side(), rect.x(), rect.y(), rect.w(), rect.h(), tab.tint());
        canvas.sprite(tab.icon(), rect.x() + ICON_INSET, rect.y() + ICON_INSET, ICON_SIZE, ICON_SIZE);
        if (!placed.fullyOpen) {
            return;
        }
        int maxTitleWidth = rect.w() - TITLE_X - CONTENT_PAD;
        canvas.text(text.ellipsize(tab.title(), maxTitleWidth), rect.x() + TITLE_X, rect.y() + TITLE_Y, TITLE_COLOR, true);
        tab.drawContent(canvas, rect.x() + CONTENT_PAD, rect.y() + HEADER, placed.measure.contentWidth, mouseX, mouseY);
    }

    private List<Component> tooltip(Placed placed, int mouseX, int mouseY) {
        SideTab tab = placed.entry.tab;
        if (!placed.open) {
            List<Component> lines = new ArrayList<>();
            lines.add(tab.title());
            lines.addAll(tab.statusLines());
            if (!placed.measure.fits) {
                lines.add(noRoomLine.get());
            }
            return lines;
        }
        if (placed.fullyOpen && mouseY - placed.rect.y() >= HEADER) {
            return tab.contentTooltip(mouseX - (placed.rect.x() + CONTENT_PAD), mouseY - (placed.rect.y() + HEADER));
        }
        return List.of();
    }

    private static @Nullable Placed hit(List<Placed> placed, double mouseX, double mouseY) {
        for (Placed candidate : placed) {
            if (candidate.rect.contains(mouseX, mouseY)) {
                return candidate;
            }
        }
        return null;
    }

    private @Nullable Placed find(String id) {
        for (Placed placed : layout()) {
            if (placed.entry.tab.id().equals(id)) {
                return placed;
            }
        }
        return null;
    }

    private record Entry(SideTab tab, TabAnimation animation) {
    }

    private record Measure(int width, int height, int contentWidth, boolean fits) {
    }

    private record Placed(Entry entry, TabRect rect, Measure measure, boolean open, boolean fullyOpen) {
    }
}
