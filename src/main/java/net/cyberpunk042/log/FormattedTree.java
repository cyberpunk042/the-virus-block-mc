package net.cyberpunk042.log;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree structure builder for FormattedContext.
 */
public class FormattedTree {
    
    private final FormattedContext parent;
    private final String root;
    private final List<Object> nodes = new ArrayList<>();
    private int depth = 0;
    
    FormattedTree(FormattedContext parent, String root) {
        this.parent = parent;
        this.root = root;
    }
    
    public FormattedTree branch(String name) {
        nodes.add(new int[] { depth, 1 }); // 1 = branch
        nodes.add(name);
        depth++;
        return this;
    }
    
    public FormattedTree leaf(String name) {
        nodes.add(new int[] { depth, 0 }); // 0 = leaf
        nodes.add(name);
        return this;
    }
    
    public FormattedTree up() {
        if (depth > 0) depth--;
        return this;
    }
    
    public FormattedContext done() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(root);
        for (int i = 0; i < nodes.size(); i += 2) {
            int[] meta = (int[]) nodes.get(i);
            String name = (String) nodes.get(i + 1);
            sb.append("\n").append("  ".repeat(meta[0] + 1));
            sb.append(meta[1] == 1 ? "├─ " : "└─ ").append(name);
        }
        parent.addSection(sb.toString());
        return parent;
    }
}
