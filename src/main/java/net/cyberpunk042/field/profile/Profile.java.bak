package net.cyberpunk042.field.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.cyberpunk042.field.category.ProfileCategory;
import net.cyberpunk042.field.category.ProfileSource;
import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.field.FieldType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A saved field profile with metadata.
 * Profiles can come from bundled assets, local files, or server.
 */
public record Profile(
    int version,
    String id,
    String name,
    String description,
    FieldType type,
    ProfileCategory category,
    List<String> tags,
    ProfileSource source,
    Instant created,
    Instant modified,
    FieldDefinition definition
) {
    
    public static final int CURRENT_VERSION = 1;
    
    /**
     * Get display name with category: "Profile Name (category)"
     */
    public String getDisplayName() {
        return name + " (" + category.getId() + ")";
    }
    
    /**
     * Check if this profile can be edited/saved.
     */
    public boolean isEditable() {
        return source.isEditable();
    }
    
    /**
     * Get UI icon based on source.
     */
    public String getSourceIcon() {
        return source.getIcon();
    }
    
    /**
     * Check if profile has a specific tag.
     */
    public boolean hasTag(String tag) {
        return tags != null && tags.stream()
            .anyMatch(t -> t.equalsIgnoreCase(tag));
    }
    
    /**
     * Create from JSON.
     */
    public static Profile fromJson(JsonObject json, ProfileSource source) {
        int version = json.has("version") ? json.get("version").getAsInt() : 1;
        String id = json.has("id") ? json.get("id").getAsString() : "unknown";
        String name = json.has("name") ? json.get("name").getAsString() : id;
        String description = json.has("description") ? json.get("description").getAsString() : "";
        
        // Type (functional)
        FieldType type = FieldType.SHIELD;
        if (json.has("type")) {
            try {
                type = FieldType.valueOf(json.get("type").getAsString().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        
        // Category (organizational)
        ProfileCategory category = ProfileCategory.UTILITY;
        if (json.has("category")) {
            category = ProfileCategory.fromId(json.get("category").getAsString());
        }
        
        // Tags
        List<String> tags = new ArrayList<>();
        if (json.has("tags") && json.get("tags").isJsonArray()) {
            for (var elem : json.getAsJsonArray("tags")) {
                tags.add(elem.getAsString());
            }
        }
        
        // Timestamps
        Instant created = Instant.now();
        Instant modified = Instant.now();
        if (json.has("created")) {
            try {
                created = Instant.parse(json.get("created").getAsString());
            } catch (Exception ignored) {}
        }
        if (json.has("modified")) {
            try {
                modified = Instant.parse(json.get("modified").getAsString());
            } catch (Exception ignored) {}
        }
        
        // Definition - the actual field data
        FieldDefinition definition = null;
        if (json.has("definition") && json.get("definition").isJsonObject()) {
            definition = FieldDefinition.fromJson(json.getAsJsonObject("definition"));
        }
        
        return new Profile(version, id, name, description, type, category, tags, source, created, modified, definition);
    }
    
    /**
     * Convert to JSON for saving.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("version", version);
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("description", description);
        json.addProperty("type", type.name());
        json.addProperty("category", category.getId());
        
        JsonArray tagsArray = new JsonArray();
        if (tags != null) {
            tags.forEach(tagsArray::add);
        }
        json.add("tags", tagsArray);
        
        json.addProperty("created", created.toString());
        json.addProperty("modified", Instant.now().toString());
        
        if (definition != null) {
            json.add("definition", definition.toJson());
        }
        
        return json;
    }
    
    /**
     * Create a copy with updated definition and modified timestamp.
     */
    public Profile withDefinition(FieldDefinition newDefinition) {
        return new Profile(
            version, id, name, description, type, category, tags, source,
            created, Instant.now(), newDefinition
        );
    }
    
    /**
     * Create a copy with new name (for Save As).
     */
    public Profile withName(String newName, String newId) {
        return new Profile(
            version, newId, newName, description, type, category, tags,
            ProfileSource.LOCAL, // Save As always creates local
            Instant.now(), Instant.now(), definition
        );
    }
    
    /**
     * Create a copy with different category.
     */
    public Profile withCategory(ProfileCategory newCategory) {
        return new Profile(
            version, id, name, description, type, newCategory, tags, source,
            created, Instant.now(), definition
        );
    }
    
    /**
     * Builder for creating new profiles.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id = "new_profile";
        private String name = "New Profile";
        private String description = "";
        private FieldType type = FieldType.SHIELD;
        private ProfileCategory category = ProfileCategory.UTILITY;
        private List<String> tags = new ArrayList<>();
        private FieldDefinition definition;
        
        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder type(FieldType type) { this.type = type; return this; }
        public Builder category(ProfileCategory category) { this.category = category; return this; }
        public Builder tags(List<String> tags) { this.tags = tags; return this; }
        public Builder tag(String tag) { this.tags.add(tag); return this; }
        public Builder definition(FieldDefinition definition) { this.definition = definition; return this; }
        
        public Profile build() {
            return new Profile(
                CURRENT_VERSION, id, name, description, type, category, tags,
                ProfileSource.LOCAL, Instant.now(), Instant.now(), definition
            );
        }
    }
}
