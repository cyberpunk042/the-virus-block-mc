package net.cyberpunk042.field.influence;

/**
 * Visual effects that can be triggered by events.
 * 
 * <p>Per ARCHITECTURE §12.2:
 * <ul>
 *   <li>FLASH - Brief color overlay</li>
 *   <li>PULSE - Scale up then back</li>
 *   <li>SHAKE - Rapid position jitter</li>
 *   <li>GLOW - Temporary glow boost</li>
 *   <li>COLOR_SHIFT - Temporary color change</li>
 * </ul>
 * 
 * <p>F153: Some effects complete naturally (return to baseline),
 * others maintain state until duration ends.
 */
public enum TriggerEffect {
    FLASH,
    PULSE,
    SHAKE,
    GLOW,
    COLOR_SHIFT;
    
    /**
     * F153: Whether this effect returns to baseline naturally.
     * 
     * <p>Effects like PULSE and SHAKE animate back to their
     * starting state. Effects like FLASH and GLOW maintain
     * their effect value until duration ends.
     */
    public boolean completesNaturally() {
        return switch (this) {
            case PULSE, SHAKE -> true;
            case FLASH, GLOW, COLOR_SHIFT -> false;
        };
    }
    
    /**
     * Parses from string, case-insensitive.
     */
    public static TriggerEffect fromId(String id) {
        if (id == null) return FLASH;
        try {
            return valueOf(id.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return FLASH;
        }
    }
}
