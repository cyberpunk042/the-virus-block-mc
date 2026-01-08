package net.cyberpunk042.util.json;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.cyberpunk042.visual.shape.*;

import java.io.IOException;

/**
 * Gson TypeAdapter for polymorphic Shape serialization.
 * 
 * <p>Handles serialization and deserialization of the Shape interface
 * by using the "type" field to determine the concrete implementation.</p>
 * 
 * <h2>Supported Types</h2>
 * <ul>
 *   <li>sphere → SphereShape</li>
 *   <li>ring → RingShape</li>
 *   <li>prism → PrismShape</li>
 *   <li>cylinder → CylinderShape</li>
 *   <li>polyhedron → PolyhedronShape</li>
 * </ul>
 * 
 * @see Shape
 */
public class ShapeTypeAdapter extends TypeAdapter<Shape> {
    
    private final Gson gson = new Gson();
    
    @Override
    public void write(JsonWriter out, Shape shape) throws IOException {
        if (shape == null) {
            out.nullValue();
            return;
        }
        // Each shape's toJson() includes the "type" field
        JsonObject json = shape.toJson();
        gson.toJson(json, out);
    }
    
    @Override
    public Shape read(JsonReader in) throws IOException {
        JsonElement element = JsonParser.parseReader(in);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        
        JsonObject json = element.getAsJsonObject();
        String type = json.has("type") ? json.get("type").getAsString().toLowerCase() : "sphere";
        
        return switch (type) {
            case "sphere" -> SphereShape.fromJson(json);
            case "ring" -> parseRing(json);
            case "prism" -> parsePrism(json);
            case "cylinder" -> parseCylinder(json);
            case "polyhedron" -> parsePolyhedron(json);
            case "jet" -> parseJet(json);
            case "rays" -> parseRays(json);
            case "molecule" -> parseMolecule(json);
            default -> SphereShape.builder().radius(1.0f).latSteps(32).lonSteps(64).build();
        };
    }
    
    private static RingShape parseRing(JsonObject json) {
        return RingShape.builder()
            .innerRadius(json.has("innerRadius") ? json.get("innerRadius").getAsFloat() : 0.8f)
            .outerRadius(json.has("outerRadius") ? json.get("outerRadius").getAsFloat() : 1.0f)
            .segments(json.has("segments") ? json.get("segments").getAsInt() : 32)
            .build();
    }
    
    private static PrismShape parsePrism(JsonObject json) {
        return PrismShape.builder()
            .sides(json.has("sides") ? json.get("sides").getAsInt() : 6)
            .radius(json.has("radius") ? json.get("radius").getAsFloat() : 1.0f)
            .height(json.has("height") ? json.get("height").getAsFloat() : 2.0f)
            .build();
    }
    
    private static CylinderShape parseCylinder(JsonObject json) {
        return CylinderShape.builder()
            .radius(json.has("radius") ? json.get("radius").getAsFloat() : 1.0f)
            .height(json.has("height") ? json.get("height").getAsFloat() : 2.0f)
            .segments(json.has("segments") ? json.get("segments").getAsInt() : 32)
            .build();
    }
    
    private static PolyhedronShape parsePolyhedron(JsonObject json) {
        PolyType polyType = PolyType.ICOSAHEDRON;
        if (json.has("polyType")) {
            try {
                polyType = PolyType.valueOf(json.get("polyType").getAsString().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return PolyhedronShape.builder()
            .polyType(polyType)
            .radius(json.has("radius") ? json.get("radius").getAsFloat() : 1.0f)
            .build();
    }
    
    private static JetShape parseJet(JsonObject json) {
        float topTipRadius = json.has("topTipRadius") ? json.get("topTipRadius").getAsFloat() : 0f;
        return JetShape.builder()
            .length(json.has("length") ? json.get("length").getAsFloat() : 2.0f)
            .baseRadius(json.has("baseRadius") ? json.get("baseRadius").getAsFloat() : 0.3f)
            .topTipRadius(topTipRadius)
            .bottomTipRadius(json.has("bottomTipRadius") ? json.get("bottomTipRadius").getAsFloat() : topTipRadius)
            .segments(json.has("segments") ? json.get("segments").getAsInt() : 16)
            .lengthSegments(json.has("lengthSegments") ? json.get("lengthSegments").getAsInt() : 1)
            .dualJets(!json.has("dualJets") || json.get("dualJets").getAsBoolean())
            .gap(json.has("gap") ? json.get("gap").getAsFloat() : 0f)
            .hollow(json.has("hollow") && json.get("hollow").getAsBoolean())
            .innerBaseRadius(json.has("innerBaseRadius") ? json.get("innerBaseRadius").getAsFloat() : 0f)
            .innerTipRadius(json.has("innerTipRadius") ? json.get("innerTipRadius").getAsFloat() : 0f)
            .capBase(!json.has("capBase") || json.get("capBase").getAsBoolean())
            .capTip(json.has("capTip") && json.get("capTip").getAsBoolean())
            .build();
    }
    
    private static RaysShape parseRays(JsonObject json) {
        RayArrangement arrangement = RayArrangement.RADIAL;
        if (json.has("arrangement")) {
            arrangement = RayArrangement.fromString(json.get("arrangement").getAsString());
        }
        RayDistribution distribution = RayDistribution.UNIFORM;
        if (json.has("distribution")) {
            try {
                distribution = RayDistribution.valueOf(json.get("distribution").getAsString().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return RaysShape.builder()
            .rayLength(json.has("rayLength") ? json.get("rayLength").getAsFloat() : 2.0f)
            .rayWidth(json.has("rayWidth") ? json.get("rayWidth").getAsFloat() : 1.0f)
            .count(json.has("count") ? json.get("count").getAsInt() : 12)
            .arrangement(arrangement)
            .distribution(distribution)
            .innerRadius(json.has("innerRadius") ? json.get("innerRadius").getAsFloat() : 0.5f)
            .outerRadius(json.has("outerRadius") ? json.get("outerRadius").getAsFloat() : 3.0f)
            .layers(json.has("layers") ? json.get("layers").getAsInt() : 1)
            .layerSpacing(json.has("layerSpacing") ? json.get("layerSpacing").getAsFloat() : 0.5f)
            .randomness(json.has("randomness") ? json.get("randomness").getAsFloat() : 0f)
            .lengthVariation(json.has("lengthVariation") ? json.get("lengthVariation").getAsFloat() : 0f)
            .fadeStart(json.has("fadeStart") ? json.get("fadeStart").getAsFloat() : 1.0f)
            .fadeEnd(json.has("fadeEnd") ? json.get("fadeEnd").getAsFloat() : 1.0f)
            .segments(json.has("segments") ? json.get("segments").getAsInt() : 1)
            .segmentGap(json.has("segmentGap") ? json.get("segmentGap").getAsFloat() : 0f)
            .build();
    }
    
    private static MoleculeShape parseMolecule(JsonObject json) {
        AtomDistribution distribution = AtomDistribution.FIBONACCI;
        if (json.has("distribution")) {
            try {
                distribution = AtomDistribution.valueOf(json.get("distribution").getAsString().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return MoleculeShape.builder()
            .atomCount(json.has("atomCount") ? json.get("atomCount").getAsInt() : 4)
            .atomRadius(json.has("atomRadius") ? json.get("atomRadius").getAsFloat() : 0.3f)
            .atomDistance(json.has("atomDistance") ? json.get("atomDistance").getAsFloat() : 0.8f)
            .neckRadius(json.has("neckRadius") ? json.get("neckRadius").getAsFloat() : 0.12f)
            .neckPinch(json.has("neckPinch") ? json.get("neckPinch").getAsFloat() : 0.5f)
            .connectionDistance(json.has("connectionDistance") ? json.get("connectionDistance").getAsFloat() : 1.2f)
            .seed(json.has("seed") ? json.get("seed").getAsInt() : 42)
            .distribution(distribution)
            .sizeVariation(json.has("sizeVariation") ? json.get("sizeVariation").getAsFloat() : 0.2f)
            .scale(json.has("scale") ? json.get("scale").getAsFloat() : 1.0f)
            .build();
    }
}

