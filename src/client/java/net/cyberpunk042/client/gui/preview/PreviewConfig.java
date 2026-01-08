package net.cyberpunk042.client.gui.preview;

/**
 * Configuration settings for the field preview renderer.
 * 
 * <p>Stores settings that are adjustable via the Debug/Trace panel.</p>
 */
public final class PreviewConfig {
    
    private PreviewConfig() {}
    
    /** 
     * Preview detail level - controls tessellation quality.
     * Range: 1-16, higher = more triangles = smoother shapes but slower.
     */
    public static int previewDetail = 8;
    
    /** Minimum detail level */
    public static final int MIN_DETAIL = 1;
    
    /** Maximum detail level */
    public static final int MAX_DETAIL = 16;
    
    /** Default detail level */
    public static final int DEFAULT_DETAIL = 8;
    
    /**
     * Gets the detail level clamped to valid range.
     */
    public static int getDetail() {
        return Math.max(MIN_DETAIL, Math.min(MAX_DETAIL, previewDetail));
    }
    
    /**
     * Sets the detail level (clamped).
     */
    public static void setDetail(int detail) {
        previewDetail = Math.max(MIN_DETAIL, Math.min(MAX_DETAIL, detail));
    }
}
