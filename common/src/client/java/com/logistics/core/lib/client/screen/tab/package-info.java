/**
 * Side tabs: colored panels stacked down the outer edges of a container screen that slide open to
 * show secondary information or settings.
 *
 * <h2>Pieces</h2>
 * <ul>
 *   <li>{@link com.logistics.core.lib.client.screen.tab.SideTabHost} — the controller: layout,
 *       accordion (one open tab per side), animation, hit-testing and remembered open state. Pure
 *       Java apart from {@code Component}; unit-tested with a fake clock and canvas.</li>
 *   <li>{@link com.logistics.core.lib.client.screen.tab.SideTab} — one tab: identity, look and
 *       open-state content. Standard tabs live in {@code tabs/}.</li>
 *   <li>{@link com.logistics.core.lib.client.screen.tab.SideTabsWidget} — the only class that
 *       touches Minecraft's render and input APIs; the screen adds it as a widget.</li>
 *   <li>{@link com.logistics.core.lib.client.screen.tab.MachineTabs} — the standard Info, Energy and
 *       Redstone Control sets for machine and engine screens.</li>
 *   <li>{@link com.logistics.core.lib.client.screen.tab.SideTabScreen} — marks a screen that carries
 *       tabs; {@code core.lib.jei.SideTabsJeiPlugin} reads it to keep JEI's item list clear of
 *       them.</li>
 * </ul>
 *
 * <h2>Adding tabs to a screen</h2>
 * <pre>{@code
 * public class FooScreen extends AbstractContainerScreen<FooMenu> implements SideTabScreen {
 *     private final SideTabHost tabs;
 *
 *     public FooScreen(FooMenu menu, Inventory inventory, Component title) {
 *         super(menu, inventory, title);
 *         this.tabs = MachineTabs.machine("foo", menu, menu.controls(),
 *                 new TabAnchor(() -> leftPos, () -> topPos, () -> imageWidth, () -> width, () -> height));
 *     }
 *
 *     @Override
 *     protected void init() {
 *         super.init();
 *         addRenderableWidget(tabs.attach(SideTabsWidget::new));
 *     }
 *
 *     @Override
 *     protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
 *         return tabs.clickedOutside(super.hasClickedOutside(mouseX, mouseY, left, top), mouseX, mouseY);
 *     }
 *
 *     @Override
 *     public SideTabHost sideTabs() {
 *         return tabs;
 *     }
 * }
 * }</pre>
 * <ul>
 *   <li><b>Build the host in the constructor, attach in {@code init()}.</b> A resize or GUI-scale
 *       change reuses the screen and calls {@code clearWidgets()} then {@code init()} again; a host
 *       built in {@code init()} would lose its open state and animation each time.</li>
 *   <li><b>{@code hasClickedOutside} is required.</b> Vanilla's {@code mouseReleased} never asks
 *       widgets first, so without the override, releasing a carried item over a tab drops it into
 *       the world.</li>
 *   <li><b>Recipe-book screens</b> must hide the left side while the book is open, and the book
 *       component is a private field of {@code AbstractRecipeBookScreen}. Capture it through a
 *       private constructor, as {@code MaceratorScreen} does, then add
 *       {@code .hideSideWhen(TabSide.LEFT, book::isVisible)}.</li>
 *   <li>The key passed to {@code MachineTabs} names the info text
 *       ({@code gui.logistics.tab.info.<key>}) and the {@link
 *       com.logistics.core.lib.client.screen.tab.TabMemory} entry, so keep it stable.</li>
 * </ul>
 *
 * <h3>The menu side</h3>
 * {@code MachineTabs} needs the menu's {@link com.logistics.core.machine.control.MachineControls}:
 * <ul>
 *   <li>the block entity implements {@code MachineStatus} ({@code MachineEntity} and
 *       {@code EngineEntity} already do) and passes {@code MachineControls.server(this)} to the
 *       menu; the client constructor passes {@code MachineControls.client()};</li>
 *   <li>the menu calls {@code checkContainerDataCount(controls.data(), MachineControls.COUNT)} and
 *       {@code addDataSlots(controls.data())} <i>after</i> its existing data block, so existing
 *       indices never move;</li>
 *   <li>{@code clickMenuButton} tries {@code controls.clickMenuButton(player, id)} first.</li>
 * </ul>
 *
 * <h2>Writing a new tab</h2>
 * Subclass {@link com.logistics.core.lib.client.screen.tab.SideTab}; {@code InfoTab} is the
 * smallest example. Pick a stable {@code id()}, a tint (multiplied onto the grayscale panel), a 16×16
 * icon under {@code textures/gui/sprites/tab/}, and a {@code minOpenWidth()} the content still works
 * at. Content is drawn only while the tab is fully open, through
 * {@link com.logistics.core.lib.client.screen.tab.TabCanvas}, in coordinates relative to the content
 * origin.
 * <p>
 * A tab that changes server state sends a menu button through an {@code IntPredicate} sender, as
 * {@code RedstoneTab} does: the client menu validates the click without changing anything, and only
 * an accepted click goes to the server, which validates it again. Button ids
 * {@code MachineControls.BUTTON_BASE} to {@code BUTTON_BASE + BUTTON_RANGE - 1} (100–127) are
 * reserved for machine controls; give a new control a sub-range there. New synced values extend
 * {@code MachineControls} rather than a menu's own data block.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li><b>Tabs never overlap slots or labels.</b> Widgets draw below slots, and
 *       {@code getHoveredSlot} ignores widgets, so a slot under a tab would still be hovered and
 *       clicked. Tabs sit flush against the GUI's outer edge with no overlap.</li>
 *   <li><b>Compare mouse buttons against {@code InputConstants.MOUSE_BUTTON_LEFT}</b>, never a
 *       literal: MC 26.3 numbers buttons from 1, older versions from 0. The host only sees a
 *       {@code primary} flag.</li>
 *   <li><b>Small screens are allowed to lose tabs.</b> A side with less than {@code COLLAPSED}
 *       pixels of margin is hidden, and a tab that cannot fit its {@code minOpenWidth()} or content
 *       stays collapsed with a "not enough room" tooltip. Hidden areas fall back to vanilla click
 *       handling.</li>
 *   <li>The host is interactive, and reports JEI exclusion areas, only if it was drawn within the
 *       last {@code INTERACTIVE_WINDOW_MS}. That covers the narrow recipe-book overlay, which skips
 *       drawing widgets.</li>
 * </ul>
 *
 * <h2>Tests to extend</h2>
 * <ul>
 *   <li>{@code SideTabScreenContractTest#screens()} — add the screen: checks it implements
 *       {@code SideTabScreen}, overrides {@code hasClickedOutside}, and that no tab rect meets a slot.
 *       {@code SideTabTranslationTest} reads the same list for the info keys.</li>
 *   <li>The Fabric client gametests ({@code MachineSideTabsClientGameTest},
 *       {@code EngineSideTabsClientGameTest}) — add the screen to their lists to open it in a real
 *       client.</li>
 *   <li>A new tab gets its own JUnit test against {@code TabFixtures}, like {@code RedstoneTabTest},
 *       and adds its translation keys to {@code SideTabTranslationTest}.</li>
 * </ul>
 *
 * <h2>Porting to older Minecraft versions</h2>
 * Everything except the following is version-neutral:
 * <ul>
 *   <li>{@code SideTabsWidget}: the render override ({@code extractRenderState} on 26.x,
 *       {@code render(GuiGraphics…)} on 1.21.x), {@code mouseClicked}'s signature, sprite tinting
 *       (via {@code setColor} on 1.21.1), the tooltip call and the click sound.</li>
 *   <li>Each screen's {@code hasClickedOutside}, which takes an extra argument on 1.21.1.</li>
 *   <li>The recipe-book capture, which does not apply on 1.21.1.</li>
 *   <li>On 1.21.1, {@code ContainerEventHandler.mouseClicked} returns true only when a child consumes
 *       the click, so the widget must consume every click inside a tab.</li>
 * </ul>
 */
package com.logistics.core.lib.client.screen.tab;
