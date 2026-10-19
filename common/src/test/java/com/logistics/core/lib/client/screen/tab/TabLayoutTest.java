package com.logistics.core.lib.client.screen.tab;

import static com.logistics.core.lib.client.screen.tab.SideTabHost.COLLAPSED;
import static com.logistics.core.lib.client.screen.tab.SideTabHost.CONTENT_PAD;
import static com.logistics.core.lib.client.screen.tab.SideTabHost.GAP;
import static com.logistics.core.lib.client.screen.tab.SideTabHost.HEADER;
import static com.logistics.core.lib.client.screen.tab.SideTabHost.BOTTOM_PAD;
import static com.logistics.core.lib.client.screen.tab.SideTabHost.TOP_OFFSET;
import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.client.screen.tab.TabFixtures.FakeClock;
import com.logistics.core.lib.client.screen.tab.TabFixtures.FakeText;
import com.logistics.core.lib.client.screen.tab.TabFixtures.MutableAnchor;
import com.logistics.core.lib.client.screen.tab.TabFixtures.RecordingCanvas;
import com.logistics.core.lib.client.screen.tab.TabFixtures.TestTab;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Side tab layout")
class TabLayoutTest {

    /** 480×300 screen: GUI image at left 152, top 67; 150px of margin on each side. */
    private final MutableAnchor geometry = new MutableAnchor().resize(480, 300);
    private final FakeClock clock = new FakeClock();
    private final RecordingCanvas canvas = new RecordingCanvas();

    @BeforeEach
    @AfterEach
    void clearMemory() {
        TabMemory.clear();
    }

    private SideTabHost host(SideTab... tabs) {
        SideTabHost host = new SideTabHost("layout", geometry.anchor(), new FakeText(), clock);
        for (SideTab tab : tabs) {
            host.add(tab);
        }
        return host.attach();
    }

    @Nested
    @DisplayName("TabLayout math")
    class PureMath {
        @Test
        @DisplayName("margins measure the free space to each screen edge, less the screen margin")
        void margins() {
            assertThat(TabLayout.margin(TabSide.LEFT, geometry.anchor())).isEqualTo(150);
            assertThat(TabLayout.margin(TabSide.RIGHT, geometry.anchor())).isEqualTo(150);
            geometry.left = 100;
            assertThat(TabLayout.margin(TabSide.LEFT, geometry.anchor())).isEqualTo(98);
            assertThat(TabLayout.margin(TabSide.RIGHT, geometry.anchor())).isEqualTo(480 - 276 - 2);
        }

        @Test
        @DisplayName("a side fits exactly when a collapsed tab fits")
        void sideFitsBoundary() {
            assertThat(TabLayout.sideFits(COLLAPSED)).isTrue();
            assertThat(TabLayout.sideFits(COLLAPSED - 1)).isFalse();
        }

        @Test
        @DisplayName("right tabs start at the right edge; left tabs end at the left edge")
        void tabX() {
            assertThat(TabLayout.tabX(TabSide.RIGHT, geometry.anchor(), 24)).isEqualTo(152 + 176);
            assertThat(TabLayout.tabX(TabSide.RIGHT, geometry.anchor(), 100)).isEqualTo(152 + 176);
            assertThat(TabLayout.tabX(TabSide.LEFT, geometry.anchor(), 24)).isEqualTo(152 - 24);
            assertThat(TabLayout.tabX(TabSide.LEFT, geometry.anchor(), 100)).isEqualTo(152 - 100);
        }

        @Test
        @DisplayName("the open width is clamped to the margin")
        void openWidthClamp() {
            assertThat(TabLayout.openWidth(100, 150)).isEqualTo(100);
            assertThat(TabLayout.openWidth(100, 60)).isEqualTo(60);
        }
    }

    @Test
    @DisplayName("collapsed tabs stack down both outer edges from the top offset")
    void collapsedPlacement() {
        SideTabHost host = host(new TestTab("r1", TabSide.RIGHT), new TestTab("r2", TabSide.RIGHT),
                new TestTab("l1", TabSide.LEFT));
        int top = 67 + TOP_OFFSET;

        assertThat(host.tabRect("r1")).isEqualTo(new TabRect(328, top, COLLAPSED, COLLAPSED));
        assertThat(host.tabRect("r2")).isEqualTo(new TabRect(328, top + COLLAPSED + GAP, COLLAPSED, COLLAPSED));
        assertThat(host.tabRect("l1")).isEqualTo(new TabRect(128, top, COLLAPSED, COLLAPSED));
    }

    @Test
    @DisplayName("collapsed and open rect edges abut the GUI's outer edge exactly on both sides")
    void rectEdgesAbutGuiEdge() {
        SideTabHost host = host(new TestTab("r1", TabSide.RIGHT), new TestTab("l1", TabSide.LEFT));
        assertThat(host.tabRect("r1").x()).as("collapsed right").isEqualTo(152 + 176);
        assertThat(host.tabRect("l1").right()).as("collapsed left").isEqualTo(152);

        TabMemory.set("layout", TabSide.RIGHT, "r1");
        TabMemory.set("layout", TabSide.LEFT, "l1");
        host = host(new TestTab("r1", TabSide.RIGHT), new TestTab("l1", TabSide.LEFT));
        assertThat(host.isFullyOpen("r1")).isTrue();
        assertThat(host.isFullyOpen("l1")).isTrue();
        assertThat(host.tabRect("r1").x()).as("open right").isEqualTo(152 + 176);
        assertThat(host.tabRect("l1").right()).as("open left").isEqualTo(152);
    }

    @Test
    @DisplayName("tabs never intrude into the GUI image at any animation frame, on either side")
    void neverIntrudeWhileAnimating() {
        SideTabHost host = host(new TestTab("r1", TabSide.RIGHT), new TestTab("l1", TabSide.LEFT));
        TabRect image = new TabRect(152, 67, 176, 166);
        host.render(canvas, -1000, -1000);
        host.mouseClicked(330, 75, true);
        host.mouseClicked(140, 75, true);

        for (int t = 0; t <= TabAnimation.DURATION_MS; t += 10) {
            for (String id : new String[] {"r1", "l1"}) {
                TabRect rect = host.tabRect(id);
                assertThat(rect.intersects(image)).as(id + " opening at " + t + "ms: " + rect).isFalse();
            }
            assertThat(host.tabRect("r1").x()).isEqualTo(152 + 176);
            assertThat(host.tabRect("l1").right()).isEqualTo(152);
            clock.advance(10);
        }
        assertThat(host.isFullyOpen("r1")).isTrue();
        assertThat(host.isFullyOpen("l1")).isTrue();
    }

    @Test
    @DisplayName("open tabs grow outward from the GUI edge at their full open size")
    void openPlacement() {
        TabMemory.set("layout", TabSide.RIGHT, "r1");
        TabMemory.set("layout", TabSide.LEFT, "l1");
        TestTab right = new TestTab("r1", TabSide.RIGHT);
        TestTab left = new TestTab("l1", TabSide.LEFT);
        SideTabHost host = host(right, left);
        int height = HEADER + 40 + BOTTOM_PAD;

        assertThat(host.tabRect("r1")).isEqualTo(new TabRect(328, 71, 100, height));
        assertThat(host.tabRect("l1")).isEqualTo(new TabRect(152 - 100, 71, 100, height));
        assertThat(right.measuredWidths).contains(100 - 2 * CONTENT_PAD);
    }

    @Test
    @DisplayName("tabs below an opening tab slide down with its animated height")
    void stackingSlidesDuringAnimation() {
        SideTabHost host = host(new TestTab("a", TabSide.RIGHT), new TestTab("b", TabSide.RIGHT));
        host.render(canvas, -1000, -1000);
        host.mouseClicked(330, 75, true);

        int previousY = host.tabRect("b").y();
        assertThat(previousY).isEqualTo(71 + COLLAPSED + GAP);
        for (int t = 0; t < 180; t += 20) {
            clock.advance(20);
            TabRect a = host.tabRect("a");
            TabRect b = host.tabRect("b");
            assertThat(b.y()).isEqualTo(a.bottom() + GAP).isGreaterThanOrEqualTo(previousY);
            previousY = b.y();
        }
        assertThat(host.tabRect("b").y()).isEqualTo(71 + HEADER + 40 + BOTTOM_PAD + GAP);
    }

    @Test
    @DisplayName("mid-animation size is the eased interpolation for both width and height")
    void animatedSizeIsEasedLerp() {
        SideTabHost host = host(new TestTab("a", TabSide.RIGHT));
        host.render(canvas, -1000, -1000);
        host.mouseClicked(330, 75, true);
        clock.advance(90);

        TabRect rect = host.tabRect("a");
        assertThat(rect.w()).isEqualTo(TabLayout.lerp(COLLAPSED, 100, 0.875));
        assertThat(rect.h()).isEqualTo(TabLayout.lerp(COLLAPSED, 68, 0.875));
    }

    @Test
    @DisplayName("the open width is clamped to the available margin and content wraps at it")
    void widthClampedToMargin() {
        geometry.screenWidth = 152 + 176 + 62;
        TabMemory.set("layout", TabSide.RIGHT, "a");
        TestTab tab = new TestTab("a", TabSide.RIGHT).size(100, 50, 40);
        SideTabHost host = host(tab);

        assertThat(host.tabRect("a").w()).isEqualTo(60);
        assertThat(host.tabRect("a").right()).as("the open tab stops at the screen margin").isEqualTo(390 - 2);
        assertThat(host.isFullyOpen("a")).isTrue();
        assertThat(tab.measuredWidths).contains(60 - 2 * CONTENT_PAD);
    }

    @Test
    @DisplayName("a side whose margin cannot hold a collapsed tab is hidden entirely")
    void sideHiddenUnderMinimumMargin() {
        SideTabHost host = host(new TestTab("r", TabSide.RIGHT), new TestTab("l", TabSide.LEFT));

        geometry.screenWidth = 152 + 176 + 2 + COLLAPSED;
        assertThat(TabLayout.margin(TabSide.RIGHT, geometry.anchor())).isEqualTo(24);
        assertThat(host.tabRect("r")).as("visible at a margin of exactly 24").isNotNull();
        assertThat(host.tabRect("r").right()).isEqualTo(geometry.screenWidth - 2);

        geometry.screenWidth--;
        assertThat(TabLayout.margin(TabSide.RIGHT, geometry.anchor())).isEqualTo(23);
        assertThat(host.tabRect("r")).as("hidden at a margin of 23").isNull();
        assertThat(host.tabRect("l")).isNotNull();

        geometry.left = 2 + COLLAPSED;
        assertThat(TabLayout.margin(TabSide.LEFT, geometry.anchor())).isEqualTo(24);
        assertThat(host.tabRect("l")).as("left visible at a margin of exactly 24").isNotNull();
        assertThat(host.tabRect("l").x()).isEqualTo(2);
        geometry.left--;
        assertThat(host.tabRect("l")).as("left hidden at a margin of 23").isNull();
    }

    @Test
    @DisplayName("a tab narrower than its minimum open width cannot open")
    void tabTooNarrowCannotOpen() {
        geometry.screenWidth = 152 + 176 + 62;
        TabMemory.set("layout", TabSide.RIGHT, "a");
        SideTabHost host = host(new TestTab("a", TabSide.RIGHT).size(100, 76, 40));

        assertThat(host.isFullyOpen("a")).isFalse();
        assertThat(host.tabRect("a")).isEqualTo(new TabRect(328, 71, COLLAPSED, COLLAPSED));
    }

    @Test
    @DisplayName("a tab whose content would run past the screen bottom cannot open")
    void tabTooTallCannotOpen() {
        TabMemory.set("layout", TabSide.RIGHT, "b");
        TestTab b = new TestTab("b", TabSide.RIGHT);
        SideTabHost host = host(new TestTab("a", TabSide.RIGHT), b);
        int bTop = 71 + COLLAPSED + GAP;
        int limit = 300 - 2;

        b.contentHeight = limit - bTop - HEADER - BOTTOM_PAD;
        assertThat(host.isFullyOpen("b")).as("bottom exactly at the limit").isTrue();

        b.contentHeight++;
        host.attach();
        assertThat(host.isFullyOpen("b")).isFalse();
        assertThat(host.tabRect("b").h()).isEqualTo(COLLAPSED);
    }
}
