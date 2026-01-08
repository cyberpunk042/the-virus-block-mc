package net.cyberpunk042.client.gui.panel.service;

import net.cyberpunk042.client.gui.state.FieldEditState;
import net.cyberpunk042.client.gui.state.FieldEditState.ProfileEntry;
import net.cyberpunk042.client.gui.util.GuiConfigPersistence;
import net.cyberpunk042.client.profile.ProfileManager;
import net.cyberpunk042.field.category.ProfileSource;
import net.cyberpunk042.field.profile.Profile;
import net.cyberpunk042.log.Logging;
import net.cyberpunk042.network.gui.ProfileSaveC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service layer for profile operations.
 * 
 * <p>Encapsulates all business logic for profile CRUD operations,
 * decoupled from UI concerns. The panel calls this service and
 * handles success/error feedback separately.</p>
 * 
 * <p>Each operation returns a {@link Result} to allow the caller
 * to handle success/error UI feedback.</p>
 */
public class ProfileActionService {
    
    private final FieldEditState state;
    private final ProfileManager profileManager;
    
    public ProfileActionService(FieldEditState state) {
        this.state = state;
        this.profileManager = ProfileManager.getInstance();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LOAD
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Loads a profile by entry, applying it to the FieldEditState.
     */
    public Result<String> load(ProfileEntry entry) {
        if (entry == null) {
            return Result.failure("No profile selected");
        }
        
        try {
            // Special handling for 'default' profile - it's a virtual profile that resets to factory
            if ("default".equalsIgnoreCase(entry.name())) {
                state.reset();
                state.setCurrentProfile("default", false);
                state.clearDirty();
                Logging.GUI.info("Loaded default profile (factory settings)");
                return Result.success("Loaded: default (factory settings)");
            }
            
            var profileOpt = profileManager.getProfileByName(entry.name());
            
            if (profileOpt.isPresent()) {
                var profile = profileOpt.get();
                if (profile.definition() != null) {
                    state.loadFromDefinition(profile.definition());
                    state.setCurrentProfile(entry.name(), entry.isServer());
                    state.clearDirty();
                    
                    // Persist for next startup (local only)
                    if (entry.isLocal()) {
                        GuiConfigPersistence.saveCurrentProfile(entry.name());
                    }
                    
                    Logging.GUI.info("Loaded profile: {} (source={})", entry.name(), profile.source());
                    return Result.success("Loaded: " + entry.name());
                } else {
                    Logging.GUI.warn("Profile {} has null definition", entry.name());
                    return Result.failure("Profile has no definition: " + entry.name());
                }
            } else {
                // Server profiles may not be locally available
                if (entry.isServer()) {
                    state.setCurrentProfile(entry.name(), true);
                    return Result.success("Server profile selected: " + entry.name());
                } else {
                    Logging.GUI.error("Profile not found by name: {}", entry.name());
                    return Result.failure("Profile not found: " + entry.name());
                }
            }
        } catch (Exception e) {
            Logging.GUI.error("Failed to load profile: {}", entry.name(), e);
            return Result.failure("Load failed: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SAVE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Saves current state to the given profile entry.
     */
    public Result<String> save(ProfileEntry entry) {
        if (entry == null) {
            return Result.failure("No profile selected");
        }
        if (entry.isServer()) {
            return Result.failure("Cannot save to server profiles locally");
        }
        if (entry.isBundled()) {
            return Result.failure("Cannot overwrite bundled profiles");
        }
        
        try {
            var definition = net.cyberpunk042.client.gui.state.DefinitionBuilder.fromState(state);
            
            var profile = Profile.builder()
                .id(entry.name())
                .name(entry.name())
                .source(ProfileSource.LOCAL)
                .definition(definition)
                .build();
            
            profileManager.saveProfile(profile);
            GuiConfigPersistence.saveCurrentProfile(entry.name());
            
            state.setCurrentProfile(entry.name(), false);
            state.clearDirty();
            
            Logging.GUI.info("Saved profile: {}", entry.name());
            return Result.success("Saved: " + entry.name());
        } catch (Exception e) {
            Logging.GUI.error("Failed to save profile: {}", entry.name(), e);
            return Result.failure("Save failed: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SAVE AS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Validates a new profile name for Save As operation.
     */
    public Result<String> validateNewName(String name, List<ProfileEntry> existingProfiles) {
        if (name == null || name.trim().isEmpty()) {
            return Result.failure("Enter a profile name");
        }
        
        String trimmed = name.trim();
        String id = trimmed.toLowerCase().replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
        
        if (id.isEmpty()) {
            id = "profile_" + System.currentTimeMillis();
        }
        
        for (ProfileEntry p : existingProfiles) {
            if (p.name().equalsIgnoreCase(trimmed) || 
                id.equals(p.name().toLowerCase().replaceAll("\\s+", "_"))) {
                return Result.failure("Profile '" + trimmed + "' already exists");
            }
        }
        
        return Result.success(trimmed);
    }
    
    /**
     * Saves current state as a new profile with the given name.
     */
    public Result<String> saveAs(String name) {
        String trimmed = name.trim();
        String id = trimmed.toLowerCase().replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
        if (id.isEmpty()) {
            id = "profile_" + System.currentTimeMillis();
        }
        
        try {
            var definition = net.cyberpunk042.client.gui.state.DefinitionBuilder.fromState(state);
            
            var profile = Profile.builder()
                .id(id)
                .name(trimmed)
                .source(ProfileSource.LOCAL)
                .definition(definition)
                .build();
            
            profileManager.saveProfile(profile);
            
            state.setCurrentProfile(trimmed, false);
            state.clearDirty();
            
            GuiConfigPersistence.saveCurrentProfile(trimmed);
            
            Logging.GUI.info("Created profile: {}", trimmed);
            return Result.success("Saved as: " + trimmed);
        } catch (Exception e) {
            Logging.GUI.error("Failed to save as: {}", trimmed, e);
            return Result.failure("Save failed: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Validates whether a profile can be deleted.
     */
    public Result<Void> canDelete(ProfileEntry entry) {
        if (entry == null) {
            return Result.failure("Select a profile first");
        }
        if (entry.isServer()) {
            return Result.failure("Cannot delete server profiles");
        }
        if (entry.isBundled()) {
            return Result.failure("Cannot delete bundled profiles");
        }
        if ("default".equalsIgnoreCase(entry.name())) {
            return Result.failure("Cannot delete default profile");
        }
        return Result.success(null);
    }
    
    /**
     * Deletes the given profile.
     */
    public Result<String> delete(ProfileEntry entry) {
        Result<Void> canDelete = canDelete(entry);
        if (!canDelete.isSuccess()) {
            return Result.failure(canDelete.error());
        }
        
        boolean deleted = profileManager.deleteProfileByName(entry.name());
        if (deleted) {
            Logging.GUI.info("Deleted profile: {}", entry.name());
            return Result.success("Deleted: " + entry.name());
        } else {
            return Result.failure("Failed to delete profile");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DUPLICATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Generates a unique copy name for duplication.
     */
    public String generateCopyName(String baseName, List<ProfileEntry> existingProfiles) {
        String copyName = baseName + " (copy)";
        int counter = 1;
        
        while (profileNameExists(copyName, existingProfiles)) {
            copyName = baseName + " (copy " + (++counter) + ")";
        }
        
        return copyName;
    }
    
    private boolean profileNameExists(String name, List<ProfileEntry> profiles) {
        for (ProfileEntry p : profiles) {
            if (p.name().equals(name)) return true;
        }
        return false;
    }
    
    /**
     * Duplicates a profile entry (metadata only - profile list update).
     * Actual file duplication would need saveAs() to be called after.
     */
    public Result<String> duplicate(ProfileEntry entry, List<ProfileEntry> existingProfiles) {
        if (entry == null) {
            return Result.failure("Select a profile first");
        }
        
        String copyName = generateCopyName(entry.name(), existingProfiles);
        Logging.GUI.info("Duplicated profile {} to {}", entry.name(), copyName);
        return Result.success(copyName);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENAME
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Validates a rename operation.
     */
    public Result<Void> canRename(ProfileEntry entry, String newName, List<ProfileEntry> profiles) {
        if (entry == null) {
            return Result.failure("Select a profile first");
        }
        if (entry.isServer()) {
            return Result.failure("Cannot rename server profiles");
        }
        if (entry.isBundled()) {
            return Result.failure("Cannot rename bundled profiles");
        }
        if (newName == null || newName.trim().isEmpty()) {
            return Result.failure("Enter a new name");
        }
        if (newName.equals(entry.name())) {
            return Result.failure("Name is unchanged");
        }
        
        for (ProfileEntry p : profiles) {
            if (p.name().equals(newName)) {
                return Result.failure("Name already exists");
            }
        }
        
        return Result.success(null);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FACTORY RESET
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Resets a profile to its factory/bundled state.
     */
    public Result<String> factoryReset(ProfileEntry entry) {
        if (entry == null) {
            return Result.failure("No profile selected");
        }
        
        boolean isDefault = "default".equalsIgnoreCase(entry.name());
        
        // Default profile: reset to factory settings
        if (isDefault) {
            state.reset();
            state.setCurrentProfile("default", false);
            state.clearDirty();
            Logging.GUI.info("Reset to factory default");
            return Result.success("Reset to factory default");
        }
        
        // Any other profile: reload from disk
        var profileOpt = profileManager.getProfileByName(entry.name());
        if (profileOpt.isPresent() && profileOpt.get().definition() != null) {
            state.loadFromDefinition(profileOpt.get().definition());
            state.setCurrentProfile(entry.name(), entry.isServer());
            state.clearDirty();
            Logging.GUI.info("Reset profile: {} (reloaded from disk)", entry.name());
            return Result.success("Reset: " + entry.name());
        } else {
            Logging.GUI.error("Reset failed - no definition for: {}", entry.name());
            return Result.failure("Could not reload: " + entry.name());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SAVE TO SERVER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Sends a profile to the server (OP only).
     */
    public Result<String> saveToServer(ProfileEntry entry) {
        if (entry == null) {
            return Result.failure("No profile selected");
        }
        
        String jsonStr = state.toProfileJson(entry.name());
        ClientPlayNetworking.send(ProfileSaveC2SPayload.saveToServer(entry.name(), jsonStr));
        Logging.GUI.info("Sent profile to server: {}", entry.name());
        return Result.success("Sent to server: " + entry.name());
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUTTON STATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Determines if the given profile is the "default" profile.
     */
    public boolean isDefaultProfile(ProfileEntry entry) {
        return entry != null && "default".equalsIgnoreCase(entry.name());
    }
    
    /**
     * Checks if the current player has OP permissions.
     */
    public boolean isPlayerOp() {
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        return player != null && player.hasPermissionLevel(2);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RESULT TYPE
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Simple result type for operation outcomes.
     */
    public record Result<T>(T value, String error) {
        
        public boolean isSuccess() {
            return error == null;
        }
        
        public static <T> Result<T> success(T value) {
            return new Result<>(value, null);
        }
        
        public static <T> Result<T> failure(String error) {
            return new Result<>(null, error);
        }
        
        public void onSuccess(Consumer<T> action) {
            if (isSuccess() && value != null) {
                action.accept(value);
            }
        }
        
        public void onError(Consumer<String> action) {
            if (!isSuccess()) {
                action.accept(error);
            }
        }
        
        public void handle(Consumer<T> onSuccess, Consumer<String> onError) {
            if (isSuccess()) {
                if (value != null) onSuccess.accept(value);
            } else {
                onError.accept(error);
            }
        }
    }
}
