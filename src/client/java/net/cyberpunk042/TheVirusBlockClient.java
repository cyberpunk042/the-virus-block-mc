package net.cyberpunk042;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class TheVirusBlockClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Wire State store to client Init framework for observable progress
		net.cyberpunk042.state.StateInit.connectClient();
		
		// Execute staged client initialization
		net.cyberpunk042.init.Init.clientOrchestrator()
			// Stage 1: Client core (config, render layers)
			.stage(net.cyberpunk042.init.InitStage.of("client_core", "Client Core")
				.add(net.cyberpunk042.client.init.nodes.ClientCoreNodes.CONFIG)
				.add(net.cyberpunk042.client.init.nodes.ClientCoreNodes.RENDER_LAYERS)
				.add(net.cyberpunk042.client.init.nodes.ClientCoreNodes.BLOCK_ENTITY_RENDERERS))
			
			// Stage 2: Visual rendering (colors, textures, entity renderers)
			.stage(net.cyberpunk042.init.InitStage.of("client_visual", "Visuals")
				.add(net.cyberpunk042.client.init.nodes.ClientVisualNodes.COLOR_PROVIDERS)
				.add(net.cyberpunk042.client.init.nodes.ClientVisualNodes.FIRE_TEXTURES)
				.add(net.cyberpunk042.client.init.nodes.ClientVisualNodes.FLUID_RENDERERS)
				.add(net.cyberpunk042.client.init.nodes.ClientVisualNodes.ENTITY_RENDERERS))
			
			// Stage 3: Field system client
			.stage(net.cyberpunk042.init.InitStage.of("client_field", "Field System")
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.FIELD_REGISTRY)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.FIELD_NETWORK_RECEIVERS)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.FIELD_RENDER_EVENT)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.FIELD_TICK_EVENT)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.FIELD_DISCONNECT_HANDLER)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.BUNDLED_PROFILES)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.JOIN_WARMUP)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.WARMUP_OVERLAY)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.RENDER_WARMUP)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.SHADER_WARMUP)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.TEST_RENDERER)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.FRAGMENT_PRESETS)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.FRESNEL_SHADER)
				.add(net.cyberpunk042.client.init.nodes.ClientFieldNodes.DEPTH_TEST_SHADER))
			
			// Stage 4: GUI
			.stage(net.cyberpunk042.init.InitStage.of("client_gui", "GUI")
				.add(net.cyberpunk042.client.init.nodes.ClientGuiNodes.SCREENS)
				.add(net.cyberpunk042.client.init.nodes.ClientGuiNodes.GUI_HANDLERS))
			
			// Stage 5: Visual effects
			.stage(net.cyberpunk042.init.InitStage.of("client_fx", "Effects")
				.add(net.cyberpunk042.client.init.nodes.ClientFxNodes.VOID_TEAR)
				.add(net.cyberpunk042.client.init.nodes.ClientFxNodes.SINGULARITY)
				.add(net.cyberpunk042.client.init.nodes.ClientFxNodes.BORDER_STATE)
				.add(net.cyberpunk042.client.init.nodes.ClientFxNodes.GROWTH_BEAM)
				.add(net.cyberpunk042.client.init.nodes.ClientFxNodes.GROWTH_RING))
			
			// Stage 6: Network receivers
			.stage(net.cyberpunk042.init.InitStage.of("client_network", "Network")
				.add(net.cyberpunk042.client.init.nodes.ClientNetworkNodes.RECEIVERS)
				.add(net.cyberpunk042.client.init.nodes.ClientNetworkNodes.DISCONNECT_HANDLER))
			
			.execute();
		
		// Client command registration
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			// Depth test shader command
			dispatcher.register(
				net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("depthtest")
					.executes(ctx -> {
						net.cyberpunk042.client.visual.shader.DepthTestShader.cycleMode();
						int mode = net.cyberpunk042.client.visual.shader.DepthTestShader.getMode();
						String name = net.cyberpunk042.client.visual.shader.DepthTestShader.getModeName();
						ctx.getSource().sendFeedback(
							net.minecraft.text.Text.literal("§eDepth Test Mode " + mode + ": §f" + name)
						);
						return 1;
					})
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("mode", 
						com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 5))
						.executes(ctx -> {
							int mode = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "mode");
							net.cyberpunk042.client.visual.shader.DepthTestShader.setMode(mode);
							String name = net.cyberpunk042.client.visual.shader.DepthTestShader.getModeName();
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§eDepth Test Mode " + mode + ": §f" + name)
							);
							return 1;
						})
					)
			);
			
			// Direct depth renderer command
			dispatcher.register(
				net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("directdepth")
					.executes(ctx -> {
						net.cyberpunk042.client.visual.shader.DirectDepthRendererArchive.cycleMode();
						int mode = net.cyberpunk042.client.visual.shader.DirectDepthRendererArchive.getMode();
						String name = net.cyberpunk042.client.visual.shader.DirectDepthRendererArchive.getModeName();
						ctx.getSource().sendFeedback(
							net.minecraft.text.Text.literal("§bDirect Depth Mode " + mode + ": §f" + name)
						);
						return 1;
					})
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("mode", 
						com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 99))
						.executes(ctx -> {
							int mode = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "mode");
							net.cyberpunk042.client.visual.shader.DirectDepthRendererArchive.setMode(mode);
							String name = net.cyberpunk042.client.visual.shader.DirectDepthRendererArchive.getModeName();
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§bDirect Depth Mode " + mode + ": §f" + name)
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("reversedz")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.DirectDepthRendererArchive.toggleReversedZ();
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§bReversed-Z toggled")
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("ring")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("distance",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(1.0f, 500.0f))
							.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("thickness",
								com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.5f, 50.0f))
								.executes(ctx -> {
									float dist = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "distance");
									float thick = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "thickness");
									net.cyberpunk042.client.visual.shader.DirectDepthRendererArchive.setRingParams(dist, thick);
									ctx.getSource().sendFeedback(
										net.minecraft.text.Text.literal("§bRing: §f" + dist + " blocks ±" + (thick/2))
									);
									return 1;
								})
							)
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("trigger")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.DirectDepthRendererArchive.triggerAnimation();
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§bShockwave triggered!")
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("speed")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("speed",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(1.0f, 200.0f))
							.executes(ctx -> {
								float speed = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "speed");
								net.cyberpunk042.client.visual.shader.DirectDepthRendererArchive.setAnimationSpeed(speed);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§bAnimation speed: §f" + speed + " blocks/sec")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("maxradius")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(10.0f, 500.0f))
							.executes(ctx -> {
								float radius = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "radius");
								net.cyberpunk042.client.visual.shader.DirectDepthRendererArchive.setMaxRadius(radius);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§bMax radius: §f" + radius + " blocks")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("resolution")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("divisor",
							com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 32))
							.executes(ctx -> {
								int divisor = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "divisor");
								net.cyberpunk042.client.visual.shader.DirectDepthRendererArchive.setResolution(divisor);
								String quality = divisor == 1 ? "FULL" : "1/" + divisor;
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§bResolution: §f" + quality)
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("radius")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 500.0f))
							.executes(ctx -> {
								float radius = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "radius");
								net.cyberpunk042.client.visual.shader.DirectDepthRendererArchive.setRadius(radius);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§bStatic radius: §f" + radius + " blocks (no animation)")
								);
								return 1;
							})
						)
					)
				);
			
			// GPU Shockwave command (proper FrameGraph integration)
			dispatcher.register(
				net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shockwavegpu")
					.executes(ctx -> {
						net.cyberpunk042.client.visual.shader.ShockwavePostEffect.toggle();
						ctx.getSource().sendFeedback(
							net.minecraft.text.Text.literal("§d§lGPU Shockwave: §f" + 
								(net.cyberpunk042.client.visual.shader.ShockwavePostEffect.isEnabled() ? "ON" : "OFF"))
						);
						return 1;
					})
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("trigger")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.ShockwavePostEffect.trigger();
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§d§lGPU Shockwave triggered!")
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("radius")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 500.0f))
							.executes(ctx -> {
								float radius = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "radius");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setRadius(radius);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lRadius: §f" + radius + " blocks")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("thickness")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("thickness",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.5f, 50.0f))
							.executes(ctx -> {
								float thickness = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "thickness");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setThickness(thickness);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lThickness: §f" + thickness + " blocks")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("intensity")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("intensity",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 3.0f))
							.executes(ctx -> {
								float intensity = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "intensity");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setIntensity(intensity);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lIntensity: §f" + intensity)
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("speed")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("speed",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(1.0f, 200.0f))
							.executes(ctx -> {
								float speed = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "speed");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setSpeed(speed);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lSpeed: §f" + speed + " blocks/sec")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("maxradius")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("max",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(10.0f, 500.0f))
							.executes(ctx -> {
								float max = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "max");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setMaxRadius(max);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lMax Radius: §f" + max + " blocks")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("status")
						.executes(ctx -> {
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§d§lGPU Shockwave: §f" + 
									net.cyberpunk042.client.visual.shader.ShockwavePostEffect.getStatusString())
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("rings")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("count",
							com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 10))
							.executes(ctx -> {
								int count = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "count");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setRingCount(count);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lRing Count: §f" + count)
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("spacing")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("spacing",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(1.0f, 50.0f))
							.executes(ctx -> {
								float spacing = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "spacing");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setRingSpacing(spacing);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lRing Spacing: §f" + spacing + " blocks")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("glow")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("width",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(1.0f, 50.0f))
							.executes(ctx -> {
								float width = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "width");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setGlowWidth(width);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lGlow Width: §f" + width + " blocks")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("contract")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.ShockwavePostEffect.triggerContract();
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§d§lContract animation triggered!")
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("cursor")
						.executes(ctx -> {
							var client = net.minecraft.client.MinecraftClient.getInstance();
							if (client != null && client.player != null && client.world != null) {
								// Perform long-range raycast (256 blocks)
								double maxDistance = 256.0;
								var cameraEntity = client.getCameraEntity();
								if (cameraEntity == null) cameraEntity = client.player;
								
								net.minecraft.util.math.Vec3d start = cameraEntity.getCameraPosVec(1.0f);
								net.minecraft.util.math.Vec3d look = cameraEntity.getRotationVec(1.0f);
								net.minecraft.util.math.Vec3d end = start.add(look.multiply(maxDistance));
								
								// Raycast against blocks only
								var hit = client.world.raycast(new net.minecraft.world.RaycastContext(
									start, end,
									net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
									net.minecraft.world.RaycastContext.FluidHandling.NONE,
									cameraEntity
								));
								
								if (hit != null && hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
									var pos = hit.getPos();
									net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setTargetPosition(
										(float)pos.x, (float)pos.y, (float)pos.z);
									net.cyberpunk042.client.visual.shader.ShockwavePostEffect.trigger();
									ctx.getSource().sendFeedback(
										net.minecraft.text.Text.literal(String.format(
											"§d§lShockwave at: §f%.1f, %.1f, %.1f (%.0f blocks away)", 
											pos.x, pos.y, pos.z, start.distanceTo(pos)))
									);
								} else {
									// No hit - use camera mode
									net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setOriginMode(
										net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.OriginMode.CAMERA);
									net.cyberpunk042.client.visual.shader.ShockwavePostEffect.trigger();
									ctx.getSource().sendFeedback(
										net.minecraft.text.Text.literal("§d§lShockwave from camera (no block in range)")
									);
								}
							}
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("mode")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("camera")
							.executes(ctx -> {
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setOriginMode(
									net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.OriginMode.CAMERA);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lOrigin mode: §fCAMERA (around you)")
								);
								return 1;
							})
						)
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("target")
							.executes(ctx -> {
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setOriginMode(
									net.cyberpunk042.client.visual.shader.shockwave.ShockwaveTypes.OriginMode.TARGET);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lOrigin mode: §fTARGET (use /shockwavegpu cursor)")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("pos")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("x",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg())
							.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("y",
								com.mojang.brigadier.arguments.FloatArgumentType.floatArg())
								.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("z",
									com.mojang.brigadier.arguments.FloatArgumentType.floatArg())
									.executes(ctx -> {
										float x = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "x");
										float y = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "y");
										float z = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "z");
										net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setTargetPosition(x, y, z);
										net.cyberpunk042.client.visual.shader.ShockwavePostEffect.trigger();
										ctx.getSource().sendFeedback(
											net.minecraft.text.Text.literal(String.format(
												"§d§lShockwave at position: §f%.1f, %.1f, %.1f", x, y, z))
										);
										return 1;
									})
								)
							)
						)
					)
				);
			
			// TEST CYCLE command - quickly cycle through all features for testing
			dispatcher.register(
				net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shockwavetest")
					.executes(ctx -> {
						String testName = net.cyberpunk042.client.visual.shader.ShockwavePostEffect.cycleTest();
						ctx.getSource().sendFeedback(
							net.minecraft.text.Text.literal("§d§lTest: §f" + testName + " §7(run again to cycle)")
						);
						return 1;
					})
			);

						// SHAPE sub-commands
			dispatcher.register(
				net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shockwaveshape")
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("point")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setShapePoint();
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§d§lShape: §fPoint (default)")
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("sphere")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.1f, 200.0f))
							.executes(ctx -> {
								float r = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "radius");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setShapeSphere(r);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lShape: §fSphere r=" + r)
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("torus")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("major",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(1.0f, 100.0f))
							.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("minor",
								com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.1f, 20.0f))
								.executes(ctx -> {
									float major = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "major");
									float minor = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "minor");
									net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setShapeTorus(major, minor);
									ctx.getSource().sendFeedback(
										net.minecraft.text.Text.literal("§d§lShape: §fTorus major=" + major + " minor=" + minor)
									);
									return 1;
								})
							)
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("polygon")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("sides",
							com.mojang.brigadier.arguments.IntegerArgumentType.integer(3, 12))
							.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius",
								com.mojang.brigadier.arguments.FloatArgumentType.floatArg(1.0f, 100.0f))
								.executes(ctx -> {
									int sides = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "sides");
									float r = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "radius");
									net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setShapePolygon(sides, r);
									ctx.getSource().sendFeedback(
										net.minecraft.text.Text.literal("§d§lShape: §fPolygon sides=" + sides + " r=" + r)
									);
									return 1;
								})
							)
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("orbital")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("count",
							com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 8))
							.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("mainR",
								com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.5f, 50.0f))
								.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("orbitalR",
									com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.5f, 20.0f))
									.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("orbitDist",
										com.mojang.brigadier.arguments.FloatArgumentType.floatArg(2.0f, 100.0f))
										.executes(ctx -> {
											int count = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "count");
											float mainR = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "mainR");
											float orbR = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "orbitalR");
											float dist = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "orbitDist");
											net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setShapeOrbital(mainR, orbR, dist, count);
											ctx.getSource().sendFeedback(
												net.minecraft.text.Text.literal("§d§lShape: §fOrbital " + count + "x main=" + mainR + " orb=" + orbR + " dist=" + dist)
											);
											return 1;
										})
									)
								)
							)
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("speed")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("speed",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(-0.2f, 0.2f))
							.executes(ctx -> {
								float speed = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "speed");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setOrbitalSpeed(speed);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lOrbital Speed: §f" + speed + " rad/frame")
								);
								return 1;
							})
						)
					)
			);
			
			// SCREEN EFFECTS sub-commands (registered separately due to command tree limits)
			dispatcher.register(
				net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shockwavefx")
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("blackout")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("amount",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 1.0f))
							.executes(ctx -> {
								float amount = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "amount");
								net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setBlackout(amount);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lBlackout: §f" + (int)(amount * 100) + "%")
								);
								return 1;
							})
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("vignette")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("amount",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 10.0f))
							.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("radius",
								com.mojang.brigadier.arguments.FloatArgumentType.floatArg(-5.0f, 10.0f))
								.executes(ctx -> {
									float amount = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "amount");
									float radius = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "radius");
									net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setVignette(amount, radius);
									ctx.getSource().sendFeedback(
										net.minecraft.text.Text.literal("§d§lVignette: §f" + (int)(amount * 100) + "% radius=" + radius)
									);
									return 1;
								})
							)
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("tint")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("r",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 2.0f))
							.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("g",
								com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 2.0f))
								.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("b",
									com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 2.0f))
									.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("amount",
										com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 1.0f))
										.executes(ctx -> {
											float r = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "r");
											float g = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "g");
											float b = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "b");
											float amount = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "amount");
											net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setTint(r, g, b, amount);
											ctx.getSource().sendFeedback(
												net.minecraft.text.Text.literal(String.format(
													"§d§lTint: §f(%.1f, %.1f, %.1f) @ %d%%", r, g, b, (int)(amount * 100)))
											);
											return 1;
										})
									)
								)
							)
						)
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("clear")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.ShockwavePostEffect.clearScreenEffects();
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§d§lScreen effects cleared")
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("ringcolor")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("r",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 1.0f))
							.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("g",
								com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 1.0f))
								.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("b",
									com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 1.0f))
									.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("opacity",
										com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 1.0f))
										.executes(ctx -> {
											float r = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "r");
											float g = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "g");
											float b = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "b");
											float opacity = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "opacity");
											net.cyberpunk042.client.visual.shader.ShockwavePostEffect.setRingColor(r, g, b, opacity);
											ctx.getSource().sendFeedback(
												net.minecraft.text.Text.literal(String.format(
													"§d§lRing Color: §f(%.1f, %.1f, %.1f) @ %.0f%%", r, g, b, opacity * 100))
											);
											return 1;
										})
									)
								)
							)
						)
					)
				);
			
			// ═══════════════════════════════════════════════════════════════════════════
			// SHADER ANIMATION COMMAND
			// ═══════════════════════════════════════════════════════════════════════════
			dispatcher.register(
				net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("shaderanim")
					// Status (no args)
					.executes(ctx -> {
						String status = net.cyberpunk042.client.visual.shader.ShaderAnimationManager.getStatusString();
						ctx.getSource().sendFeedback(
							net.minecraft.text.Text.literal("§d§lShader Animation: §f" + status)
						);
						return 1;
					})
					// Presets
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("off")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.ShaderAnimationManager.setMode(
								net.cyberpunk042.client.visual.shader.ShaderAnimationManager.PRESET_NONE);
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§d§lAnimation: §fOFF")
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("breathe")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.ShaderAnimationManager.setMode(
								net.cyberpunk042.client.visual.shader.ShaderAnimationManager.PRESET_BREATHING);
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§d§lAnimation: §fBreathing (rim + corona)")
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("energy")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.ShaderAnimationManager.setMode(
								net.cyberpunk042.client.visual.shader.ShaderAnimationManager.PRESET_ENERGY);
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§d§lAnimation: §fEnergy (corona + flicker)")
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("rainbow")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.ShaderAnimationManager.setMode(
								net.cyberpunk042.client.visual.shader.ShaderAnimationManager.PRESET_RAINBOW);
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§d§lAnimation: §fRainbow (color cycling)")
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("unstable")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.ShaderAnimationManager.setMode(
								net.cyberpunk042.client.visual.shader.ShaderAnimationManager.PRESET_UNSTABLE);
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§d§lAnimation: §fUnstable (flicker + pulse + wave)")
							);
							return 1;
						})
					)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("all")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.shader.ShaderAnimationManager.setMode(
								net.cyberpunk042.client.visual.shader.ShaderAnimationManager.PRESET_ALL);
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§d§lAnimation: §fALL EFFECTS")
							);
							return 1;
						})
					)
					// Speed control
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("speed")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("value",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.1f, 10.0f))
							.executes(ctx -> {
								float speed = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "value");
								net.cyberpunk042.client.visual.shader.ShaderAnimationManager.setSpeed(speed);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lAnimation Speed: §f" + speed + "x")
								);
								return 1;
							})
						)
					)
					// Strength control
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("strength")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("value",
							com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0.0f, 1.0f))
							.executes(ctx -> {
								float strength = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "value");
								net.cyberpunk042.client.visual.shader.ShaderAnimationManager.setStrength(strength);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lAnimation Strength: §f" + (int)(strength * 100) + "%")
								);
								return 1;
							})
						)
					)
					// Raw mode bitmask
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("mode")
						.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("bitmask",
							com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 63))
							.executes(ctx -> {
								int mode = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "bitmask");
								net.cyberpunk042.client.visual.shader.ShaderAnimationManager.setMode(mode);
								ctx.getSource().sendFeedback(
									net.minecraft.text.Text.literal("§d§lAnimation Mode: §f" + mode + 
										" (" + net.cyberpunk042.client.visual.shader.ShaderAnimationManager.getModeDescription() + ")")
								);
								return 1;
							})
						)
					)
				);
			
			// ═══════════════════════════════════════════════════════════════════════════
			// SHADER TIME SOURCE COMMAND
			// ═══════════════════════════════════════════════════════════════════════════
			dispatcher.register(
				net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("timesource")
					// Status (no args)
					.executes(ctx -> {
						var source = net.cyberpunk042.client.visual.effect.ShaderTimeSource.class;
						ctx.getSource().sendFeedback(
							net.minecraft.text.Text.literal("§e§lTime Source: §f" + 
								net.cyberpunk042.client.visual.effect.ShaderTimeSource.getStatus())
						);
						return 1;
					})
					// Synced mode (server time with interpolation) - recommended for multiplayer
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("synced")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.effect.ShaderTimeSource.setMode(
								net.cyberpunk042.client.visual.effect.ShaderTimeSource.Mode.SYNCED_TIME);
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§a§lTime Source: §fSYNCED_TIME (smooth + multiplayer synced)")
							);
							return 1;
						})
					)
					// World time (tick-based, jerky)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("world")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.effect.ShaderTimeSource.setMode(
								net.cyberpunk042.client.visual.effect.ShaderTimeSource.Mode.WORLD_TIME);
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§6§lTime Source: §fWORLD_TIME (20 ticks/sec, synced but jerky)")
							);
							return 1;
						})
					)
					// Client time (local, smooth but desynced)
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("client")
						.executes(ctx -> {
							net.cyberpunk042.client.visual.effect.ShaderTimeSource.setMode(
								net.cyberpunk042.client.visual.effect.ShaderTimeSource.Mode.CLIENT_TIME);
							ctx.getSource().sendFeedback(
								net.minecraft.text.Text.literal("§c§lTime Source: §fCLIENT_TIME (smooth but multiplayer desync)")
							);
							return 1;
						})
					)
			);

		});
	}
}