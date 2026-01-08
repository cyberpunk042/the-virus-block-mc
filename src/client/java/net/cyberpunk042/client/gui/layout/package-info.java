/**
 * GUI Layout System
 * 
 * <p>This package provides a flexible layout abstraction that enables the same panel code
 * to work in both windowed and fullscreen modes.</p>
 * 
 * <h2>Key Components</h2>
 * 
 * <h3>Core Types</h3>
 * <ul>
 *   <li>{@link net.cyberpunk042.client.gui.layout.Bounds} - Immutable rectangle with slicing/transformation methods</li>
 *   <li>{@link net.cyberpunk042.client.gui.layout.GuiMode} - Enum defining display modes (WINDOWED, FULLSCREEN)</li>
 *   <li>{@link net.cyberpunk042.client.gui.layout.LayoutManager} - Strategy interface for layout calculations</li>
 * </ul>
 * 
 * <h3>Layout Implementations</h3>
 * <ul>
 *   <li>{@link net.cyberpunk042.client.gui.layout.WindowedLayout} - Split HUD with game world visible</li>
 *   <li>{@link net.cyberpunk042.client.gui.layout.FullscreenLayout} - Immersive editor with 3D viewport</li>
 * </ul>
 * 
 * <h3>Containers</h3>
 * <ul>
 *   <li>{@link net.cyberpunk042.client.gui.layout.SidePanel} - Side panel container for windowed mode</li>
 *   <li>{@link net.cyberpunk042.client.gui.layout.StatusBar} - Status bar showing context info</li>
 * </ul>
 * 
 * <h3>Panel Architecture (V2)</h3>
 * <ul>
 *   <li>{@link net.cyberpunk042.client.gui.layout.LayoutPanel} - New-gen panel base using native widgets</li>
 * </ul>
 * 
 * <h2>Minecraft Native Widgets</h2>
 * <p>The V2 architecture leverages Minecraft's built-in layout widgets:</p>
 * <ul>
 *   <li>{@code GridWidget} - 2D grid layout with row/column positioning</li>
 *   <li>{@code DirectionalLayoutWidget} - Horizontal/vertical lists with spacing</li>
 *   <li>{@code Positioner} - Fluent API for alignment (margin, alignCenter, etc.)</li>
 *   <li>{@code ScrollableWidget} - Abstract base for scrollable content</li>
 * </ul>
 * 
 * <h2>Design Patterns</h2>
 * <ul>
 *   <li><b>Strategy Pattern</b>: LayoutManager implementations provide different positioning strategies</li>
 *   <li><b>Template Method</b>: AbstractPanel.init() → reflow() → render()</li>
 *   <li><b>Composite</b>: Panels contain sub-panels, all using bounds-relative positioning</li>
 *   <li><b>Factory</b>: LayoutFactory creates and caches layout managers</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // Get layout for current mode
 * LayoutManager layout = LayoutFactory.getAndCalculate(mode, width, height);
 * 
 * // Use layout bounds for positioning
 * panel.setBounds(layout.getContentBounds());
 * statusBar.setBounds(layout.getStatusBarBounds());
 * 
 * // Switch modes (Tab key)
 * setMode(mode.toggle());
 * }</pre>
 * 
 * <h2>Windowed Mode Layout</h2>
 * <pre>
 * ┌────────────────────────────────────────────────────────────┐
 * │  ┌──────────┐                            ┌──────────┐     │
 * │  │  LEFT    │                            │  RIGHT   │     │
 * │  │  PANEL   │      GAME WORLD            │  PANEL   │     │
 * │  │          │                            │          │     │
 * │  │ Shape    │        (player)            │ Layers   │     │
 * │  │ Fill     │     (debug field)          │ Profiles │     │
 * │  │ Style    │                            │          │     │
 * │  │ Anim     │                            │          │     │
 * │  └──────────┘                            └──────────┘     │
 * │  ┌────────────────────────────────────────────────────┐   │
 * │  │                   STATUS BAR                        │   │
 * │  └────────────────────────────────────────────────────┘   │
 * └────────────────────────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>Fullscreen Mode Layout</h2>
 * <pre>
 * ┌────────────────────────────────────────────────────────────┐
 * │ [⬜] Field Customizer                              [─][×]  │
 * ├────────────────────────────────────────────────────────────┤
 * │                    3D PREVIEW VIEWPORT                     │
 * ├────────────────────────────────────────────────────────────┤
 * │ [Quick] [Advanced] [Debug] [Profiles]                      │
 * ├────────────────────────────────────────────────────────────┤
 * │                    CONTENT PANEL                           │
 * ├────────────────────────────────────────────────────────────┤
 * │ Layer: 0 | Prim: 0 | sphere                            ●  │
 * └────────────────────────────────────────────────────────────┘
 * </pre>
 * 
 * @see net.cyberpunk042.client.gui.panel.AbstractPanel
 * @see net.cyberpunk042.client.gui.screen.FieldCustomizerScreen
 */
package net.cyberpunk042.client.gui.layout;

