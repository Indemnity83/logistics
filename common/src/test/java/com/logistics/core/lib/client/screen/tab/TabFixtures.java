package com.logistics.core.lib.client.screen.tab;

import com.logistics.core.lib.resource.ResourceId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import net.minecraft.network.chat.Component;

/** Fakes shared by the side-tab tests. */
final class TabFixtures {
    static final int IMAGE_WIDTH = 176;
    static final int IMAGE_HEIGHT = 166;

    private TabFixtures() {
    }

    /** Ellipsizes {@code plain} with every character, "…" included, 6px wide. */
    static Component ellipsizeAt6px(String plain, Component original, int maxWidth) {
        if (plain.length() * 6 <= maxWidth) {
            return original;
        }
        int keep = Math.min(plain.length(), Math.max(0, (maxWidth - 6) / 6));
        return Component.literal(plain.substring(0, keep) + "\u2026");
    }

    static final class FakeClock implements LongSupplier {
        long now = 1_000L;

        @Override
        public long getAsLong() {
            return now;
        }

        void advance(long ms) {
            now += ms;
        }
    }

    /** Every character is 6px wide; lines are 9px. */
    static final class FakeText implements TabText {
        @Override
        public int width(Component text) {
            return text.getString().length() * 6;
        }

        @Override
        public int lineHeight() {
            return 9;
        }

        @Override
        public int wrappedLineCount(Component text, int maxWidth) {
            return Math.max(1, (width(text) + maxWidth - 1) / maxWidth);
        }

        @Override
        public Component ellipsize(Component text, int maxWidth) {
            return ellipsizeAt6px(text.getString(), text, maxWidth);
        }
    }

    /** A screen whose GUI image is centered, as vanilla places it. */
    static final class MutableAnchor {
        int left;
        int top;
        int imageWidth = IMAGE_WIDTH;
        int screenWidth;
        int screenHeight;

        MutableAnchor resize(int width, int height) {
            screenWidth = width;
            screenHeight = height;
            left = (width - imageWidth) / 2;
            top = (height - IMAGE_HEIGHT) / 2;
            return this;
        }

        TabAnchor anchor() {
            return new TabAnchor(() -> left, () -> top, () -> imageWidth, () -> screenWidth, () -> screenHeight);
        }
    }

    record PanelCall(TabSide side, TabRect rect, int tint) {
    }

    record FillCall(int x0, int y0, int x1, int y1, int argb) {}

    record SpriteCall(ResourceId sprite, int x, int y, int w, int h) {
    }

    record TextCall(Component text, int x, int y, int argb, boolean shadow) {
    }

    record TooltipCall(List<Component> lines, int x, int y) {
    }

    record WrappedCall(Component text, int x, int y, int maxWidth, int argb, boolean shadow) {
    }

    static final class RecordingCanvas implements TabCanvas {
        final List<PanelCall> panels = new ArrayList<>();
        final List<SpriteCall> sprites = new ArrayList<>();
        final List<FillCall> fills = new ArrayList<>();
        final List<TextCall> texts = new ArrayList<>();
        final List<TooltipCall> tooltips = new ArrayList<>();
        final List<WrappedCall> wrapped = new ArrayList<>();
        int clickSounds;
        private final TabText text;

        RecordingCanvas() {
            this(new FakeText());
        }

        /** A canvas that measures wrapped text with {@code text}. */
        RecordingCanvas(TabText text) {
            this.text = text;
        }

        @Override
        public void panel(TabSide side, int x, int y, int w, int h, int argbTint) {
            panels.add(new PanelCall(side, new TabRect(x, y, w, h), argbTint));
        }

        @Override
        public void sprite(ResourceId sprite, int x, int y, int w, int h) {
            sprites.add(new SpriteCall(sprite, x, y, w, h));
        }

        @Override
        public void sprite(ResourceId sprite, int x, int y, int w, int h, int argbTint) {
            sprites.add(new SpriteCall(sprite, x, y, w, h));
        }

        @Override
        public void fill(int x0, int y0, int x1, int y1, int argb) {
            fills.add(new FillCall(x0, y0, x1, y1, argb));
        }

        @Override
        public void text(Component text, int x, int y, int argb, boolean shadow) {
            texts.add(new TextCall(text, x, y, argb, shadow));
        }

        @Override
        public int wrappedText(Component text, int x, int y, int maxWidth, int argb, boolean shadow) {
            wrapped.add(new WrappedCall(text, x, y, maxWidth, argb, shadow));
            return this.text.wrappedLineCount(text, maxWidth) * this.text.lineHeight();
        }

        @Override
        public void tooltip(List<Component> lines, int mouseX, int mouseY) {
            tooltips.add(new TooltipCall(List.copyOf(lines), mouseX, mouseY));
        }

        @Override
        public void clickSound() {
            clickSounds++;
        }

        @Override
        public TabText text() {
            return text;
        }

        void reset() {
            panels.clear();
            sprites.clear();
            texts.clear();
            tooltips.clear();
            wrapped.clear();
            clickSounds = 0;
        }
    }

    record ContentDraw(int x, int y, int contentWidth) {
    }

    /** A tab with fixed content height that records what the host asks of it. */
    static final class TestTab extends SideTab {
        final String id;
        final TabSide side;
        String title;
        int preferredWidth = 100;
        int minOpenWidth = 60;
        int contentHeight = 40;
        List<Component> statusLines = List.of();
        List<Component> contentTooltip = List.of();
        boolean clickResult = true;
        final List<ContentDraw> draws = new ArrayList<>();
        final List<double[]> clicks = new ArrayList<>();
        final List<Integer> measuredWidths = new ArrayList<>();

        TestTab(String id, TabSide side) {
            this.id = id;
            this.side = side;
            this.title = id + " title";
        }

        TestTab title(String title) {
            this.title = title;
            return this;
        }

        TestTab size(int preferredWidth, int minOpenWidth, int contentHeight) {
            this.preferredWidth = preferredWidth;
            this.minOpenWidth = minOpenWidth;
            this.contentHeight = contentHeight;
            return this;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public TabSide side() {
            return side;
        }

        @Override
        public int tint() {
            return 0xFF112233;
        }

        @Override
        public ResourceId icon() {
            return ResourceId.in("logistics", "tab/" + id);
        }

        @Override
        public Component title() {
            return Component.literal(title);
        }

        @Override
        public List<Component> statusLines() {
            return statusLines;
        }

        @Override
        public int preferredWidth() {
            return preferredWidth;
        }

        @Override
        public int minOpenWidth() {
            return minOpenWidth;
        }

        @Override
        public int contentHeight(TabText text, int contentWidth) {
            measuredWidths.add(contentWidth);
            return contentHeight;
        }

        @Override
        public void drawContent(TabCanvas canvas, int x, int y, int contentWidth, int mouseX, int mouseY) {
            draws.add(new ContentDraw(x, y, contentWidth));
        }

        @Override
        public boolean clickContent(double localX, double localY) {
            clicks.add(new double[] {localX, localY});
            return clickResult;
        }

        @Override
        public List<Component> contentTooltip(double localX, double localY) {
            return contentTooltip;
        }
    }
}
