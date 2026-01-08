package net.cyberpunk042.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashSet;
import java.util.Set;

/**
 * DEBUG MIXIN: Capture FULL STACK TRACES for entity creation and tick().
 * This will show us exactly WHERE entities are created vs WHERE they tick.
 */
@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityTickMixin {
    private static boolean hasLoggedTickTrace = false;
    private static int createCounter = 0;
    private static int tickCounter = 0;
    
    // Track UNIQUE stack trace signatures to see if creation happens from different paths
    private static Set<String> seenStackSignatures = new HashSet<>();
    private static int uniquePathsLogged = 0;
    private static final int MAX_UNIQUE_PATHS = 5;
    
    // Track first entity created to see if it ever ticks
    private static java.util.UUID firstCreatedUuid = null;
    private static boolean firstEntityEverTicked = false;
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void theVirusBlock$traceTickCall(CallbackInfo ci) {
        tickCounter++;
        
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        
        // Check if the first entity ever ticks
        if (firstCreatedUuid != null && self.getUuid().equals(firstCreatedUuid) && !firstEntityEverTicked) {
            firstEntityEverTicked = true;
            net.cyberpunk042.log.Logging.PROFILER.warn(
                "[FallingBlockTick] FIRST CREATED ENTITY IS NOW TICKING! uuid={} age={}",
                firstCreatedUuid, self.age
            );
        }
        
        // Log FULL STACK TRACE for the FIRST tick call ever
        if (!hasLoggedTickTrace) {
            hasLoggedTickTrace = true;
            StringBuilder sb = new StringBuilder();
            sb.append("\n\n====== FULL STACK TRACE FOR FallingBlockEntity.tick() ======\n");
            sb.append("Entity: age=").append(self.age).append(" pos=").append(self.getBlockPos());
            sb.append(" uuid=").append(self.getUuid()).append("\n");
            sb.append("World type: ").append(self.getWorld().getClass().getSimpleName()).append("\n");
            sb.append("World isClient: ").append(self.getWorld().isClient()).append("\n");
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (int i = 0; i < Math.min(40, stack.length); i++) {
                sb.append("  [").append(i).append("] ").append(stack[i]).append("\n");
            }
            sb.append("=============================================================\n");
            net.cyberpunk042.log.Logging.PROFILER.warn(sb.toString());
        }
        
        // Also log tick count periodically
        if (tickCounter % 100 == 0) {
            net.cyberpunk042.log.Logging.PROFILER.warn(
                "[FallingBlockTick] tick() called {} times total. Sample: age={} pos={}",
                tickCounter, self.age, self.getBlockPos().toShortString()
            );
        }
    }
    
    // Track entity CREATION with full stack trace - capture MULTIPLE UNIQUE paths!
    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V", at = @At("TAIL"))
    private void theVirusBlock$traceCreation(CallbackInfo ci) {
        createCounter++;
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        
        // Track first entity UUID to monitor if it ever ticks
        if (firstCreatedUuid == null && self.getUuid() != null) {
            firstCreatedUuid = self.getUuid();
            net.cyberpunk042.log.Logging.PROFILER.warn(
                "[FallingBlockCreate] First entity created with UUID={} - will monitor if it ever ticks",
                firstCreatedUuid
            );
        }
        
        // Create a signature from the stack trace to detect unique paths
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StringBuilder sig = new StringBuilder();
        for (int i = 2; i < Math.min(10, stack.length); i++) {
            sig.append(stack[i].getClassName()).append(".").append(stack[i].getMethodName()).append(";");
        }
        String signature = sig.toString();
        
        // Log FULL STACK TRACE for each UNIQUE code path (up to 5 different paths)
        if (!seenStackSignatures.contains(signature) && uniquePathsLogged < MAX_UNIQUE_PATHS) {
            seenStackSignatures.add(signature);
            uniquePathsLogged++;
            
            StringBuilder sb = new StringBuilder();
            sb.append("\n\n====== UNIQUE CREATION PATH #").append(uniquePathsLogged).append(" ======\n");
            sb.append("createCounter=").append(createCounter);
            sb.append(" uuid=").append(self.getUuid());
            sb.append(" world=").append(self.getWorld() != null ? self.getWorld().getClass().getSimpleName() : "null");
            sb.append(" isClient=").append(self.getWorld() != null ? self.getWorld().isClient() : "N/A");
            sb.append("\n");
            for (int i = 0; i < Math.min(40, stack.length); i++) {
                sb.append("  [").append(i).append("] ").append(stack[i]).append("\n");
            }
            sb.append("================================================================\n");
            net.cyberpunk042.log.Logging.PROFILER.warn(sb.toString());
        }
        
        // Log creation count periodically with more details
        if (createCounter % 5000 == 0) {
            boolean isServerWorld = self.getWorld() instanceof ServerWorld;
            net.cyberpunk042.log.Logging.PROFILER.warn(
                "[FallingBlockCreate] {} entities created. Unique paths: {}. isServerWorld: {}. firstEntityTicked: {}",
                createCounter, uniquePathsLogged, isServerWorld, firstEntityEverTicked
            );
        }
    }
}
