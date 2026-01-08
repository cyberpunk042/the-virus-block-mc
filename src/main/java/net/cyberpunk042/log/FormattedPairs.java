package net.cyberpunk042.log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Key-value pair builder for FormattedContext.
 */
public class FormattedPairs {
    
    private final FormattedContext parent;
    private final List<String[]> pairs = new ArrayList<>();
    private int padding = 0;
    
    FormattedPairs(FormattedContext parent) {
        this.parent = parent;
    }
    
    public FormattedPairs add(String key, Object value) {
        pairs.add(new String[] { key, LogFormat.format(value) });
        padding = Math.max(padding, key.length());
        return this;
    }
    
    public FormattedPairs addAll(Map<String, ?> map) {
        for (Map.Entry<String, ?> e : map.entrySet()) add(e.getKey(), e.getValue());
        return this;
    }
    
    public FormattedPairs separator() {
        pairs.add(null);
        return this;
    }
    
    public FormattedContext done() {
        StringBuilder sb = new StringBuilder();
        for (String[] pair : pairs) {
            if (pair == null) {
                sb.append("\n  ").append("-".repeat(padding + 10));
            } else {
                sb.append("\n  ").append(String.format("%-" + padding + "s", pair[0]))
                  .append(": ").append(pair[1]);
            }
        }
        parent.addSection(sb.toString());
        return parent;
    }
}
