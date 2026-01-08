/**
 * Client-side InitNode implementations.
 * 
 * <h2>Organization</h2>
 * <ul>
 *   <li>{@link net.cyberpunk042.client.init.nodes.ClientCoreNodes} - Config, render layers</li>
 *   <li>{@link net.cyberpunk042.client.init.nodes.ClientVisualNodes} - Colors, textures, renderers</li>
 *   <li>{@link net.cyberpunk042.client.init.nodes.ClientFieldNodes} - Field system client</li>
 *   <li>{@link net.cyberpunk042.client.init.nodes.ClientGuiNodes} - Screens, GUI handlers</li>
 *   <li>{@link net.cyberpunk042.client.init.nodes.ClientFxNodes} - Visual effects</li>
 *   <li>{@link net.cyberpunk042.client.init.nodes.ClientNetworkNodes} - Network receivers</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * Init.clientOrchestrator()
 *     .stage(InitStage.of("client_core", "Client Core")
 *         .add(ClientCoreNodes.CONFIG)
 *         .add(ClientCoreNodes.RENDER_LAYERS))
 *     .stage(InitStage.of("client_visual", "Visuals")
 *         .add(ClientVisualNodes.COLOR_PROVIDERS)
 *         .add(ClientVisualNodes.ENTITY_RENDERERS))
 *     .execute();
 * }</pre>
 */
package net.cyberpunk042.client.init.nodes;
