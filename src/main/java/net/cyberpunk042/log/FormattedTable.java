package net.cyberpunk042.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Table builder for FormattedContext.
 */
public class FormattedTable {
    
    private final FormattedContext parent;
    private final String title;
    private String[] headers;
    private final List<Object[]> rows = new ArrayList<>();
    private TableStyle style = TableStyle.ASCII;
    private final Map<Integer, Function<Object, String>> colFormatters = new HashMap<>();
    
    FormattedTable(FormattedContext parent, String title) {
        this.parent = parent;
        this.title = title;
    }
    
    public FormattedTable headers(String... h) { headers = h; return this; }
    public FormattedTable row(Object... cells) { rows.add(cells); return this; }
    
    public <T> FormattedTable rows(Iterable<T> items, Function<T, Object[]> mapper) {
        for (T item : items) rows.add(mapper.apply(item));
        return this;
    }
    
    public FormattedTable style(TableStyle s) { style = s; return this; }
    
    public FormattedTable colFormatter(int col, Function<Object, String> f) {
        colFormatters.put(col, f);
        return this;
    }
    
    public FormattedContext done() {
        if (headers == null || headers.length == 0) return parent;
        
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) widths[i] = headers[i].length();
        for (Object[] row : rows) {
            for (int i = 0; i < row.length && i < widths.length; i++) {
                String cell = formatCell(i, row[i]);
                widths[i] = Math.max(widths[i], cell.length());
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(title).append(":");
        
        // Header
        sb.append("\n").append(style.vertical);
        for (int i = 0; i < headers.length; i++) {
            sb.append(" ").append(String.format("%-" + widths[i] + "s", headers[i])).append(" ").append(style.vertical);
        }
        
        // Separator
        sb.append("\n").append(style.topLeft);
        for (int i = 0; i < headers.length; i++) {
            sb.append(style.horizontal.repeat(widths[i] + 2));
            sb.append(i < headers.length - 1 ? style.horizontal : style.topRight);
        }
        
        // Rows
        for (Object[] row : rows) {
            sb.append("\n").append(style.vertical);
            for (int i = 0; i < headers.length; i++) {
                String cell = i < row.length ? formatCell(i, row[i]) : "";
                sb.append(" ").append(String.format("%-" + widths[i] + "s", cell)).append(" ").append(style.vertical);
            }
        }
        
        parent.addSection(sb.toString());
        return parent;
    }
    
    private String formatCell(int col, Object value) {
        Function<Object, String> f = colFormatters.get(col);
        return f != null ? f.apply(value) : LogFormat.format(value);
    }
}
