package net.cyberpunk042.client.gui.state;

/**
 * Listener interface for state changes in {@link FieldEditState}.
 * 
 * <p>Implementations receive notifications when:
 * <ul>
 *   <li>Profile is loaded (all widgets need sync)</li>
 *   <li>Layer or primitive selection changes (adapters have new data)</li>
 *   <li>Fragment/preset is applied (may need rebuild)</li>
 *   <li>Property changes from external source (command, network)</li>
 * </ul>
 * 
 * <p><b>Usage:</b></p>
 * <pre>
 * public class MyPanel extends BoundPanel implements StateChangeListener {
 *     &#64;Override
 *     public void onStateChanged(ChangeType type) {
 *         switch (type) {
 *             case PROFILE_LOADED, PRIMITIVE_SWITCHED -&gt; syncAllFromState();
 *             case FRAGMENT_APPLIED -&gt; {
 *                 if (needsRebuild()) rebuildWidgets();
 *                 else syncAllFromState();
 *             }
 *         }
 *     }
 * }
 * </pre>
 * 
 * @see ChangeType
 * @see FieldEditState#addStateListener(StateChangeListener)
 */
@FunctionalInterface
public interface StateChangeListener {
    
    /**
     * Called when state changes externally (not from this panel's widgets).
     * 
     * <p>Implementations should:
     * <ol>
     *   <li>Check the {@link ChangeType} to determine response</li>
     *   <li>For major changes: sync all widget values from state</li>
     *   <li>For mode changes: potentially rebuild widgets</li>
     * </ol>
     * 
     * @param changeType The type of change that occurred
     */
    void onStateChanged(ChangeType changeType);
}
