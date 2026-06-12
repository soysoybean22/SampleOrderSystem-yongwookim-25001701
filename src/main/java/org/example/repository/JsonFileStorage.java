package org.example.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class JsonFileStorage {

    static String DATA_DIR = "data"; // package-private: 같은 패키지 테스트에서 직접 접근

    /** 다른 패키지 테스트에서 데이터 디렉터리를 변경할 때 사용 */
    public static void setDataDir(String dir) {
        DATA_DIR = dir;
    }

    private JsonFileStorage() {}

    public static String read(String filename) {
        Path path = Path.of(DATA_DIR, filename);
        if (!Files.exists(path)) return "[]";
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패: " + filename, e);
        }
    }

    public static void write(String filename, String content) {
        ensureDataDir();
        try {
            Files.writeString(Path.of(DATA_DIR, filename), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("파일 쓰기 실패: " + filename, e);
        }
    }

    public static List<Map<String, String>> parseArray(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        json = json.trim();
        if (json.equals("[]") || json.isEmpty()) return result;
        if (json.startsWith("[")) json = json.substring(1);
        if (json.endsWith("]")) json = json.substring(0, json.length() - 1);

        int depth = 0, start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    result.add(parseObject(json.substring(start, i + 1)));
                    start = -1;
                }
            }
        }
        return result;
    }

    public static String toJsonArray(List<Map<String, String>> items) {
        if (items.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < items.size(); i++) {
            sb.append("  ").append(toJsonObject(items.get(i)));
            if (i < items.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private static Map<String, String> parseObject(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        int i = 0;
        while (i < json.length()) {
            while (i < json.length() && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ',')) i++;
            if (i >= json.length() || json.charAt(i) != '"') break;

            i++; // opening "
            int keyStart = i;
            while (i < json.length() && json.charAt(i) != '"') i++;
            String key = json.substring(keyStart, i);
            i++; // closing "

            while (i < json.length() && json.charAt(i) != ':') i++;
            i++; // skip :
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;

            String value;
            if (i < json.length() && json.charAt(i) == '"') {
                i++; // opening "
                StringBuilder sb = new StringBuilder();
                while (i < json.length() && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\') {
                        i++;
                        if (i < json.length()) sb.append(json.charAt(i));
                    } else {
                        sb.append(json.charAt(i));
                    }
                    i++;
                }
                value = sb.toString();
                i++; // closing "
            } else {
                int valStart = i;
                while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') i++;
                value = json.substring(valStart, i).trim();
            }
            result.put(key, value);
        }
        return result;
    }

    static String toJsonObject(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            sb.append("\"").append(entry.getKey()).append("\": ");
            String val = entry.getValue();
            if (isInteger(val)) {
                sb.append(val);
            } else if (isDecimal(val)) {
                sb.append(val);
            } else {
                sb.append("\"").append(val.replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static boolean isInteger(String val) {
        try { Long.parseLong(val); return true; } catch (NumberFormatException e) { return false; }
    }

    private static boolean isDecimal(String val) {
        try { Double.parseDouble(val); return true; } catch (NumberFormatException e) { return false; }
    }

    private static void ensureDataDir() {
        try {
            Files.createDirectories(Path.of(DATA_DIR));
        } catch (IOException e) {
            throw new RuntimeException("data 디렉터리 생성 실패", e);
        }
    }
}
