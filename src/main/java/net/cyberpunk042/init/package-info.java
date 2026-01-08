/**
 * Initialization Framework for The Virus Block
 * 
 * <h2>Quick Start</h2>
 * 
 * <p>This package provides a structured way to initialize mod components with:
 * <ul>
 *   <li>Dependency tracking (ensure X loads before Y)</li>
 *   <li>Progress reporting (know what's loading)</li>
 *   <li>Hot-reload support (reload configs at runtime)</li>
 *   <li>Error handling (fail gracefully with clear messages)</li>
 * </ul>
 * 
 * <h2>The 3 Things You Need to Know</h2>
 * 
 * <h3>1. Create an InitNode (represents something that loads)</h3>
 * <pre>{@code
 * public class MyRegistryNode extends InitNode {
 *     public MyRegistryNode() {
 *         super("my_registry", "My Registry");
 *     }
 *     
 *     @Override
 *     protected int load() {
 *         // Load your stuff here
 *         return itemCount; // Return how many things loaded
 *     }
 * }
 * }</pre>
 * 
 * <h3>2. Register it with the orchestrator</h3>
 * <pre>{@code
 * // In your ModInitializer:
 * Init.orchestrator()
 *     .register(new MyRegistryNode())
 *     .register(new AnotherNode())
 *     .execute();
 * }</pre>
 * 
 * <h3>3. (Optional) Listen for changes</h3>
 * <pre>{@code
 * Init.store().subscribe(event -> {
 *     if (event.isComplete("my_registry")) {
 *         // React to my_registry finishing
 *     }
 * });
 * }</pre>
 * 
 * <h2>That's It!</h2>
 * 
 * <p>For more advanced usage (dependencies, reload, etc.), see the individual class docs.
 * 
 * @see net.cyberpunk042.init.Init The main entry point
 * @see net.cyberpunk042.init.InitNode The base class for loadable things
 * @see net.cyberpunk042.init.InitOrchestrator Manages execution order
 */
package net.cyberpunk042.init;
