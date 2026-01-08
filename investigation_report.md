# Shader Preprocessor & Mixin Investigation

## Summary
The investigation into the shader preprocessor setup reveals **two critical issues**:
1. **Mixin Target Uncertainty**: `CompiledShader.compile` is likely not the method responsible for compiling Post Effect shaders in Minecraft 1.21.6, explaining why the interception might not be occurring.
2. **Infinite Recursion Bug**: The current error handling in `CompiledShaderMixin` will cause a **StackOverflowError** (game crash) if shader preprocessing fails.

## 1. Mixin Target Issue
The mixin `CompiledShaderMixin` intercepts:
```java
net.minecraft.client.gl.CompiledShader.compile(Identifier, ShaderType, String)
```
While this class exists, Post Effect shaders mechanism (used by `DepthTestShader` via `ShaderLoader.loadPostEffect`) often follows a different compilation path, possibly directly through `GlProgram` or `ShaderProgram` logic that bypasses the `CompiledShader` static helper, or uses a different overload.

**Diagnostic Step:**
If the mixin is working, you MUST see `[Render/shader_mixin] CompiledShader.compile intercepted` in your logs during game startup (when vanilla shaders load).
- **If no logs appear:** The mixin target is incorrect for this version of Minecraft.
- **If logs appear but not for your shader:** Your shader might be loaded via a different system (e.g., proper PostChain vs Core Shader).

## 2. Critical Recursion Bug
In `CompiledShaderMixin.java`:
```java
// Logic:
String processed = ShaderPreprocessor.process(source, id);
// If process() fails, it returns 'source' (original with includes)
// Then:
CompiledShader result = CompiledShader.compile(id, type, processed);
```
If `processed` equals `source` (due to error), calling `compile(source)` will trigger the **same mixin again**, finding includes again, and looping infinitely until crash.

**Fix:**
Check if `processed` differs from `source` before recursing.

## 3. Recommended Fixes

### A. Fix the Recursion Loop
Update `CompiledShaderMixin.java`:

```java
try {
    String processed = ShaderPreprocessor.process(source, id);
    
    // CRITICAL: If no changes (or error), DO NOT recurse.
    if (processed == null || processed.equals(source)) {
        return; // Proceed to original method
    }

    // ... logging ...
    
    CompiledShader result = CompiledShader.compile(id, type, processed);
    cir.setReturnValue(result);
    
} catch (Exception e) {
    // ...
}
```

### B. Verify/Change Mixin Target
If `CompiledShader.compile` is indeed not called, you need to find the correct insertion point. For Minecraft 1.21+, consider investigating `net.minecraft.client.gl.ShaderLoader` or `net.minecraft.client.gl.ShaderProgram`.

If you cannot find the correct method, a broader capture might be needed, or ensure your mappings match the runtime.

## 4. Preprocessor Logic
The preprocessing logic in `ShaderPreprocessor.java` appears correct:
- Regex: `^\s*#include\s+"([^"]+)"\s*$` handles typical include lines.
- Path resolution: Resolves relative to the shader file (e.g. `shaders/post/shockwave_ring.fsh` + `include/math.glsl` -> `shaders/post/include/math.glsl`).
- Filtering: Correctly limits to `the-virus-block` namespace.

**Note:** Ensure your shader files use Unix line endings (`\n`) or that the regex handles `\r\n` correctly (`$` in multiline mode usually does).
