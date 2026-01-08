package net.cyberpunk042.field.effect;

/**
 * Types of effects that fields can apply.
 */
public enum EffectType {
    
    /** Pushes entities away from field center. */
    PUSH("push"),
    
    /** Pulls entities toward field center. */
    PULL("pull"),
    
    /** Damages entities in the field. */
    DAMAGE("damage"),
    
    /** Heals entities in the field. */
    HEAL("heal"),
    
    /** Blocks infection spread. */
    SHIELD("shield"),
    
    /** Slows entities in the field. */
    SLOW("slow"),
    
    /** Speeds up entities in the field. */
    SPEED("speed"),
    
    /** Custom effect (handled externally). */
    CUSTOM("custom");
    
    private final String id;
    
    EffectType(String id) {
        this.id = id;
    }
    
    public String id() {
        return id;
    }
    
    public static EffectType fromId(String id) {
        for (EffectType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return CUSTOM;
    }
}
