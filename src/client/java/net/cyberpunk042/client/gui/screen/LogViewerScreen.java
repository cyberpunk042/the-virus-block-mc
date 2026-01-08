package net.cyberpunk042.client.gui.screen;

import net.cyberpunk042.client.gui.layout.Bounds;
import net.cyberpunk042.log.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Standalone Log Viewer screen for runtime log configuration.
 * 
 * Opened via: /virus logs gui
 * 
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  🔧 Log Controls                                                    │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │  [Global Mute: OFF]  [Min Level: ▼ ---]  [Reset All]               │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │  Channel        Level      Mute  Force  Pass                       │
 * │  ─────────────────────────────────────────────────                 │
 * │  ● singularity  [INFO ▼]   [ ]   [ ]    [ ]                        │
 * │  ● collapse     [INFO ▼]   [ ]   [ ]    [ ]                        │
 * │  ○ growth       [WARN ▼]   [✓]   [ ]    [ ]                        │
 * │  ...                                                               │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │                                              [Close]               │
 * └─────────────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class LogViewerScreen extends Screen {
    
    private static final int TITLE_HEIGHT = 30;
    private static final int CONTROLS_HEIGHT = 36;
    private static final int ROW_HEIGHT = 24;
    private static final int HEADER_HEIGHT = 24;
    private static final int FOOTER_HEIGHT = 40;
    private static final int PADDING = 16;
    private static final int SCROLL_WIDTH = 10;
    
    // Column widths
    private static final int COL_STATUS = 18;
    private static final int COL_CHANNEL = 110;
    private static final int COL_LEVEL = 90;
    private static final int COL_TOGGLE = 50;
    
    private int panelWidth = 560;
    private int panelHeight = 440;
    
    private Bounds panelBounds = Bounds.EMPTY;
    private Bounds contentBounds = Bounds.EMPTY;
    
    private final List<ChannelRow> channelRows = new ArrayList<>();
    
    // Scroll state
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private int visibleRows = 0;
    
    // State
    private ButtonWidget muteButton;
    private ButtonWidget minLevelButton;
    private int minLevelIndex = 0;
    
    public LogViewerScreen() {
        super(Text.literal("Log Controls"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Sync minLevelIndex with current state
        LogLevel currentMin = LogOverride.minLevel();
        if (currentMin == null) {
            minLevelIndex = 0;
        } else {
            minLevelIndex = switch (currentMin) {
                case ERROR -> 1;
                case WARN -> 2;
                case INFO -> 3;
                case DEBUG -> 4;
                case TRACE -> 5;
                default -> 0;
            };
        }
        
        rebuild();
    }
    
    private void rebuild() {
        clearChildren();
        channelRows.clear();
        
        // Center panel
        panelBounds = Bounds.centeredIn(width, height, panelWidth, panelHeight);
        
        // === GLOBAL CONTROLS ===
        int controlsY = panelBounds.y() + TITLE_HEIGHT + 8;
        int btnX = panelBounds.x() + PADDING;
        
        // Global mute toggle
        String muteLabel = LogOverride.isMuted() ? "§cMute: ON" : "Mute: OFF";
        muteButton = ButtonWidget.builder(Text.literal(muteLabel), btn -> {
            if (LogOverride.isMuted()) {
                LogOverride.unmuteAll();
            } else {
                LogOverride.muteAll();
            }
            rebuild();
        })
            .dimensions(btnX, controlsY, 90, 22)
            .build();
        addDrawableChild(muteButton);
        btnX += 96;
        
        // Min level cycle button
        LogLevel currentMin = LogOverride.minLevel();
        String minLabel = currentMin != null ? "Min: " + currentMin.name() : "Min: ---";
        minLevelButton = ButtonWidget.builder(Text.literal(minLabel), btn -> cycleMinLevel())
            .dimensions(btnX, controlsY, 90, 22)
            .build();
        addDrawableChild(minLevelButton);
        btnX += 96;
        
        // Reset all button
        addDrawableChild(ButtonWidget.builder(Text.literal("§eReset All"), btn -> {
            LogOverride.clearAll();
            Logging.reset();
            minLevelIndex = 0;
            rebuild();
        })
            .dimensions(btnX, controlsY, 80, 22)
            .build());
        
        // === CHANNEL LIST ===
        int listY = controlsY + CONTROLS_HEIGHT + HEADER_HEIGHT;
        int listHeight = panelHeight - TITLE_HEIGHT - CONTROLS_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - PADDING;
        visibleRows = listHeight / ROW_HEIGHT;
        
        contentBounds = new Bounds(
            panelBounds.x() + PADDING,
            listY,
            panelWidth - PADDING * 2 - SCROLL_WIDTH,
            listHeight
        );
        
        // Build channel rows
        List<Channel> channels = new ArrayList<>(Logging.channels());
        maxScrollOffset = Math.max(0, channels.size() - visibleRows);
        scrollOffset = Math.min(scrollOffset, maxScrollOffset);
        
        int y = contentBounds.y();
        for (int i = scrollOffset; i < Math.min(scrollOffset + visibleRows, channels.size()); i++) {
            Channel ch = channels.get(i);
            ChannelRow row = new ChannelRow(ch, panelBounds.x() + PADDING, y, contentBounds.width());
            channelRows.add(row);
            y += ROW_HEIGHT;
        }
        
        // === FOOTER ===
        int footerY = panelBounds.bottom() - FOOTER_HEIGHT + 10;
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
            .dimensions(panelBounds.right() - PADDING - 70, footerY, 70, 22)
            .build());
    }
    
    private void cycleMinLevel() {
        LogLevel[] levels = {null, LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO, LogLevel.DEBUG, LogLevel.TRACE};
        minLevelIndex = (minLevelIndex + 1) % levels.length;
        LogLevel newLevel = levels[minLevelIndex];
        if (newLevel == null) {
            LogOverride.clearMinLevel();
        } else {
            LogOverride.setMinLevel(newLevel);
        }
        rebuild();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Darken background
        context.fill(0, 0, width, height, 0xCC000000);
        
        // Panel shadow
        context.fill(panelBounds.x() + 4, panelBounds.y() + 4,
                     panelBounds.right() + 4, panelBounds.bottom() + 4, 0x66000000);
        
        // Panel border (accent color - green)
        context.fill(panelBounds.x() - 2, panelBounds.y() - 2,
                     panelBounds.right() + 2, panelBounds.bottom() + 2, 0xFF2D5A27);
        
        // Panel background
        context.fill(panelBounds.x(), panelBounds.y(),
                     panelBounds.right(), panelBounds.bottom(), 0xFF0D0D0D);
        
        // Title bar
        context.fill(panelBounds.x(), panelBounds.y(),
                     panelBounds.right(), panelBounds.y() + TITLE_HEIGHT, 0xFF1A1A1A);
        
        // Title text
        String title = "🔧 Log Controls";
        if (LogOverride.hasOverrides()) {
            title += " §e(overrides active)";
        }
        context.drawTextWithShadow(textRenderer, title, 
            panelBounds.x() + PADDING, panelBounds.y() + 10, 0xFF66FF66);
        
        // Title separator
        context.fill(panelBounds.x(), panelBounds.y() + TITLE_HEIGHT,
                     panelBounds.right(), panelBounds.y() + TITLE_HEIGHT + 1, 0xFF2D5A27);
        
        // Controls separator
        int controlsEndY = panelBounds.y() + TITLE_HEIGHT + CONTROLS_HEIGHT + 8;
        context.fill(panelBounds.x() + PADDING, controlsEndY,
                     panelBounds.right() - PADDING, controlsEndY + 1, 0xFF333333);
        
        // Column headers
        int headerY = controlsEndY + 8;
        int x = panelBounds.x() + PADDING;
        context.drawTextWithShadow(textRenderer, "Channel", x + COL_STATUS, headerY, 0xFF888888);
        x += COL_STATUS + COL_CHANNEL;
        context.drawTextWithShadow(textRenderer, "Level", x, headerY, 0xFF888888);
        x += COL_LEVEL;
        context.drawTextWithShadow(textRenderer, "M", x + 18, headerY, 0xFF666666);  // Mute
        x += COL_TOGGLE;
        context.drawTextWithShadow(textRenderer, "F", x + 18, headerY, 0xFF666666);  // Force
        x += COL_TOGGLE;
        context.drawTextWithShadow(textRenderer, "P", x + 18, headerY, 0xFF666666);  // Pass
        
        // Header underline
        context.fill(panelBounds.x() + PADDING, headerY + 14,
                     panelBounds.right() - PADDING - SCROLL_WIDTH, headerY + 15, 0xFF333333);
        
        // Channel status indicators
        for (ChannelRow row : channelRows) {
            row.renderStatus(context, textRenderer);
        }
        
        // Scrollbar
        if (maxScrollOffset > 0) {
            int scrollbarX = panelBounds.right() - PADDING - SCROLL_WIDTH + 4;
            int scrollbarY = contentBounds.y();
            int scrollbarHeight = contentBounds.height();
            
            // Track
            context.fill(scrollbarX, scrollbarY, scrollbarX + 6, scrollbarY + scrollbarHeight, 0xFF222222);
            
            // Thumb
            int thumbHeight = Math.max(24, scrollbarHeight * visibleRows / (maxScrollOffset + visibleRows));
            int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollOffset / maxScrollOffset;
            context.fill(scrollbarX, thumbY, scrollbarX + 6, thumbY + thumbHeight, 0xFF555555);
        }
        
        // Footer separator
        int footerY = panelBounds.bottom() - FOOTER_HEIGHT;
        context.fill(panelBounds.x() + PADDING, footerY,
                     panelBounds.right() - PADDING, footerY + 1, 0xFF333333);
        
        // Render widgets
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (panelBounds.contains(mouseX, mouseY)) {
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset - (int) verticalAmount));
            rebuild();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CHANNEL ROW
    // ═══════════════════════════════════════════════════════════════════════════
    
    private class ChannelRow {
        final Channel channel;
        final int x, y, rowWidth;
        
        ChannelRow(Channel ch, int x, int y, int rowWidth) {
            this.channel = ch;
            this.x = x;
            this.y = y;
            this.rowWidth = rowWidth;
            build();
        }
        
        void build() {
            int btnX = x + COL_STATUS + COL_CHANNEL;
            int btnY = y;
            int btnH = ROW_HEIGHT - 4;
            
            // Level cycle button
            LogLevel effective = LogOverride.effectiveLevel(channel);
            String levelLabel = levelColor(effective) + effective.name();
            addDrawableChild(ButtonWidget.builder(Text.literal(levelLabel), btn -> cycleLevel())
                .dimensions(btnX, btnY, COL_LEVEL - 6, btnH)
                .build());
            btnX += COL_LEVEL;
            
            // Mute toggle
            boolean muted = LogOverride.getChannelOverride(channel.id()) == LogLevel.OFF;
            addDrawableChild(ButtonWidget.builder(Text.literal(muted ? "§c✓" : "-"), btn -> {
                if (muted) {
                    LogOverride.clearChannelLevel(channel.id());
                } else {
                    LogOverride.setChannelLevel(channel.id(), LogLevel.OFF);
                }
                rebuild();
            })
                .dimensions(btnX, btnY, COL_TOGGLE - 6, btnH)
                .build());
            btnX += COL_TOGGLE;
            
            // Force output toggle
            boolean forced = LogOverride.isForceOutput(channel.id());
            addDrawableChild(ButtonWidget.builder(Text.literal(forced ? "§a✓" : "-"), btn -> {
                if (forced) {
                    LogOverride.unforceOutput(channel.id());
                } else {
                    LogOverride.forceOutput(channel.id());
                }
                rebuild();
            })
                .dimensions(btnX, btnY, COL_TOGGLE - 6, btnH)
                .build());
            btnX += COL_TOGGLE;
            
            // Passthrough toggle
            boolean pass = LogOverride.isPassthrough(channel.id());
            addDrawableChild(ButtonWidget.builder(Text.literal(pass ? "§b✓" : "-"), btn -> {
                if (pass) {
                    LogOverride.removePassthrough(channel.id());
                } else {
                    LogOverride.addPassthrough(channel.id());
                }
                rebuild();
            })
                .dimensions(btnX, btnY, COL_TOGGLE - 6, btnH)
                .build());
        }
        
        void cycleLevel() {
            LogLevel[] levels = LogLevel.values();
            LogLevel current = LogOverride.getChannelOverride(channel.id());
            if (current == null) current = channel.level();
            
            int idx = 0;
            for (int i = 0; i < levels.length; i++) {
                if (levels[i] == current) { idx = i; break; }
            }
            idx = (idx + 1) % levels.length;
            LogOverride.setChannelLevel(channel.id(), levels[idx]);
            rebuild();
        }
        
        void renderStatus(DrawContext context, net.minecraft.client.font.TextRenderer tr) {
            LogLevel effective = LogOverride.effectiveLevel(channel);
            boolean active = effective != LogLevel.OFF;
            String status = active ? "●" : "○";
            int color = active ? levelColorInt(effective) : 0xFF555555;
            context.drawTextWithShadow(tr, status, x, y + 6, color);
            
            int nameColor = active ? 0xFFCCCCCC : 0xFF666666;
            context.drawTextWithShadow(tr, channel.label(), x + COL_STATUS, y + 6, nameColor);
        }
        
        private String levelColor(LogLevel level) {
            return switch (level) {
                case OFF -> "§8";
                case ERROR -> "§c";
                case WARN -> "§e";
                case INFO -> "§a";
                case DEBUG -> "§b";
                case TRACE -> "§d";
            };
        }
        
        private int levelColorInt(LogLevel level) {
            return switch (level) {
                case OFF -> 0xFF555555;
                case ERROR -> 0xFFFF5555;
                case WARN -> 0xFFFFAA00;
                case INFO -> 0xFF55FF55;
                case DEBUG -> 0xFF55FFFF;
                case TRACE -> 0xFFFF55FF;
            };
        }
    }
}

