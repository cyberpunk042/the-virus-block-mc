package net.cyberpunk042.log;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Forwards log messages to in-game chat.
 */
public final class LogChatBridge {
    
    private static volatile MinecraftServer server = null;
    private static final ConcurrentHashMap<String, RateTracker> rateTrackers = new ConcurrentHashMap<>();
    
    private static class RateTracker {
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicLong lastSecond = new AtomicLong(0);
    }
    
    public static void setServer(MinecraftServer srv) {
        server = srv;
    }
    
    public static void forward(Channel channel, String topic, LogLevel level, String message) {
        MinecraftServer srv = server;
        if (srv == null) return;
        
        // Rate limiting
        int rateLimit = LogConfig.chatRateLimit();
        if (rateLimit > 0) {
            String key = channel.id() + (topic != null ? ":" + topic : "");
            RateTracker tracker = rateTrackers.computeIfAbsent(key, k -> new RateTracker());
            long now = System.currentTimeMillis() / 1000;
            if (tracker.lastSecond.get() != now) {
                tracker.lastSecond.set(now);
                tracker.count.set(0);
            }
            if (tracker.count.incrementAndGet() > rateLimit) {
                return;
            }
        }
        
        Formatting color = switch (level) {
            case ERROR -> Formatting.RED;
            case WARN -> Formatting.YELLOW;
            case INFO -> Formatting.WHITE;
            case DEBUG -> Formatting.GRAY;
            case TRACE -> Formatting.DARK_GRAY;
            default -> Formatting.WHITE;
        };
        
        Text text = Text.literal(message).formatted(color);
        
        ChatRecipients recipients = LogConfig.chatRecipients();
        for (ServerPlayerEntity player : srv.getPlayerManager().getPlayerList()) {
            if (recipients == ChatRecipients.ALL || 
                (recipients == ChatRecipients.OPS && srv.getPlayerManager().isOperator(player.getGameProfile()))) {
                player.sendMessage(text, false);
            }
        }
    }
    
    private LogChatBridge() {}
}
