/**
 * InitNode implementations for mod initialization.
 * 
 * <h2>Organization</h2>
 * <ul>
 *   <li>{@link net.cyberpunk042.init.nodes.CoreNodes} - Config, logging, commands</li>
 *   <li>{@link net.cyberpunk042.init.nodes.RegistryNodes} - Blocks, items, entities</li>
 *   <li>{@link net.cyberpunk042.init.nodes.NetworkNodes} - S2C/C2S payloads</li>
 *   <li>{@link net.cyberpunk042.init.nodes.FieldNodes} - Field system</li>
 *   <li>{@link net.cyberpunk042.init.nodes.InfectionNodes} - Virus system</li>
 *   <li>{@link net.cyberpunk042.init.nodes.CommandNodes} - Command registration</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * Init.orchestrator()
 *     .stage(InitStage.of("core", "Core Systems")
 *         .add(CoreNodes.CONFIG)
 *         .add(CoreNodes.LOGGING))
 *     .stage(InitStage.of("registry", "Registries")
 *         .add(RegistryNodes.BLOCKS)
 *         .add(RegistryNodes.ITEMS))
 *     .execute();
 * }</pre>
 */
package net.cyberpunk042.init.nodes;
