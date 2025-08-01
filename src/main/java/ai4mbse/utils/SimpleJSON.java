/*
 * Copyright (c) 2024 AI4MBSE Development Team
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * This file was generated with assistance from artificial intelligence tools.
 */

package ai4mbse.utils;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Simple JSON implementation to replace Gson dependency for open source compilation.
 * 
 * This provides basic JSON functionality without external dependencies.
 * In a real deployment with MagicDraw, Gson libraries will be available.
 */
public class SimpleJSON {
    /**
     * Universal findPackage method that works with any JsonNode type
     */
    public static Object findPackage(Object node, String name) {
        try {
            // Try Jackson JsonNode first (Windows/production environment)
            if (node.getClass().getName().equals("com.fasterxml.jackson.databind.JsonNode")) {
                return findPackageInJacksonNode(node, name);
            }
            // Fallback to local JsonNode (stub environment)
            else if (node instanceof JsonNode) {
                return JsonPackageUtils.findPackage((JsonNode) node, name);
            }
        } catch (Exception e) {
            // Fallback behavior
        }
        return null;
    }
    
    /**
     * Implementation for Jackson JsonNode using reflection
     */
    private static Object findPackageInJacksonNode(Object node, String searchName) {
        try {
            // Use reflection to access Jackson JsonNode methods
            Class<?> nodeClass = node.getClass();
            java.lang.reflect.Method pathMethod = nodeClass.getMethod("path", String.class);
            java.lang.reflect.Method asTextMethod = nodeClass.getMethod("asText");
            java.lang.reflect.Method isArrayMethod = nodeClass.getMethod("isArray");
            java.lang.reflect.Method iteratorMethod = nodeClass.getMethod("iterator");
            
            Object nameNode = pathMethod.invoke(node, "name");
            String pkgName = (String) asTextMethod.invoke(nameNode);
            
            // Package name matching logic
            String[] exactCandidates = { searchName, "Paket " + searchName, "Package " + searchName };
            for (String candidate : exactCandidates) {
                if (candidate.equals(pkgName)) {
                    return node;
                }
            }
            
            // Recursive search in child packages if this is an array
            Boolean isArray = (Boolean) isArrayMethod.invoke(node);
            if (isArray) {
                java.util.Iterator<?> iterator = (java.util.Iterator<?>) iteratorMethod.invoke(node);
                while (iterator.hasNext()) {
                    Object child = iterator.next();
                    Object result = findPackageInJacksonNode(child, searchName);
                    if (result != null) {
                        return result;
                    }
                }
            }
            
        } catch (Exception e) {
            // Reflection failed, return null
        }
        return null;
    }
    
    /**
     * Universal path method that works with any JsonNode type
     */
    public static Object path(Object node, String fieldName) {
        try {
            if (node.getClass().getName().equals("com.fasterxml.jackson.databind.JsonNode")) {
                java.lang.reflect.Method pathMethod = node.getClass().getMethod("path", String.class);
                return pathMethod.invoke(node, fieldName);
            } else if (node instanceof JsonNode) {
                return ((JsonNode) node).path(fieldName);
            }
        } catch (Exception e) {
            // Fallback
        }
        return null;
    }
    
    /**
     * Universal isArray method that works with any JsonNode type
     */
    public static boolean isArray(Object node) {
        try {
            if (node.getClass().getName().equals("com.fasterxml.jackson.databind.JsonNode")) {
                java.lang.reflect.Method isArrayMethod = node.getClass().getMethod("isArray");
                return (Boolean) isArrayMethod.invoke(node);
            } else if (node instanceof JsonNode) {
                return ((JsonNode) node).isArray();
            }
        } catch (Exception e) {
            // Fallback
        }
        return false;
    }
    
    /**
     * Universal iterator method that works with any JsonNode type
     */
    public static java.util.Iterator<?> iterator(Object node) {
        try {
            if (node.getClass().getName().equals("com.fasterxml.jackson.databind.JsonNode")) {
                java.lang.reflect.Method iteratorMethod = node.getClass().getMethod("iterator");
                return (java.util.Iterator<?>) iteratorMethod.invoke(node);
            } else if (node instanceof JsonNode) {
                return ((JsonNode) node).iterator();
            }
        } catch (Exception e) {
            // Fallback
        }
        return java.util.Collections.emptyIterator();
    }
    
    /**
     * Universal asText method that works with any JsonNode type
     */
    public static String asText(Object node) {
        try {
            if (node.getClass().getName().equals("com.fasterxml.jackson.databind.JsonNode")) {
                java.lang.reflect.Method asTextMethod = node.getClass().getMethod("asText");
                return (String) asTextMethod.invoke(node);
            } else if (node instanceof JsonNode) {
                return ((JsonNode) node).asText();
            }
        } catch (Exception e) {
            // Fallback
        }
        return "";
    }
}

class Gson {
    public String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + obj + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Map) return mapToJson((Map<?, ?>) obj);
        if (obj instanceof Collection) return collectionToJson((Collection<?>) obj);
        return "{}"; // Fallback for objects
    }
    
    public <T> T fromJson(String json, Class<T> classOfT) {
        // Simplified implementation - in real deployment, Gson will handle this
        return null;
    }
    
    public <T> T fromJson(String json, Type typeOfT) {
        // Simplified implementation - in real deployment, Gson will handle this
        return null;
    }
    
    private String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":")
              .append(toJson(entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
    
    private String collectionToJson(Collection<?> collection) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : collection) {
            if (!first) sb.append(",");
            sb.append(toJson(item));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
}

class GsonBuilder {
    public GsonBuilder setPrettyPrinting() {
        return this;
    }
    
    public Gson create() {
        return new Gson();
    }
}

class JsonSyntaxException extends RuntimeException {
    public JsonSyntaxException(String message) {
        super(message);
    }
    
    public JsonSyntaxException(String message, Throwable cause) {
        super(message, cause);
    }
}

class TypeToken<T> {
    public Type getType() {
        return Object.class; // Simplified implementation
    }
}

// Simple JSON parsing classes
class JsonParser {
    public static JsonElement parseString(String json) {
        return new JsonObject(); // Simplified implementation
    }
}

abstract class JsonElement {
    public abstract JsonObject getAsJsonObject();
    public abstract JsonArray getAsJsonArray();
    public abstract String getAsString();
    public abstract boolean isJsonObject();
    public abstract boolean isJsonArray();
}

class JsonObject extends JsonElement {
    private Map<String, Object> properties = new HashMap<>();
    
    public void addProperty(String key, String value) {
        properties.put(key, value);
    }
    
    public void addProperty(String key, Number value) {
        properties.put(key, value);
    }
    
    public void add(String key, JsonElement element) {
        properties.put(key, element);
    }
    
    public JsonElement get(String key) {
        Object value = properties.get(key);
        if (value instanceof String) {
            return new JsonPrimitive((String) value);
        }
        return new JsonObject(); // Simplified
    }
    
    public JsonArray getAsJsonArray(String key) {
        return new JsonArray(); // Simplified
    }
    
    public JsonObject getAsJsonObject(String key) {
        return new JsonObject(); // Simplified
    }
    
    @Override
    public JsonArray getAsJsonArray() {
        return new JsonArray(); // Simplified
    }
    
    @Override
    public JsonObject getAsJsonObject() {
        return this;
    }
    
    @Override
    public String getAsString() {
        return toString();
    }
    
    @Override
    public boolean isJsonObject() {
        return true;
    }
    
    @Override
    public boolean isJsonArray() {
        return false;
    }
}

class JsonArray extends JsonElement {
    private List<JsonElement> elements = new ArrayList<>();
    
    public void add(JsonElement element) {
        elements.add(element);
    }
    
    public JsonElement get(int index) {
        return elements.get(index);
    }
    
    public int size() {
        return elements.size();
    }
    
    @Override
    public JsonObject getAsJsonObject() {
        return new JsonObject();
    }
    
    @Override
    public JsonArray getAsJsonArray() {
        return this;
    }
    
    @Override
    public String getAsString() {
        return toString();
    }
    
    @Override
    public boolean isJsonObject() {
        return false;
    }
    
    @Override
    public boolean isJsonArray() {
        return true;
    }
}

class JsonPrimitive extends JsonElement {
    private Object value;
    
    public JsonPrimitive(String value) {
        this.value = value;
    }
    
    public JsonPrimitive(Number value) {
        this.value = value;
    }
    
    public JsonPrimitive(Boolean value) {
        this.value = value;
    }
    
    @Override
    public JsonObject getAsJsonObject() {
        return new JsonObject();
    }
    
    @Override
    public JsonArray getAsJsonArray() {
        return new JsonArray();
    }
    
    @Override
    public String getAsString() {
        return value.toString();
    }
    
    @Override
    public boolean isJsonObject() {
        return false;
    }
    
    @Override
    public boolean isJsonArray() {
        return false;
    }
}

// Jackson JSON compatibility classes
class JsonNode implements Iterable<JsonNode> {
    public JsonNode path(String key) {
        return new JsonNode(); // Simplified
    }
    
    public String asText() {
        return ""; // Simplified
    }
    
    public boolean isArray() {
        return false; // Simplified
    }
    
    @Override
    public java.util.Iterator<JsonNode> iterator() {
        return java.util.Collections.emptyIterator();
    }
    
    /**
     * Public static wrapper für JsonPackageUtils.findPackage()
     */
    public static JsonNode findPackage(JsonNode node, String name) {
        return JsonPackageUtils.findPackage(node, name);
    }
}

class ObjectMapper {
    public JsonNode readTree(java.io.File file) throws java.io.IOException {
        return new JsonNode(); // Simplified - would read actual JSON in real implementation
    }
}

/**
 * Utility class for JSON package search functionality.
 */
class JsonPackageUtils {
    /**
     * Sucht ein Package in der JSON-Struktur anhand des Namens.
     * 
     * Unterstützt verschiedene Namensvarianten (z.B. mit "Paket "-Prefix).
     * 
     * @param node Der JSON-Knoten, in dem gesucht werden soll
     * @param name Der gesuchte Package-Name
     * @return Der gefundene JSON-Knoten oder null
     */
    public static JsonNode findPackage(JsonNode node, String name) {
        String pkgName = node.path("name").asText();
        
        // 1. Exakte Übereinstimmung (höchste Priorität)
        String[] exactCandidates = { name, "Paket " + name, "Package " + name };
        if (java.util.Arrays.asList(exactCandidates).contains(pkgName)) {
            return node;
        }
        
        // 2. Normalisierte Suche (Leerzeichen vs Unterstriche)
        String normalizedSearch = name.toLowerCase().replace("_", " ").trim();
        String normalizedPkg = pkgName.toLowerCase().replace("_", " ").trim();
        if (normalizedSearch.equals(normalizedPkg)) {
            return node;
        }
        
        // 3. Teilstring-Suche (enthält)
        if (pkgName.toLowerCase().contains(name.toLowerCase()) || 
            name.toLowerCase().contains(pkgName.toLowerCase())) {
            return node;
        }
        
        // 4. Suche ohne Zahlen-Präfix (03_Subsystem → Subsystem)
        String nameWithoutPrefix = name.replaceFirst("^\\d+_?", "");
        String pkgWithoutPrefix = pkgName.replaceFirst("^\\d+_?", "");
        if (!nameWithoutPrefix.isEmpty() && !pkgWithoutPrefix.isEmpty()) {
            if (nameWithoutPrefix.equalsIgnoreCase(pkgWithoutPrefix)) {
                return node;
            }
        }
        
        // Rekursive Suche in Unterpackages
        for (JsonNode child : node.path("subPackages")) {
            JsonNode found = findPackage(child, name);
            if (found != null) return found;
        }
        return null;
    }
}