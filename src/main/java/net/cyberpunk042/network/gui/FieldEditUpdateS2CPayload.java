package net.cyberpunk042.network.gui;

import net.cyberpunk042.log.Logging;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Server → Client packet for /field command updates to FieldEditState.
 * 
 * <p>Carries field edit changes from server commands to client state.
 * Uses JSON string for flexibility - can send partial updates.</p>
 * 
 * <h2>Update Types</h2>
 * <ul>
 *   <li>SHAPE - shape type and parameters</li>
 *   <li>TRANSFORM - anchor, offset, rotation, scale</li>
 *   <li>ORBIT - orbit parameters</li>
 *   <li>FILL - fill mode and parameters</li>
 *   <li>VISIBILITY - mask type and parameters</li>
 *   <li>APPEARANCE - color, alpha, glow</li>
 *   <li>ANIMATION - spin, pulse, modifiers</li>
 *   <li>LAYER - layer selection/management</li>
 *   <li>BINDING - property bindings</li>
 *   <li>BEAM - beam configuration</li>
 *   <li>FOLLOW - follow mode</li>
 *   <li>PREDICTION - prediction settings</li>
 *   <li>FULL - complete state replacement</li>
 *   <li>RESET - reset to defaults</li>
 * </ul>
 * 
 * @param updateType Category of update (SHAPE, TRANSFORM, FULL, etc.)
 * @param jsonData JSON string with the update data
 */
public record FieldEditUpdateS2CPayload(
    String updateType,
    String jsonData
) implements CustomPayload {
    
    public static final Id<FieldEditUpdateS2CPayload> ID = new Id<>(GuiPacketIds.FIELD_EDIT_UPDATE_S2C);
    
    public static final PacketCodec<RegistryByteBuf, FieldEditUpdateS2CPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeString(payload.updateType);
            buf.writeString(payload.jsonData);
        },
        buf -> new FieldEditUpdateS2CPayload(
            buf.readString(),
            buf.readString()
        )
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FACTORY METHODS for common updates
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** Shape type change. */
    public static FieldEditUpdateS2CPayload shapeType(String type) {
        return new FieldEditUpdateS2CPayload("SHAPE_TYPE", "{\"type\":\"" + type + "\"}");
    }
    
    /** Single float parameter. */
    public static FieldEditUpdateS2CPayload floatParam(String category, String param, float value) {
        return new FieldEditUpdateS2CPayload(category, 
            "{\"" + param + "\":" + value + "}");
    }
    
    /** Single int parameter. */
    public static FieldEditUpdateS2CPayload intParam(String category, String param, int value) {
        return new FieldEditUpdateS2CPayload(category,
            "{\"" + param + "\":" + value + "}");
    }
    
    /** Single boolean parameter. */
    public static FieldEditUpdateS2CPayload boolParam(String category, String param, boolean value) {
        return new FieldEditUpdateS2CPayload(category,
            "{\"" + param + "\":" + value + "}");
    }
    
    /** Single string parameter. */
    public static FieldEditUpdateS2CPayload stringParam(String category, String param, String value) {
        return new FieldEditUpdateS2CPayload(category,
            "{\"" + param + "\":\"" + value + "\"}");
    }
    
    /** Full state reset. */
    public static FieldEditUpdateS2CPayload reset() {
        return new FieldEditUpdateS2CPayload("RESET", "{}");
    }
    
    /** Test field control. */
    public static FieldEditUpdateS2CPayload testField(String action) {
        return new FieldEditUpdateS2CPayload("TEST_FIELD", "{\"action\":\"" + action + "\"}");
    }
    
    public static void register() {
        Logging.GUI.topic("network").debug("Registered FieldEditUpdateS2CPayload");
    }
}

