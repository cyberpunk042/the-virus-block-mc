package net.cyberpunk042.client.gui.builder;

import net.cyberpunk042.client.gui.widget.LabeledSlider;
import org.joml.Vector3f;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Three-slider binding for Vector3f state values.
 * 
 * <p>Each slider modifies one component while preserving others.
 * Solves the compound update problem where 3 sliders share one state path.</p>
 * 
 * <h3>Example - Transform Offset</h3>
 * <pre>
 * Vec3Binding offset = Vec3Binding.of(sliderX, sliderY, sliderZ)
 *     .readFrom(() -&gt; state.transform().offset())
 *     .writeTo(v -&gt; state.set("transform.offset", v))
 *     .build();
 * 
 * // Wire up widget onChange callbacks
 * sliderX.onChange(offset::onXChanged);
 * sliderY.onChange(offset::onYChanged);
 * sliderZ.onChange(offset::onZChanged);
 * 
 * // On state change, sync all three
 * offset.syncFromState();
 * </pre>
 * 
 * @see Bound
 */
public final class Vec3Binding {
    
    private final LabeledSlider sliderX;
    private final LabeledSlider sliderY;
    private final LabeledSlider sliderZ;
    private final Supplier<Vector3f> getter;
    private final Consumer<Vector3f> setter;
    
    private boolean syncing = false;
    
    private Vec3Binding(LabeledSlider x, LabeledSlider y, LabeledSlider z,
            Supplier<Vector3f> getter, Consumer<Vector3f> setter) {
        this.sliderX = x;
        this.sliderY = y;
        this.sliderZ = z;
        this.getter = getter;
        this.setter = setter;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET CHANGE HANDLERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Called when X slider changes.
     * Updates X component while preserving Y and Z.
     */
    public void onXChanged(float value) {
        if (syncing) return;
        syncing = true;
        try {
            Vector3f current = safeGet();
            current.x = value;
            setter.accept(current);
        } finally {
            syncing = false;
        }
    }
    
    /**
     * Called when Y slider changes.
     * Updates Y component while preserving X and Z.
     */
    public void onYChanged(float value) {
        if (syncing) return;
        syncing = true;
        try {
            Vector3f current = safeGet();
            current.y = value;
            setter.accept(current);
        } finally {
            syncing = false;
        }
    }
    
    /**
     * Called when Z slider changes.
     * Updates Z component while preserving X and Y.
     */
    public void onZChanged(float value) {
        if (syncing) return;
        syncing = true;
        try {
            Vector3f current = safeGet();
            current.z = value;
            setter.accept(current);
        } finally {
            syncing = false;
        }
    }
    
    private Vector3f safeGet() {
        Vector3f v = getter.get();
        // Return a copy so we don't mutate the original
        return v != null ? new Vector3f(v) : new Vector3f();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE SYNC
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Syncs all three sliders from state.
     * Call this on profile load, primitive switch, or external changes.
     */
    public void syncFromState() {
        if (syncing) return;
        syncing = true;
        try {
            Vector3f v = getter.get();
            if (v == null) v = new Vector3f();
            
            if (sliderX != null) sliderX.setValue(v.x);
            if (sliderY != null) sliderY.setValue(v.y);
            if (sliderZ != null) sliderZ.setValue(v.z);
        } finally {
            syncing = false;
        }
    }
    
    /**
     * @return The three sliders for adding to panel widget list.
     */
    public LabeledSlider[] widgets() {
        return new LabeledSlider[] { sliderX, sliderY, sliderZ };
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER
    // ═══════════════════════════════════════════════════════════════════════════
    
    public static Builder of(LabeledSlider x, LabeledSlider y, LabeledSlider z) {
        return new Builder(x, y, z);
    }
    
    public static class Builder {
        private final LabeledSlider x, y, z;
        private Supplier<Vector3f> getter;
        private Consumer<Vector3f> setter;
        
        private Builder(LabeledSlider x, LabeledSlider y, LabeledSlider z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        /**
         * Sets the getter for reading the current Vector3f value.
         */
        public Builder readFrom(Supplier<Vector3f> getter) {
            this.getter = getter;
            return this;
        }
        
        /**
         * Sets the setter for writing the updated Vector3f value.
         */
        public Builder writeTo(Consumer<Vector3f> setter) {
            this.setter = setter;
            return this;
        }
        
        public Vec3Binding build() {
            if (getter == null) throw new IllegalStateException("readFrom() required");
            if (setter == null) throw new IllegalStateException("writeTo() required");
            return new Vec3Binding(x, y, z, getter, setter);
        }
    }
}
