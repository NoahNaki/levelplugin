package me.nakilex.playerspoofer;

final class SimpleJson {
    private SimpleJson() {}

    static String quote(String value) {
        if (value == null) return "null";
        StringBuilder out = new StringBuilder(value.length() + 16).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    static String firstStringField(String json, String field) {
        if (json == null || field == null) return null;
        String needle = '"' + field + '"';
        int from = 0;
        while (true) {
            int key = json.indexOf(needle, from);
            if (key < 0) return null;
            int colon = json.indexOf(':', key + needle.length());
            if (colon < 0) return null;
            int p = colon + 1;
            while (p < json.length() && Character.isWhitespace(json.charAt(p))) p++;
            if (p < json.length() && json.charAt(p) == '"') return readString(json, p + 1);
            from = key + needle.length();
        }
    }

    static String contentAfterMessage(String json) {
        if (json == null) return null;
        int message = json.indexOf("\"message\"");
        if (message >= 0) {
            String nested = firstStringField(json.substring(message), "content");
            if (nested != null) return nested;
        }
        return firstStringField(json, "content");
    }

    private static String readString(String json, int start) {
        StringBuilder out = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') return out.toString();
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (++i >= json.length()) return out.toString();
            char esc = json.charAt(i);
            switch (esc) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (i + 4 < json.length()) {
                        try {
                            out.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                            i += 4;
                        } catch (NumberFormatException ignored) {
                            out.append("\\u");
                        }
                    }
                }
                default -> out.append(esc);
            }
        }
        return out.toString();
    }
}
