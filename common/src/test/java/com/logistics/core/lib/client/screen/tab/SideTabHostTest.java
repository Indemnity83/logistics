package com.logistics.core.lib.client.screen.tab;

import static com.logistics.core.lib.client.screen.tab.SideTabHost.COLLAPSED;
import static com.logistics.core.lib.client.screen.tab.SideTabHost.CONTENT_PAD;
import static com.logistics.core.lib.client.screen.tab.SideTabHost.GAP;
import static com.logistics.core.lib.client.screen.tab.SideTabHost.HEADER;
import static com.logistics.core.lib.client.screen.tab.SideTabHost.INTERACTIVE_WINDOW_MS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.logistics.core.lib.client.screen.tab.TabFixtures.ContentDraw;
import com.logistics.core.lib.client.screen.tab.TabFixtures.FakeClock;
import com.logistics.core.lib.client.screen.tab.TabFixtures.FakeText;
import com.logistics.core.lib.client.screen.tab.TabFixtures.MutableAnchor;
import com.logistics.core.lib.client.screen.tab.TabFixtures.RecordingCanvas;
import com.logistics.core.lib.client.screen.tab.TabFixtures.SpriteCall;
import com.logistics.core.lib.client.screen.tab.TabFixtures.TestTab;
import com.logistics.core.lib.client.screen.tab.TabFixtures.TextCall;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SideTabHost")
class SideTabHostTest {
    private static final String KEY = "test_screen";
    private static final long FULL = TabAnimation.DURATION_MS;

    /** 480×300 screen: GUI image at left 152, top 67; 150px of margin on each side. */
    private final MutableAnchor geometry = new MutableAnchor().resize(480, 300);
    private final FakeClock clock = new FakeClock();
    private final RecordingCanvas canvas = new RecordingCanvas();
    private final TestTab a = new TestTab("a", TabSide.RIGHT);
    private final TestTab b = new TestTab("b", TabSide.RIGHT);
    private final TestTab left = new TestTab("left", TabSide.LEFT);

    @BeforeEach
    @AfterEach
    void clearMemory() {
        TabMemory.clear();
    }

    private SideTabHost newHost() {
        return new SideTabHost(KEY, geometry.anchor(), new FakeText(), clock).add(a).add(b).add(left);
    }

    private SideTabHost attached() {
        SideTabHost host = newHost().attach();
        frame(host);
        return host;
    }

    private void frame(SideTabHost host) {
        host.render(canvas, -1000, -1000);
    }

    /** Advances past any animation and draws a frame, keeping the host interactive. */
    private void settle(SideTabHost host) {
        clock.advance(FULL);
        frame(host);
    }

    private static boolean clickCollapsed(SideTabHost host, String id) {
        TabRect rect = host.tabRect(id);
        return host.mouseClicked(rect.x() + 5, rect.y() + 5, true);
    }

    private static boolean clickHeader(SideTabHost host, String id) {
        TabRect rect = host.tabRect(id);
        return host.mouseClicked(rect.x() + rect.w() / 2.0, rect.y() + HEADER - 1, true);
    }

    private static boolean clickBody(SideTabHost host, String id) {
        TabRect rect = host.tabRect(id);
        return host.mouseClicked(rect.x() + CONTENT_PAD + 10, rect.y() + HEADER + 7, true);
    }

    @Nested
    @DisplayName("accordion and hit zones")
    class Accordion {
        @Test
        @DisplayName("opening a tab closes the other open tab on its side, not the other side")
        void accordion() {
            SideTabHost host = attached();
            clickCollapsed(host, "left");
            clickCollapsed(host, "a");
            settle(host);
            assertThat(host.isFullyOpen("a")).isTrue();
            assertThat(host.isFullyOpen("left")).isTrue();

            assertThat(clickCollapsed(host, "b")).isTrue();
            assertThat(host.openIntent(TabSide.RIGHT)).isEqualTo("b");
            assertThat(TabMemory.get(KEY, TabSide.RIGHT)).isEqualTo("b");
            settle(host);

            assertThat(host.isFullyOpen("b")).isTrue();
            assertThat(host.isFullyOpen("a")).isFalse();
            assertThat(host.tabRect("a")).isEqualTo(new TabRect(326, 71, COLLAPSED, COLLAPSED));
            assertThat(host.isFullyOpen("left")).isTrue();
        }

        @Test
        @DisplayName("the header strip collapses an open tab; the body routes to the content")
        void headerVersusBody() {
            SideTabHost host = attached();
            clickCollapsed(host, "a");
            settle(host);
            TabRect rect = host.tabRect("a");

            assertThat(clickBody(host, "a")).isTrue();
            assertThat(a.clicks).hasSize(1);
            assertThat(a.clicks.get(0)).containsExactly(10.0, 7.0);
            assertThat(host.openIntent(TabSide.RIGHT)).isEqualTo("a");

            assertThat(host.mouseClicked(rect.x() + 50, rect.y() + HEADER, true)).isTrue();
            assertThat(a.clicks).as("the first body row is content").hasSize(2);

            assertThat(clickHeader(host, "a")).isTrue();
            assertThat(a.clicks).hasSize(2);
            assertThat(host.openIntent(TabSide.RIGHT)).isNull();
            assertThat(TabMemory.get(KEY, TabSide.RIGHT)).isNull();
            settle(host);
            assertThat(host.tabRect("a").h()).isEqualTo(COLLAPSED);
        }

        @Test
        @DisplayName("the body of a tab that is still opening is consumed but not routed")
        void bodyWhileOpeningIsInert() {
            SideTabHost host = attached();
            clickCollapsed(host, "a");
            clock.advance(FULL - 20);
            frame(host);

            assertThat(clickBody(host, "a")).isTrue();
            assertThat(a.clicks).isEmpty();
            assertThat(host.openIntent(TabSide.RIGHT)).isEqualTo("a");
        }

        @Test
        @DisplayName("hit rects follow the animated size, and tabs below move with them")
        void hitRectsFollowAnimation() {
            SideTabHost host = attached();
            int collapsedBY = host.tabRect("b").y();
            clickCollapsed(host, "a");
            clock.advance(FULL / 2);
            frame(host);
            TabRect mid = host.tabRect("a");

            assertThat(mid.w()).isStrictlyBetween(COLLAPSED, 100);
            assertThat(host.isMouseOver(mid.right() - 1, mid.y() + 1)).isTrue();
            assertThat(host.isMouseOver(mid.right(), mid.y() + 1)).isFalse();
            assertThat(host.isMouseOver(326 + 99, 72)).as("inside the final rect only").isFalse();
            assertThat(host.tabRect("b").y()).isEqualTo(mid.bottom() + GAP).isGreaterThan(collapsedBY);
            assertThat(host.exclusionAreas()).contains(mid, host.tabRect("b"));
        }

        @Test
        @DisplayName("any mouse button inside a tab is consumed; only primary acts")
        void secondaryClicksConsumedWithoutAction() {
            SideTabHost host = attached();
            TabRect rect = host.tabRect("a");

            assertThat(host.mouseClicked(rect.x() + 5, rect.y() + 5, false)).isTrue();
            assertThat(host.openIntent(TabSide.RIGHT)).isNull();
            assertThat(host.anyAnimating()).isFalse();
            assertThat(host.mouseClicked(10, 10, false)).isFalse();
            assertThat(host.mouseClicked(10, 10, true)).isFalse();
        }

        @Test
        @DisplayName("a click on a tab with no room is consumed and changes nothing")
        void nonFittingClickConsumed() {
            a.minOpenWidth = 200;
            SideTabHost host = attached();
            canvas.reset();

            assertThat(clickCollapsed(host, "a")).isTrue();
            assertThat(host.openIntent(TabSide.RIGHT)).isNull();
            assertThat(TabMemory.get(KEY, TabSide.RIGHT)).isNull();
            assertThat(host.anyAnimating()).isFalse();
            frame(host);
            assertThat(canvas.clickSounds).isZero();
        }

        @Test
        @DisplayName("toggles and acting content clicks play the click sound on the next frame")
        void clickSound() {
            SideTabHost host = attached();
            canvas.reset();
            clickCollapsed(host, "a");
            frame(host);
            assertThat(canvas.clickSounds).isEqualTo(1);

            settle(host);
            canvas.reset();
            a.clickResult = false;
            clickBody(host, "a");
            frame(host);
            assertThat(canvas.clickSounds).isZero();

            a.clickResult = true;
            clickBody(host, "a");
            frame(host);
            assertThat(canvas.clickSounds).isEqualTo(1);
        }

        @Test
        @DisplayName("duplicate tab ids are rejected")
        void duplicateIds() {
            SideTabHost host = newHost();
            assertThatThrownBy(() -> host.add(new TestTab("a", TabSide.LEFT)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("intent versus effective state")
    class IntentVersusEffective {
        @Test
        @DisplayName("zero geometry before attach evaluates nothing and keeps the remembered intent")
        void zeroGeometryBeforeAttach() {
            TabMemory.set(KEY, TabSide.RIGHT, "a");
            MutableAnchor zero = new MutableAnchor();
            zero.imageWidth = 0;
            SideTabHost host = new SideTabHost(KEY, zero.anchor(), new FakeText(), clock).add(a).add(b);

            host.render(canvas, 0, 0);
            assertThat(canvas.panels).isEmpty();
            assertThat(a.measuredWidths).isEmpty();
            assertThat(host.isMouseOver(0, 0)).isFalse();
            assertThat(host.mouseClicked(0, 0, true)).isFalse();
            assertThat(host.exclusionAreas()).isEmpty();
            assertThat(host.isFullyOpen("a")).isFalse();

            host.attach();
            assertThat(host.tabRect("a")).as("zero margins hide the side").isNull();
            assertThat(host.openIntent(TabSide.RIGHT)).isEqualTo("a");
            assertThat(TabMemory.get(KEY, TabSide.RIGHT)).isEqualTo("a");

            zero.resize(480, 300);
            host.attach();
            assertThat(host.isFullyOpen("a")).isTrue();
        }

        @Test
        @DisplayName("the first attach applies the remembered tab instantly")
        void firstAttachSnaps() {
            TabMemory.set(KEY, TabSide.RIGHT, "a");
            SideTabHost host = newHost().attach();

            assertThat(host.isFullyOpen("a")).isTrue();
            assertThat(host.anyAnimating()).isFalse();
            frame(host);
            assertThat(a.draws).hasSize(1);
        }

        @Test
        @DisplayName("shrinking collapses without clearing intent; growing reopens")
        void shrinkKeepsIntent() {
            TabMemory.set(KEY, TabSide.RIGHT, "a");
            SideTabHost host = attached();

            geometry.screenWidth = geometry.left + 176 + 2 + 40;
            host.attach();
            assertThat(host.isFullyOpen("a")).isFalse();
            assertThat(host.tabRect("a").w()).isEqualTo(COLLAPSED);
            assertThat(host.openIntent(TabSide.RIGHT)).isEqualTo("a");
            assertThat(TabMemory.get(KEY, TabSide.RIGHT)).isEqualTo("a");

            geometry.resize(480, 300);
            host.attach();
            assertThat(host.isFullyOpen("a")).isTrue();
        }

        @Test
        @DisplayName("losing room between frames animates closed; regaining it animates open")
        void liveGeometryChangeAnimates() {
            TabMemory.set(KEY, TabSide.RIGHT, "a");
            SideTabHost host = attached();

            geometry.screenWidth = geometry.left + 176 + 2 + 40;
            assertThat(host.anyAnimating()).isTrue();
            settle(host);
            assertThat(host.tabRect("a").w()).isEqualTo(COLLAPSED);

            geometry.resize(480, 300);
            assertThat(host.anyAnimating()).isTrue();
            settle(host);
            assertThat(host.isFullyOpen("a")).isTrue();
        }

        @Test
        @DisplayName("a player collapse clears intent and memory, and growing does not reopen")
        void playerCollapseIsFinal() {
            TabMemory.set(KEY, TabSide.RIGHT, "a");
            SideTabHost host = attached();

            clickHeader(host, "a");
            assertThat(host.openIntent(TabSide.RIGHT)).isNull();
            assertThat(TabMemory.get(KEY, TabSide.RIGHT)).isNull();

            geometry.resize(400, 250);
            host.attach();
            geometry.resize(800, 600);
            host.attach();
            settle(host);
            assertThat(host.isFullyOpen("a")).isFalse();
            assertThat(host.tabRect("a").h()).isEqualTo(COLLAPSED);
        }

        @Test
        @DisplayName("a new host for the same screen key reopens the remembered tab")
        void memoryAcrossHosts() {
            SideTabHost first = attached();
            clickCollapsed(first, "b");

            SideTabHost second = newHost().attach();
            assertThat(second.isFullyOpen("b")).isTrue();
            assertThat(new SideTabHost("other", geometry.anchor(), new FakeText(), clock)
                    .openIntent(TabSide.RIGHT)).isNull();
        }
    }

    @Nested
    @DisplayName("frame stamp and attach")
    class FrameStampAndAttach {
        @Test
        @DisplayName("the host is interactive only within the window after a render")
        void frameStampGating() {
            SideTabHost host = newHost().attach();
            TabRect rect = host.tabRect("a");
            double x = rect.x() + 5;
            double y = rect.y() + 5;

            assertThat(host.isMouseOver(x, y)).isFalse();
            assertThat(host.exclusionAreas()).isEmpty();
            assertThat(host.envelope()).isEmpty();
            assertThat(host.mouseClicked(x, y, true)).isFalse();

            frame(host);
            assertThat(host.isMouseOver(x, y)).isTrue();
            assertThat(host.exclusionAreas()).hasSize(3);

            clock.advance(INTERACTIVE_WINDOW_MS);
            assertThat(host.isMouseOver(x, y)).isTrue();

            clock.advance(1);
            assertThat(host.isMouseOver(x, y)).isFalse();
            assertThat(host.exclusionAreas()).isEmpty();
            assertThat(host.envelope()).isEmpty();
            assertThat(host.mouseClicked(x, y, true)).isFalse();
            assertThat(host.openIntent(TabSide.RIGHT)).isNull();
        }

        @Test
        @DisplayName("repeated attach returns the same view and keeps the open state")
        void repeatedAttach() {
            SideTabHost host = newHost();
            AtomicInteger created = new AtomicInteger();
            Object view = host.attach(h -> {
                created.incrementAndGet();
                return new Object();
            });
            frame(host);
            clickCollapsed(host, "a");
            settle(host);

            for (int i = 0; i < 3; i++) {
                assertThat(host.<Object>attach(h -> new Object())).isSameAs(view);
            }
            assertThat(created).hasValue(1);
            assertThat(host.isFullyOpen("a")).isTrue();

            canvas.reset();
            frame(host);
            assertThat(canvas.panels).hasSize(3);
        }

        @Test
        @DisplayName("attach mid-animation snaps to the target")
        void attachSnapsMidAnimation() {
            SideTabHost host = attached();
            clickCollapsed(host, "a");
            clock.advance(30);
            host.attach();
            assertThat(host.isFullyOpen("a")).isTrue();
            assertThat(host.anyAnimating()).isFalse();
        }
    }

    @Nested
    @DisplayName("clickedOutside, hidden sides and the envelope")
    class OutsideAndEnvelope {
        @Test
        @DisplayName("tab rects count as inside; elsewhere the vanilla verdict stands")
        void clickedOutside() {
            SideTabHost host = attached();
            TabRect rect = host.tabRect("a");

            assertThat(host.clickedOutside(true, rect.x() + 5, rect.y() + 5)).isFalse();
            assertThat(host.clickedOutside(false, rect.x() + 5, rect.y() + 5)).isFalse();
            assertThat(host.clickedOutside(true, 5, 5)).isTrue();
            assertThat(host.clickedOutside(false, 5, 5)).isFalse();
        }

        @Test
        @DisplayName("a hidden side is not drawn or hit-tested and reverts to vanilla")
        void hiddenSide() {
            AtomicBoolean bookOpen = new AtomicBoolean();
            TabMemory.set(KEY, TabSide.LEFT, "left");
            SideTabHost host = newHost().hideSideWhen(TabSide.LEFT, bookOpen::get).attach();
            frame(host);
            TabRect rect = host.tabRect("left");
            double x = rect.x() + 5;
            double y = rect.y() + 5;
            assertThat(host.clickedOutside(true, x, y)).isFalse();

            bookOpen.set(true);
            canvas.reset();
            frame(host);
            assertThat(canvas.panels).extracting(p -> p.side()).containsOnly(TabSide.RIGHT);
            assertThat(host.tabRect("left")).isNull();
            assertThat(host.clickedOutside(true, x, y)).isTrue();
            assertThat(host.mouseClicked(x, y, true)).isFalse();
            assertThat(host.exclusionAreas()).hasSize(2);
            assertThat(host.envelope()).hasSize(1);
            assertThat(host.openIntent(TabSide.LEFT)).isEqualTo("left");
            clock.advance(FULL);
            frame(host);

            bookOpen.set(false);
            assertThat(host.anyAnimating()).as("reappearing animates open").isTrue();
            settle(host);
            assertThat(host.isFullyOpen("left")).isTrue();
        }

        @Test
        @DisplayName("hideSideWhen conditions combine with OR")
        void hideConditionsCombine() {
            AtomicBoolean first = new AtomicBoolean();
            AtomicBoolean second = new AtomicBoolean();
            SideTabHost host = newHost()
                    .hideSideWhen(TabSide.LEFT, first::get)
                    .hideSideWhen(TabSide.LEFT, second::get)
                    .attach();

            assertThat(host.tabRect("left")).isNotNull();
            second.set(true);
            assertThat(host.tabRect("left")).isNull();
            second.set(false);
            first.set(true);
            assertThat(host.tabRect("left")).isNull();
        }

        @Test
        @DisplayName("the envelope bounds the largest reachable stack and is stable while animating")
        void envelopeStable() {
            b.preferredWidth = 90;
            b.contentHeight = 80;
            SideTabHost host = attached();
            int collapsedStack = 2 * COLLAPSED + GAP;
            TabRect right = new TabRect(326, 71, 100, collapsedStack + (HEADER + 80 + 6) - COLLAPSED);
            TabRect leftEnvelope = new TabRect(154 - 100, 71, 100, HEADER + 40 + 6);
            assertThat(host.envelope()).containsExactly(leftEnvelope, right);

            clickCollapsed(host, "a");
            for (int t = 0; t < FULL; t += 15) {
                clock.advance(15);
                frame(host);
                assertThat(host.envelope()).containsExactly(leftEnvelope, right);
                for (TabRect area : host.exclusionAreas()) {
                    assertThat(host.envelope()).anySatisfy(env -> assertThat(
                            env.x() <= area.x() && env.y() <= area.y()
                                    && env.right() >= area.right() && env.bottom() >= area.bottom()).isTrue());
                }
            }
            clickCollapsed(host, "b");
            clock.advance(40);
            frame(host);
            assertThat(host.envelope()).containsExactly(leftEnvelope, right);
        }

        @Test
        @DisplayName("tabs that cannot open do not widen the envelope")
        void envelopeIgnoresNonFitting() {
            a.minOpenWidth = 500;
            b.minOpenWidth = 500;
            SideTabHost host = attached();
            assertThat(host.envelope()).contains(new TabRect(326, 71, COLLAPSED, 2 * COLLAPSED + GAP));
        }
    }

    @Nested
    @DisplayName("drawing and tooltips")
    class DrawingAndTooltips {
        @Test
        @DisplayName("content and title are drawn only once the tab is fully open")
        void contentOnlyWhenFullyOpen() {
            SideTabHost host = attached();
            clickCollapsed(host, "a");
            for (int t = 0; t < FULL - 10; t += 10) {
                clock.advance(10);
                frame(host);
            }
            assertThat(a.draws).isEmpty();
            assertThat(canvas.texts).isEmpty();

            clock.advance(10);
            canvas.reset();
            frame(host);
            TabRect rect = host.tabRect("a");
            assertThat(a.draws).containsExactly(
                    new ContentDraw(rect.x() + CONTENT_PAD, rect.y() + HEADER, 100 - 2 * CONTENT_PAD));
            assertThat(canvas.texts).containsExactly(new TextCall(
                    a.title(), rect.x() + 24, rect.y() + 8, SideTabHost.TITLE_COLOR, true));

            clickHeader(host, "a");
            clock.advance(1);
            frame(host);
            assertThat(a.draws).hasSize(1);
        }

        @Test
        @DisplayName("panels carry the tint; icons sit by the outer corner; left titles sit at the pad")
        void panelsAndIcons() {
            TabMemory.set(KEY, TabSide.LEFT, "left");
            SideTabHost host = attached();
            TabRect right = host.tabRect("a");
            TabRect leftRect = host.tabRect("left");

            assertThat(canvas.panels).extracting(p -> p.tint()).containsOnly(0xFF112233);
            assertThat(canvas.sprites).contains(
                    new SpriteCall(a.icon(), right.x() + 4, right.y() + 4, 16, 16),
                    new SpriteCall(left.icon(), leftRect.x() + 4, leftRect.y() + 4, 16, 16));
            assertThat(canvas.texts).extracting(TextCall::x).containsExactly(leftRect.x() + SideTabHost.TITLE_X);
        }

        @Test
        @DisplayName("a collapsed tab's tooltip is its title and status lines")
        void collapsedTooltip() {
            a.statusLines = List.of(Component.literal("Idle"));
            SideTabHost host = attached();
            TabRect rect = host.tabRect("a");
            canvas.reset();
            host.render(canvas, rect.x() + 5, rect.y() + 5);

            assertThat(canvas.tooltips).hasSize(1);
            assertThat(canvas.tooltips.get(0).lines()).containsExactly(a.title(), Component.literal("Idle"));
        }

        @Test
        @DisplayName("a tab with no room adds the no-room line to its tooltip")
        void noRoomTooltip() {
            a.minOpenWidth = 500;
            SideTabHost host = attached();
            TabRect rect = host.tabRect("a");
            canvas.reset();
            host.render(canvas, rect.x() + 5, rect.y() + 5);

            assertThat(canvas.tooltips.get(0).lines())
                    .containsExactly(a.title(), Component.translatable(SideTabHost.NO_ROOM_KEY));
        }

        @Test
        @DisplayName("a long title still opens at the width floor, drawn ellipsized after the icon on both sides; the tooltip keeps it whole")
        void longTitleEllipsized() {
            String longTitle = "Thermodynamics overview";
            int floor = SideTabHost.TITLE_X + SideTabHost.MIN_TITLE_WIDTH + CONTENT_PAD;
            a.title(longTitle).size(floor, 0, 40);
            left.title(longTitle).size(floor, 0, 40);
            SideTabHost host = attached();

            TabRect collapsed = host.tabRect("a");
            canvas.reset();
            host.render(canvas, collapsed.x() + 5, collapsed.y() + 5);
            assertThat(canvas.tooltips.get(0).lines()).containsExactly(Component.literal(longTitle));

            clickCollapsed(host, "a");
            clickCollapsed(host, "left");
            settle(host);
            assertThat(host.isFullyOpen("a")).isTrue();
            assertThat(host.isFullyOpen("left")).isTrue();
            TabRect right = host.tabRect("a");
            TabRect leftRect = host.tabRect("left");
            assertThat(right.w()).isEqualTo(floor);
            int maxTitleWidth = floor - SideTabHost.TITLE_X - CONTENT_PAD;
            FakeText text = new FakeText();
            assertThat(canvas.texts).hasSize(2).allSatisfy(call -> {
                assertThat(call.text().getString()).isEqualTo("Th\u2026");
                assertThat(text.width(call.text())).isLessThanOrEqualTo(maxTitleWidth);
            });
            assertThat(canvas.texts).extracting(TextCall::x)
                    .containsExactlyInAnyOrder(right.x() + SideTabHost.TITLE_X, leftRect.x() + SideTabHost.TITLE_X);
            assertThat(canvas.sprites).contains(
                    new SpriteCall(a.icon(), right.x() + SideTabHost.ICON_INSET, right.y() + SideTabHost.ICON_INSET, 16, 16),
                    new SpriteCall(left.icon(), leftRect.x() + SideTabHost.ICON_INSET, leftRect.y() + SideTabHost.ICON_INSET, 16, 16));
        }

        @Test
        @DisplayName("a tab narrower than the title floor cannot open, whatever its own minimum")
        void titleFloor() {
            int floor = SideTabHost.TITLE_X + SideTabHost.MIN_TITLE_WIDTH + CONTENT_PAD;
            a.size(floor - 1, 0, 40);
            SideTabHost host = attached();

            assertThat(clickCollapsed(host, "a")).isTrue();
            settle(host);
            assertThat(host.isFullyOpen("a")).isFalse();
        }

        @Test
        @DisplayName("a title that fits is drawn unchanged")
        void shortTitleUnchanged() {
            a.title("Hi");
            SideTabHost host = attached();
            clickCollapsed(host, "a");
            settle(host);

            assertThat(canvas.texts).extracting(TextCall::text).containsExactly(Component.literal("Hi"));
        }

        @Test
        @DisplayName("an open tab shows its content tooltip in the body and nothing on the header")
        void contentTooltip() {
            a.contentTooltip = List.of(Component.literal("Mode: High"));
            TabMemory.set(KEY, TabSide.RIGHT, "a");
            SideTabHost host = attached();
            TabRect rect = host.tabRect("a");

            canvas.reset();
            host.render(canvas, rect.x() + 20, rect.y() + HEADER + 4);
            assertThat(canvas.tooltips).hasSize(1);
            assertThat(canvas.tooltips.get(0).lines()).containsExactly(Component.literal("Mode: High"));

            canvas.reset();
            host.render(canvas, rect.x() + 20, rect.y() + 4);
            assertThat(canvas.tooltips).isEmpty();

            a.contentTooltip = List.of();
            canvas.reset();
            host.render(canvas, rect.x() + 20, rect.y() + HEADER + 4);
            assertThat(canvas.tooltips).isEmpty();
        }

        @Test
        @DisplayName("nothing is drawn before attach")
        void nothingBeforeAttach() {
            SideTabHost host = newHost();
            frame(host);
            assertThat(canvas.panels).isEmpty();
        }
    }

    @Nested
    @DisplayName("rapid toggling")
    class RapidToggling {
        @Test
        @DisplayName("rapid open/close clicks on one tab settle in the last-clicked state")
        void rapidSingleTab() {
            SideTabHost host = attached();
            for (int i = 0; i < 7; i++) {
                assertThat(clickCollapsed(host, "a")).isTrue();
                clock.advance(25);
                frame(host);
            }
            assertThat(host.openIntent(TabSide.RIGHT)).isEqualTo("a");
            settle(host);
            assertThat(host.isFullyOpen("a")).isTrue();
            assertThat(TabMemory.get(KEY, TabSide.RIGHT)).isEqualTo("a");

            for (int i = 0; i < 5; i++) {
                clickCollapsed(host, "a");
                clock.advance(25);
                frame(host);
            }
            assertThat(host.openIntent(TabSide.RIGHT)).isNull();
            settle(host);
            assertThat(host.isFullyOpen("a")).isFalse();
            assertThat(host.anyAnimating()).isFalse();
            assertThat(host.tabRect("a")).isEqualTo(new TabRect(326, 71, COLLAPSED, COLLAPSED));
        }

        @Test
        @DisplayName("alternating between tabs mid-animation leaves exactly one open")
        void rapidAlternation() {
            SideTabHost host = attached();
            String last = null;
            for (int i = 0; i < 8; i++) {
                last = i % 2 == 0 ? "a" : "b";
                assertThat(clickCollapsed(host, last)).isTrue();
                clock.advance(20);
                frame(host);
            }
            settle(host);
            assertThat(host.openIntent(TabSide.RIGHT)).isEqualTo(last);
            assertThat(host.isFullyOpen(last)).isTrue();
            assertThat(host.isFullyOpen("a".equals(last) ? "b" : "a")).isFalse();
            assertThat(host.anyAnimating()).isFalse();
        }
    }
}
