package net.cyberpunk042.client.gui.state.adapter;

import net.cyberpunk042.client.gui.state.AppearanceState;
import net.cyberpunk042.client.gui.state.StateField;
import net.cyberpunk042.field.primitive.Primitive;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.visual.appearance.Appearance;
import net.cyberpunk042.visual.appearance.ColorDistribution;
import net.cyberpunk042.visual.appearance.ColorMode;
import net.cyberpunk042.visual.appearance.ColorSet;
import net.cyberpunk042.visual.appearance.GradientDirection;

import java.util.Set;

/**
 * Adapter for appearance state (colors, alpha, glow, emissive, color modes, etc.).
 * 
 * <p>Note: Uses AppearanceState (int colors) internally, converts to/from 
 * Appearance (String colors) when syncing with Primitive.</p>
 */
@StateCategory("appearance")
public class AppearanceAdapter extends AbstractAdapter implements PrimitiveAdapter {
    
    @StateField private AppearanceState appearance = AppearanceState.DEFAULT;
    
    @Override
    public String category() { return "appearance"; }
    
    @Override
    public void loadFrom(Primitive source) {
        Appearance app = source.appearance();
        if (app != null) {
            int colorInt = parseColorToInt(app.color());
            int secondaryInt = parseColorToInt(app.secondaryColor());
            float alphaMin = app.alpha() != null ? app.alpha().min() : 1.0f;
            float alphaMax = app.alpha() != null ? app.alpha().max() : 1.0f;
            
            this.appearance = new AppearanceState(
                colorInt,                                       // color
                alphaMin,                                       // alphaMin
                alphaMax,                                       // alphaMax
                app.glow(),                                     // glow
                app.emissive(),                                 // emissive
                app.saturation(),                               // saturation
                app.brightness(),                               // brightness
                app.hueShift(),                                 // hueShift
                colorInt,                                       // primaryColor
                secondaryInt,                                   // secondaryColor
                app.colorBlend(),                               // colorBlend
                app.effectiveColorMode(),                       // colorMode
                app.effectiveDistribution(),                    // colorDistribution
                app.effectiveColorSet(),                        // colorSet
                app.effectiveDirection(),                       // gradientDirection
                app.timePhase(),                                // timePhase
                net.cyberpunk042.visual.layer.BlendMode.NORMAL  // blendMode (layer-level, default NORMAL)
            );
        } else {
            this.appearance = AppearanceState.DEFAULT;
        }
        Logging.GUI.topic("adapter").trace("AppearanceAdapter loaded");
    }
    
    @Override
    public void saveTo(PrimitiveBuilder builder) {
        String colorHex = String.format("#%06X", appearance.primaryColor() & 0xFFFFFF);
        String secondaryHex = appearance.secondaryColor() != 0 
            ? String.format("#%06X", appearance.secondaryColor() & 0xFFFFFF) 
            : null;
        
        builder.appearance(Appearance.builder()
            .color(colorHex)
            .alpha(net.cyberpunk042.visual.appearance.AlphaRange.of(appearance.alpha()))
            .glow(appearance.glow())
            .emissive(appearance.emissive())
            .saturation(appearance.saturation())
            .brightness(appearance.brightness())
            .hueShift(appearance.hueShift())
            .secondaryColor(secondaryHex)
            .colorBlend(appearance.colorBlend())
            .colorMode(appearance.colorMode())
            .colorDistribution(appearance.colorDistribution())
            .colorSet(appearance.colorSet())
            .gradientDirection(appearance.gradientDirection())
            .timePhase(appearance.timePhase())
            .build());
    }
    
    private int parseColorToInt(String color) {
        if (color == null || color.isEmpty()) return 0xFFFFFFFF;
        if (color.startsWith("#")) {
            try {
                String hex = color.substring(1);
                if (hex.length() == 6) {
                    return 0xFF000000 | Integer.parseInt(hex, 16);
                } else if (hex.length() == 8) {
                    return (int) Long.parseLong(hex, 16);
                }
            } catch (NumberFormatException e) {
                Logging.GUI.topic("adapter").warn("Invalid hex color: {}", color);
            }
        }
        return 0xFF00FFFF; // Default cyan
    }
    
    public AppearanceState appearance() { return appearance; }
    public void setAppearance(AppearanceState appearance) { this.appearance = appearance; }
    
    @Override
    public void reset() {
        this.appearance = AppearanceState.DEFAULT;
    }
}
