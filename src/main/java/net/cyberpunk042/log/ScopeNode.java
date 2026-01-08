package net.cyberpunk042.log;

import java.util.ArrayList;
import java.util.List;

/**
 * A node in a hierarchical log scope tree.
 * 
 * <p>Used by {@link LogScope} to build deferred, structured log output.
 * Nodes can have key-value pairs and child nodes (branches or leaves).</p>
 * 
 * <h3>Example Structure:</h3>
 * <pre>
 * render-frame
 * ├─ layer:0 {primitives=5, visible=true}
 * │  ├─ prim:sphere {vertices=1089}
 * │  └─ prim:cube {vertices=24}
 * └─ layer:1 {primitives=2}
 *    └─ prim:ring {vertices=256}
 * </pre>
 * 
 * @see LogScope
 */
public class ScopeNode {
    
    private final String name;
    private final List<String> kvPairs = new ArrayList<>();
    private final List<ScopeNode> children = new ArrayList<>();
    private ScopeNode parent;
    private boolean isLeaf = false;
    
    /**
     * Creates a new scope node with the given name.
     * @param name The node name (displayed in output)
     */
    public ScopeNode(String name) {
        this.name = name;
    }
    
    // =========================================================================
    // Tree Building
    // =========================================================================
    
    /**
     * Creates a child branch node.
     * 
     * <p>Branches can have their own children and key-value pairs.
     * Returns the new child node for chaining.</p>
     * 
     * @param name The branch name
     * @return The new child node
     */
    public ScopeNode branch(String name) {
        ScopeNode child = new ScopeNode(name);
        child.parent = this;
        children.add(child);
        return child;
    }
    
    /**
     * Adds an existing node as a child (for merging trees).
     * 
     * <p>Used by LogScope auto-nesting to merge child scopes into parent.</p>
     * 
     * @param child The node to add
     * @return This node (for chaining)
     */
    public ScopeNode addChild(ScopeNode child) {
        child.parent = this;
        children.add(child);
        return this;
    }
    
    /**
     * Adds all children from another node (for merging trees).
     * 
     * @param other The node whose children to adopt
     * @return This node (for chaining)
     */
    public ScopeNode mergeChildren(ScopeNode other) {
        for (ScopeNode child : other.children) {
            child.parent = this;
            children.add(child);
        }
        return this;
    }
    
    /**
     * Creates a child leaf node (no further children expected).
     * 
     * <p>Returns THIS node (not the leaf) for continued chaining on the parent.</p>
     * 
     * @param text The leaf text
     * @return This node (for chaining)
     */
    public ScopeNode leaf(String text) {
        ScopeNode leaf = new ScopeNode(text);
        leaf.parent = this;
        leaf.isLeaf = true;
        children.add(leaf);
        return this;
    }
    
    /**
     * Adds a key-value pair to this node.
     * 
     * @param key The key name
     * @param value The value (formatted via {@link LogFormat})
     * @return This node (for chaining)
     */
    public ScopeNode kv(String key, Object value) {
        if (key != null) {
            kvPairs.add(key + "=" + LogFormat.format(value));
        }
        return this;
    }
    
    /**
     * Adds multiple key-value pairs at once.
     * 
     * @param pairs Alternating key, value, key, value...
     * @return This node (for chaining)
     */
    public ScopeNode kvs(Object... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String key = String.valueOf(pairs[i]);
            Object value = pairs[i + 1];
            kvPairs.add(key + "=" + LogFormat.format(value));
        }
        return this;
    }
    
    /**
     * Navigates back to the parent node.
     * 
     * @return The parent node, or this node if at root
     */
    public ScopeNode up() {
        return parent != null ? parent : this;
    }
    
    /**
     * Navigates back to the root node.
     * 
     * @return The root node of this tree
     */
    public ScopeNode root() {
        ScopeNode node = this;
        while (node.parent != null) {
            node = node.parent;
        }
        return node;
    }
    
    // =========================================================================
    // Summary / Aggregation
    // =========================================================================
    
    /**
     * Adds a count summary to this node.
     * Useful for summarizing loop iterations.
     * 
     * @param name The count name
     * @param value The count value
     * @return This node (for chaining)
     */
    public ScopeNode count(String name, int value) {
        kvPairs.add(name + "=" + value);
        return this;
    }
    
    /**
     * Adds a timing summary to this node.
     * 
     * @param millis Time in milliseconds
     * @return This node (for chaining)
     */
    public ScopeNode timing(long millis) {
        if (millis < 1000) {
            kvPairs.add("time=" + millis + "ms");
        } else {
            kvPairs.add("time=" + String.format("%.2fs", millis / 1000.0));
        }
        return this;
    }
    
    /**
     * Marks this node as having an error.
     * 
     * @param message Error message
     * @return This node (for chaining)
     */
    public ScopeNode error(String message) {
        kvPairs.add("ERROR=" + message);
        return this;
    }
    
    /**
     * Marks this node as skipped with a reason.
     * 
     * @param reason Why it was skipped
     * @return This node (for chaining)
     */
    public ScopeNode skipped(String reason) {
        kvPairs.add("SKIPPED=" + reason);
        return this;
    }
    
    // =========================================================================
    // Accessors
    // =========================================================================
    
    public String name() { return name; }
    public ScopeNode parent() { return parent; }
    public List<ScopeNode> children() { return children; }
    public boolean isLeaf() { return isLeaf; }
    public boolean hasChildren() { return !children.isEmpty(); }
    public int depth() {
        int d = 0;
        ScopeNode n = parent;
        while (n != null) {
            d++;
            n = n.parent;
        }
        return d;
    }
    
    // =========================================================================
    // Rendering
    // =========================================================================
    
    /**
     * Renders this node and all descendants as a formatted string.
     * 
     * @return The rendered tree structure
     */
    public String render() {
        StringBuilder sb = new StringBuilder();
        render(sb, 0, true);
        return sb.toString();
    }
    
    /**
     * Renders with a custom indent and prefix.
     */
    private void render(StringBuilder sb, int depth, boolean isLast) {
        // Build indent based on depth
        if (depth > 0) {
            sb.append(getIndent(depth - 1));
            sb.append(isLast ? "└─ " : "├─ ");
        }
        
        // Node name
        sb.append(name);
        
        // Key-value pairs in braces
        if (!kvPairs.isEmpty()) {
            sb.append(" {").append(String.join(", ", kvPairs)).append("}");
        }
        
        sb.append("\n");
        
        // Render children
        for (int i = 0; i < children.size(); i++) {
            boolean childIsLast = (i == children.size() - 1);
            children.get(i).render(sb, depth + 1, childIsLast);
        }
    }
    
    /**
     * Gets the indent string for a given depth.
     * Uses box-drawing characters for tree structure.
     */
    private String getIndent(int depth) {
        if (depth <= 0) return "";
        
        StringBuilder indent = new StringBuilder();
        ScopeNode node = this.parent;
        
        // Build from bottom up, checking if each ancestor has more siblings
        boolean[] hasSiblings = new boolean[depth];
        for (int i = depth - 1; i >= 0 && node != null; i--) {
            if (node.parent != null) {
                int idx = node.parent.children.indexOf(node);
                hasSiblings[i] = idx < node.parent.children.size() - 1;
            }
            node = node.parent;
        }
        
        for (int i = 0; i < depth; i++) {
            indent.append(hasSiblings[i] ? "│  " : "   ");
        }
        
        return indent.toString();
    }
    
    /**
     * Renders a compact single-line summary (for collapsed output).
     * 
     * @return Single line summary like "render-frame [3 layers, 12 primitives]"
     */
    public String renderCompact() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        
        if (!kvPairs.isEmpty()) {
            sb.append(" {").append(String.join(", ", kvPairs)).append("}");
        }
        
        if (!children.isEmpty()) {
            sb.append(" [").append(children.size()).append(" children]");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return renderCompact();
    }
}


