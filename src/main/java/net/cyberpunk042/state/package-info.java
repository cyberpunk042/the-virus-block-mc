/**
 * Global State Store
 * 
 * <h2>Quick Start</h2>
 * <pre>{@code
 * import net.cyberpunk042.state.State;
 * 
 * // Set a value
 * State.set("gui.activeTab", "advanced");
 * 
 * // Get a value
 * String tab = State.getString("gui.activeTab");
 * 
 * // Watch for changes
 * State.watch("gui.activeTab", newValue -> {
 *     updateUI();
 * });
 * }</pre>
 * 
 * <h2>Path Convention</h2>
 * <pre>
 * init.*      - Initialization state (automatically populated)
 *               - init.progress, init.stage, init.loading, init.complete
 * gui.*       - UI state (tabs, modals, panels)
 *               - gui.activeTab, gui.modal.open, gui.forceModal.selectedZone
 * data.*      - Domain data (for future use)
 * </pre>
 * 
 * <h2>Integration</h2>
 * <pre>{@code
 * // In ModInitializer:
 * StateInit.connect();  // Wires Init framework → State
 * Init.orchestrator()
 *     .stage(...)
 *     .execute();
 * 
 * // In ClientModInitializer:
 * StateInit.connectClient();  // Wires client init → State
 * }</pre>
 * 
 * @see net.cyberpunk042.state.State
 * @see net.cyberpunk042.state.StateInit
 */
package net.cyberpunk042.state;
