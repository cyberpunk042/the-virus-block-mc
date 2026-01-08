package net.cyberpunk042.sphere;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.cyberpunk042.log.Logging;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates Minecraft JSON block/item models that render as spheres.
 *
 * <p>This creates static model files for:
 * <ul>
 *   <li>{@code assets/yourmod/models/block/sphere.json} (for blocks)</li>
 *   <li>{@code assets/yourmod/models/item/sphere.json} (for items)</li>
 * </ul>
 *
 * <h2>Algorithm Types</h2>
 * <table>
 *   <tr><th>Type</th><th>Method</th><th>Elements</th><th>Best For</th></tr>
 *   <tr><td>TYPE_A</td><td>Overlapping cubes</td><td>10-15</td><td>Static, accurate</td></tr>
 *   <tr><td>TYPE_E</td><td>Rotated rectangles</td><td>40-60</td><td>Efficient, simpler</td></tr>
 * </table>
 *
 * <h2>Usage</h2>
 * <pre>
 * // Generate Type A sphere for a block
 * JsonObject model = SphereModelGenerator.builder()
 *     .typeA()
 *     .radius(1.0)
 *     .verticalLayers(6)
 *     .horizontalDetail(4)
 *     .texture("the-virus-block:block/growth_sphere")
 *     .build();
 *
 * // Save to resources
 * SphereModelGenerator.saveToFile(model,
 *     Path.of("src/main/resources/assets/the-virus-block/models/block/sphere.json"));
 *
 * // For item with scaling
 * JsonObject itemModel = SphereModelGenerator.builder()
 *     .typeA()
 *     .radius(0.5)
 *     .verticalLayers(4)
 *     .texture("the-virus-block:item/orb")
 *     .scale(0.6, 0.6, 0.6)
 *     .translation(0, 4, 0)
 *     .build();
 * </pre>
 *
 * <p>Based on MC Sphere Model Generator by Dylan Dang.</p>
 *
 * @see net.cyberpunk042.client.visual.mesh.sphere.TypeASphereRenderer
 * @see net.cyberpunk042.client.visual.mesh.sphere.TypeESphereRenderer
 */
public final class SphereModelGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson GSON_COMPACT = new Gson();

    private SphereModelGenerator() {}

    /**
     * Sphere generation algorithm type.
     */
    public enum SphereType {
        /**
         * Type A (Accuracy): Uses overlapping axis-aligned cubes with different
         * aspect ratios. Produces smoother results but more elements.
         */
        TYPE_A,

        /**
         * Type E (Efficiency): Uses thin rectangles rotated around Y-axis to form
         * a polygon (hexadecagon). Fewer elements, still looks round.
         */
        TYPE_E
    }

    // =========================================================================
    // Builder
    // =========================================================================

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private SphereType type = SphereType.TYPE_A;
        private double radius = 1.0;
        private int verticalLayers = 6;
        private int horizontalDetail = 4;
        private String texture = "#sphere";
        private boolean shade = false;
        private double[] scale = {1, 1, 1};
        private double[] translation = {0, 0, 0};
        private double[] rotation = {0, 0, 0};

        private Builder() {}

        /**
         * Use Type A algorithm (overlapping cubes, more accurate).
         */
        public Builder typeA() {
            this.type = SphereType.TYPE_A;
            return this;
        }

        /**
         * Use Type E algorithm (rotated rectangles, more efficient).
         */
        public Builder typeE() {
            this.type = SphereType.TYPE_E;
            return this;
        }

        /**
         * Set sphere radius (1.0 = fills one block, 0.5 = half block).
         */
        public Builder radius(double radius) {
            this.radius = Math.max(0.1, Math.min(2.0, radius));
            return this;
        }

        /**
         * Number of vertical layers (more = smoother, 4-8 recommended).
         */
        public Builder verticalLayers(int layers) {
            this.verticalLayers = Math.max(2, Math.min(12, layers));
            return this;
        }

        /**
         * Horizontal detail per layer (more = smoother, 2-4 recommended).
         * Only affects Type A.
         */
        public Builder horizontalDetail(int detail) {
            this.horizontalDetail = Math.max(1, Math.min(6, detail));
            return this;
        }

        /**
         * Texture reference (e.g., "yourmod:block/sphere" or "#layer0" for items).
         */
        public Builder texture(String texture) {
            this.texture = texture;
            return this;
        }

        /**
         * Enable shading (ambient occlusion). Usually looks weird on spheres.
         */
        public Builder shade(boolean shade) {
            this.shade = shade;
            return this;
        }

        /**
         * Set display scale for head/gui/etc (max 4.0 per axis).
         */
        public Builder scale(double x, double y, double z) {
            this.scale = new double[]{
                Math.min(4.0, x),
                Math.min(4.0, y),
                Math.min(4.0, z)
            };
            return this;
        }

        /**
         * Set display translation (max ±80 per axis).
         */
        public Builder translation(double x, double y, double z) {
            this.translation = new double[]{
                Math.max(-80, Math.min(80, x)),
                Math.max(-80, Math.min(80, y)),
                Math.max(-80, Math.min(80, z))
            };
            return this;
        }

        /**
         * Set display rotation.
         */
        public Builder rotation(double x, double y, double z) {
            this.rotation = new double[]{x, y, z};
            return this;
        }

        /**
         * Build the model JSON.
         */
        public JsonObject build() {
            Logging.REGISTRY.topic("sphere").debug(
                "Generating {} sphere model: radius={}, layers={}, detail={}, texture={}",
                type, radius, verticalLayers, horizontalDetail, texture);

            return switch (type) {
                case TYPE_A -> generateTypeA();
                case TYPE_E -> generateTypeE();
            };
        }

        // =====================================================================
        // Type A Generation
        // =====================================================================

        private JsonObject generateTypeA() {
            JsonObject model = new JsonObject();
            JsonArray elements = new JsonArray();

            // Texture references
            JsonObject textures = new JsonObject();
            textures.addProperty("sphere", texture);
            textures.addProperty("particle", texture);
            model.add("textures", textures);

            int actualSubdivision = verticalLayers + 1;
            double verticalStep = 90.0 / actualSubdivision;

            for (double levelDegrees = verticalStep; levelDegrees < 90.0; levelDegrees += verticalStep) {
                double levelRadians = Math.toRadians(levelDegrees);

                double layerRadius = radius * 8.0 * Math.cos(levelRadians);
                double y = radius * 8.0 * Math.sin(levelRadians);

                // Adaptive subdivision (fewer cubes at poles)
                int quadSubLevel = Math.max(1, (int) Math.round(horizontalDetail * layerRadius / (radius * 8.0)));
                double horizStep = 90.0 / quadSubLevel;

                for (double degrees = horizStep; degrees < 90.0; degrees += horizStep) {
                    double radians = Math.toRadians(degrees);

                    double x = layerRadius * Math.cos(radians);
                    double z = layerRadius * Math.sin(radians);

                    JsonObject element = createCubeElement(
                        8.0 - x, 8.0 - y, 8.0 - z,
                        8.0 + x, 8.0 + y, 8.0 + z,
                        0, "y"
                    );
                    elements.add(element);
                }
            }

            model.add("elements", elements);
            model.add("display", createDisplay());

            Logging.REGISTRY.topic("sphere").info(
                "Generated Type A sphere: {} elements", elements.size());

            return model;
        }

        // =====================================================================
        // Type E Generation
        // =====================================================================

        private JsonObject generateTypeE() {
            JsonObject model = new JsonObject();
            JsonArray elements = new JsonArray();

            // Texture references
            JsonObject textures = new JsonObject();
            textures.addProperty("sphere", texture);
            textures.addProperty("particle", texture);
            model.add("textures", textures);

            int actualSubdivision = verticalLayers + 1;
            double verticalStep = 90.0 / actualSubdivision;
            double sideRatio = Math.tan(Math.PI / 16.0); // hexadecagon (16 sides)

            for (double levelDegrees = verticalStep; levelDegrees < 90.0; levelDegrees += verticalStep) {
                double levelRadians = Math.toRadians(levelDegrees);

                double layerRadius = radius * 8.0 * Math.cos(levelRadians);
                double y = radius * 8.0 * Math.sin(levelRadians);
                double side = layerRadius * sideRatio;

                // Z-aligned thin rectangles at various rotations
                for (double angle = -45.0; angle < 67.5; angle += 22.5) {
                    JsonObject element = createCubeElement(
                        8.0 - side, 8.0 - y, 8.0 - layerRadius,
                        8.0 + side, 8.0 + y, 8.0 + layerRadius,
                        angle, "y"
                    );
                    elements.add(element);
                }

                // X-aligned thin rectangles
                for (double angle = -22.5; angle < 45.0; angle += 22.5) {
                    JsonObject element = createCubeElement(
                        8.0 - layerRadius, 8.0 - y, 8.0 - side,
                        8.0 + layerRadius, 8.0 + y, 8.0 + side,
                        angle, "y"
                    );
                    elements.add(element);
                }
            }

            model.add("elements", elements);
            model.add("display", createDisplay());

            Logging.REGISTRY.topic("sphere").info(
                "Generated Type E sphere: {} elements", elements.size());

            return model;
        }

        // =====================================================================
        // Helpers
        // =====================================================================

        private JsonObject createCubeElement(double x1, double y1, double z1,
                                              double x2, double y2, double z2,
                                              double angle, String axis) {
            JsonObject element = new JsonObject();

            // From/To coordinates
            JsonArray from = new JsonArray();
            from.add(x1);
            from.add(y1);
            from.add(z1);
            element.add("from", from);

            JsonArray to = new JsonArray();
            to.add(x2);
            to.add(y2);
            to.add(z2);
            element.add("to", to);

            element.addProperty("shade", shade);

            // Add rotation if angle != 0
            if (angle != 0) {
                JsonObject rot = new JsonObject();
                rot.addProperty("angle", angle);
                rot.addProperty("axis", axis);
                JsonArray origin = new JsonArray();
                origin.add(8);
                origin.add(8);
                origin.add(8);
                rot.add("origin", origin);
                element.add("rotation", rot);
            }

            // Add faces with UV mapping
            JsonObject faces = new JsonObject();
            addFace(faces, "north", x1, y1, x2, y2);
            addFace(faces, "south", x1, y1, x2, y2);
            addFace(faces, "west", z1, y1, z2, y2);
            addFace(faces, "east", z1, y1, z2, y2);
            addFace(faces, "up", x1, z1, x2, z2);
            addFace(faces, "down", x1, z1, x2, z2);
            element.add("faces", faces);

            return element;
        }

        private void addFace(JsonObject faces, String name,
                             double u1, double v1, double u2, double v2) {
            JsonObject face = new JsonObject();
            JsonArray uv = new JsonArray();
            uv.add(u1);
            uv.add(v1);
            uv.add(u2);
            uv.add(v2);
            face.add("uv", uv);
            face.addProperty("texture", "#sphere");
            faces.add(name, face);
        }

        private JsonObject createDisplay() {
            JsonObject display = new JsonObject();

            String[] contexts = {
                "thirdperson_righthand", "thirdperson_lefthand",
                "firstperson_righthand", "firstperson_lefthand",
                "gui", "head", "ground", "fixed"
            };

            for (String context : contexts) {
                JsonObject settings = new JsonObject();

                JsonArray rot = new JsonArray();
                rot.add(rotation[0]);
                rot.add(rotation[1]);
                rot.add(rotation[2]);
                settings.add("rotation", rot);

                JsonArray trans = new JsonArray();
                trans.add(translation[0]);
                trans.add(translation[1]);
                trans.add(translation[2]);
                settings.add("translation", trans);

                JsonArray scl = new JsonArray();
                scl.add(scale[0]);
                scl.add(scale[1]);
                scl.add(scale[2]);
                settings.add("scale", scl);

                display.add(context, settings);
            }

            return display;
        }
    }

    // =========================================================================
    // File I/O
    // =========================================================================

    /**
     * Save model to a JSON file.
     *
     * @param model The generated model JSON
     * @param path The file path to save to
     * @throws IOException If file cannot be written
     */
    public static void saveToFile(JsonObject model, Path path) throws IOException {
        // Ensure parent directories exist
        if (path.getParent() != null) {
            path.getParent().toFile().mkdirs();
        }

        try (FileWriter writer = new FileWriter(path.toFile())) {
            GSON.toJson(model, writer);
        }

        Logging.REGISTRY.topic("sphere").info("Saved sphere model to: {}", path);
    }

    /**
     * Convert model to formatted JSON string.
     */
    public static String toJsonString(JsonObject model) {
        return GSON.toJson(model);
    }

    /**
     * Convert model to compact JSON string (no formatting).
     */
    public static String toCompactJsonString(JsonObject model) {
        return GSON_COMPACT.toJson(model);
    }

    // =========================================================================
    // Quick Generation Methods
    // =========================================================================

    /**
     * Generate a simple Type A sphere model with default settings.
     */
    public static JsonObject simpleTypeA(String texture) {
        return builder().typeA().texture(texture).build();
    }

    /**
     * Generate a simple Type E sphere model with default settings.
     */
    public static JsonObject simpleTypeE(String texture) {
        return builder().typeE().texture(texture).build();
    }

    /**
     * Generate a sphere suitable for progressive growth block.
     *
     * @param texture The texture identifier
     * @param radius The radius (0.5 = half block)
     * @return The model JSON
     */
    public static JsonObject forGrowthBlock(String texture, double radius) {
        return builder()
            .typeA()
            .radius(radius)
            .verticalLayers(6)
            .horizontalDetail(4)
            .texture(texture)
            .shade(false)
            .build();
    }

    // =========================================================================
    // Estimation Methods
    // =========================================================================

    /**
     * Estimate element count for Type A sphere.
     */
    public static int estimateTypeAElements(int verticalLayers, int horizontalDetail) {
        int count = 0;
        int actualSubdivision = verticalLayers + 1;
        double verticalStep = 90.0 / actualSubdivision;

        for (double levelDegrees = verticalStep; levelDegrees < 90.0; levelDegrees += verticalStep) {
            double levelRadians = Math.toRadians(levelDegrees);
            double layerRadius = Math.cos(levelRadians);
            int quadSubLevel = Math.max(1, (int) Math.round(horizontalDetail * layerRadius));
            double horizStep = 90.0 / quadSubLevel;
            for (double degrees = horizStep; degrees < 90.0; degrees += horizStep) {
                count++;
            }
        }
        return count;
    }

    /**
     * Estimate element count for Type E sphere.
     * Type E has 8 elements per layer (5 Z-aligned + 3 X-aligned).
     */
    public static int estimateTypeEElements(int verticalLayers) {
        int actualSubdivision = verticalLayers + 1;
        int layerCount = actualSubdivision - 1;
        return layerCount * 8;
    }
}

