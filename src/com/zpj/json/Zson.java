package com.zpj.json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 一个在java对象和JSON之间实现序列化和反序列化的轻量级java库
 *
 * @author Z-P-J
 */
public final class Zson {

    private final List<Adapter> adapters;

    private final int excludeModifiers = Modifier.TRANSIENT | Modifier.STATIC;

    public Zson() {
        List<Adapter> list = new ArrayList<>();
        list.add(new StringAdapter());
        list.add(new NumberAdapter());
        list.add(new BooleanAdapter());
        list.add(new ArrayAdapter());
        list.add(new ObjectAdapter());
        adapters = Collections.unmodifiableList(list);
    }

    /**
     * 序列化
     *
     * @param obj
     * @return
     * @throws Exception
     */
    public String serialize(Object obj) throws Exception {
        TypeToken<?> typeToken = TypeToken.get(obj.getClass());
        Adapter adapter = getAdapter(typeToken);
        StringBuilder builder = new StringBuilder();
        adapter.write(builder, null, obj, typeToken);
        if (builder.lastIndexOf(",") == builder.length() - 1) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    /**
     * 反序列化
     *
     * @param json
     * @param clazz
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> T deserialize(String json, Class<T> clazz) throws Exception {
        TypeToken<?> typeToken = TypeToken.get(clazz);
        Adapter adapter = getAdapter(typeToken);
        JsonReader reader = new JsonReader(json);
        if (reader.peek() != '{') {
            throw new RuntimeException("The format of json is incorrect!");
        }
        return clazz.cast(adapter.read(reader, null, clazz.newInstance(), typeToken));
    }

    private Adapter getAdapter(TypeToken<?> typeToken) {
        Adapter adapter = null;
        for (Adapter a : adapters) {
            if (a.is(typeToken)) {
                adapter = a;
                break;
            }
        }
        return adapter;
    }

    private interface Adapter {
        /**
         * 判断该适配器是否可以处理该类型
         *
         * @param token
         * @return
         */
        boolean is(TypeToken<?> token);

        /**
         * 从json解析出相应的对象
         *
         * @param reader JsonReader
         * @param value  解析的字符串
         * @param obj    当前对象
         * @param token  java对象的TypeToken类型
         * @return
         * @throws Exception
         */
        Object read(JsonReader reader, String value, Object obj, TypeToken<?> token) throws Exception;

        /**
         * 将java对象写入json
         *
         * @param builder
         * @param name
         * @param obj
         * @param token
         * @throws Exception
         */
        void write(StringBuilder builder, String name, Object obj, TypeToken<?> token) throws Exception;

    }

    private static class JsonReader {

        private final String str;
        private final int length;
        private int index;

        public JsonReader(String str) {
            this.str = str;
            this.length = str.length();
            this.index = 0;
        }

        public boolean hasNext() {
            return index >= length;
        }

        public int peek() {
            while (index < length) {
                int c = str.charAt(index++);
                switch (c) {
                    case ':':
                    case ',':
                    case ' ':
                    case '\n':
                    case '\t':
                    case '\r':
                        continue;
                    default:
                        index--;
                        return c;
                }
            }
            return -1;
        }

        public int next() {
            return str.charAt(index + 1);
        }

        public String readNext() {
            char c = (char) peek();
            if (c == '{' || c == '[') {
                return null;
            }

            boolean isStr = (c == '"');
            if (isStr) {
                index++;
            }

            int start = index;
            int end = index;
            while (index < length) {
                c = str.charAt(index++);
                if (isStr) {
                    if (c == '"') {
                        if (str.charAt(index - 2) == '\\') {
                            continue;
                        }
                        end = index - 1;
                        break;
                    }
                } else if (c == ':' || c == ',') {
                    end = index - 1;
                    break;
                } else if (c == '}' || c == ']') {
                    end = --index;
                    break;
                }
            }
            String s = str.substring(start, end);
            return s;
        }

        public void skipBy(int step) {
            index += step;
        }

        public void skipToNext() {
            Stack<Character> stack = new Stack<>();
            char c;
            while (index < length) {
                c = str.charAt(index++);
                if (c == '{' || c == '[' || c == ':') {
                    stack.push(c);
                } else if (c == '"') {
                    if (stack.peek() == '"') {
                        stack.pop();
                    } else {
                        stack.push('"');
                    }
                } else if (c == '}' && stack.peek() == '{') {
                    stack.pop();
                } else if (c == ']' && stack.peek() == '[') {
                    stack.pop();
                } else if (c == ',' && stack.peek() == ':') {
                    stack.pop();
                }
                if (stack.isEmpty()) {
                    break;
                }
            }
        }

    }

    private class ObjectAdapter implements Adapter {

        private class FieldWrapper {

            private final Field field;
            private final TypeToken<?> token;

            public FieldWrapper(Field field, TypeToken<?> token) {
                this.field = field;
                this.token = token;
            }

            public void set(Object obj, Object value) throws IllegalAccessException {
                field.set(obj, value);
            }

            public Object get(Object obj) throws IllegalAccessException {
                return field.get(obj);
            }

            public TypeToken<?> getTypeToken() {
                return TypeToken.get(ReflectUtils.resolve(token.getType(), token.getRawType(), field.getGenericType()));
            }

        }

        @Override
        public boolean is(TypeToken<?> token) {
            return true;
        }

        @Override
        public Object read(JsonReader reader, String value, Object obj, TypeToken<?> token) throws Exception {
            Class<?> raw = token.getRawType();
            if (obj == null) {
                if (raw == Map.class) {
                    obj = HashMap.class.newInstance();
                } else {
                    obj = raw.newInstance();
                }
            }
            if (value != null) {
                if ("null".equals(value)) {
                    return null;
                } else {
                    throw new RuntimeException("The format of json is incorrect! value=" + value + " index=" + reader.index);
                }
            } else if (reader.peek() == '{' && reader.next() == '}') {
                reader.skipBy(2);
                return obj;
            } else if (reader.peek() == '{') {
                reader.skipBy(1);
            }
            if (Map.class.isAssignableFrom(raw)) {
                Type[] keyAndValueTypes = ReflectUtils.getMapKeyAndValueTypes(token.getType(), raw);
                TypeToken<?> keyTypeToken = TypeToken.get(keyAndValueTypes[0]);
                TypeToken<?> valueTypeToken = TypeToken.get(keyAndValueTypes[1]);
                Adapter keyAdapter = getAdapter(keyTypeToken);
                Adapter valueAdapter = getAdapter(valueTypeToken);
                while (!reader.hasNext()) {
                    if (reader.peek() == '}') {
                        reader.skipBy(1);
                        break;
                    }
                    String key = reader.readNext();
                    if (key == null) {
                        break;
                    }
                    String v = reader.readNext();
                    Map<Object, Object> map = (Map<Object, Object>) obj;
                    map.put(keyAdapter.read(reader, key, null, keyTypeToken), valueAdapter.read(reader, v, null, valueTypeToken));
                }
            } else {
                Map<String, FieldWrapper> fieldMap = new HashMap<>();
                while (raw != Object.class) {
                    for (Field field : raw.getDeclaredFields()) {
                        if (excludeField(field)) {
                            continue;
                        }
                        if (!field.isAccessible()) {
                            field.setAccessible(true);
                        }
                        fieldMap.put(ReflectUtils.getSerializeName(field), new FieldWrapper(field, token));
                    }
                    token = TypeToken.get(raw.getGenericSuperclass());
                    raw = token.getRawType();
                }
                while (!reader.hasNext()) {
                    if (reader.peek() == '}') {
                        reader.skipBy(1);
                        break;
                    }
                    String name = reader.readNext();
                    if (name == null) {
                        break;
                    }
                    FieldWrapper field = fieldMap.get(name);
                    if (field == null) {
                        reader.skipToNext();
                        continue;
                    }
                    TypeToken<?> tt = field.getTypeToken();
                    Adapter adapter = getAdapter(tt);
                    field.set(obj, adapter.read(reader, reader.readNext(), field.get(obj), tt));
                }
            }
            return obj;
        }

        @Override
        public void write(StringBuilder builder, String name, Object obj, TypeToken<?> token) throws Exception {
            if (name != null) {
                builder.append('"').append(name).append('"').append(":");
                if (obj == null) {
                    builder.append("null,");
                    return;
                }
            }
            builder.append("{");

            Class<?> raw = token.getRawType();
            if (Map.class.isAssignableFrom(raw)) {
                Type[] keyAndValueTypes = ReflectUtils.getMapKeyAndValueTypes(token.getType(), raw);
                TypeToken<?> keyTypeToken = TypeToken.get(keyAndValueTypes[0]);
                TypeToken<?> valueTypeToken = TypeToken.get(keyAndValueTypes[1]);
                Adapter keyAdapter = getAdapter(keyTypeToken);
                Adapter valueAdapter = getAdapter(valueTypeToken);
                boolean isStrKey = keyAdapter instanceof StringAdapter;

                Map<?, ?> map = ((Map<?, ?>) obj);
                for (Object key : map.keySet()) {
                    if (!isStrKey) {
                        builder.append('"');
                    }
                    keyAdapter.write(builder, null, key, keyTypeToken);
                    if (builder.lastIndexOf(",") == builder.length() - 1) {
                        builder.deleteCharAt(builder.length() - 1);
                    }
                    if (!isStrKey) {
                        builder.append('"');
                    }
                    builder.append(':');
                    valueAdapter.write(builder, null, map.get(key), valueTypeToken);
                }
            } else {
                while (raw != Object.class) {
                    for (Field field : raw.getDeclaredFields()) {
                        if (excludeField(field)) {
                            continue;
                        }
                        if (!field.isAccessible()) {
                            field.setAccessible(true);
                        }
                        TypeToken<?> tt = TypeToken.get(ReflectUtils.resolve(token.getType(), raw, field.getGenericType()));
                        Adapter adapter = getAdapter(tt);
                        adapter.write(builder, ReflectUtils.getSerializeName(field), field.get(obj), tt);
                    }

                    token = TypeToken.get(raw.getGenericSuperclass());
                    raw = token.getRawType();
                }
            }

            if (builder.lastIndexOf(",") == builder.length() - 1) {
                builder.deleteCharAt(builder.length() - 1);
            }
            builder.append("},");
        }

        private boolean excludeField(Field field) {
            if ((excludeModifiers & field.getModifiers()) != 0) {
                return true;
            }
            if (field.isSynthetic()) {
                return true;
            }
            return isAnonymousOrLocal(field.getType());
        }

        private boolean isAnonymousOrLocal(Class<?> clazz) {
            return !Enum.class.isAssignableFrom(clazz)
                    && (clazz.isAnonymousClass() || clazz.isLocalClass());
        }

    }

    private class ArrayAdapter implements Adapter {

        @Override
        public boolean is(TypeToken<?> token) {
            Class<?> raw = token.getRawType();
            return raw.isArray() || Collection.class.isAssignableFrom(raw);
        }

        @Override
        public Object read(JsonReader reader, String value, Object obj, TypeToken<?> token) throws Exception {
            if (reader.peek() != '[') {
                return null;
            }
            reader.skipBy(1);
            Class<?> raw = token.getRawType();
            if (raw.isArray()) {
                if (reader.peek() == '[' && reader.next() == ']') {
                    return Array.newInstance(raw, 0);
                }
                Type componentType = ReflectUtils.getArrayComponentType(token.getType());
                TypeToken<?> tt = TypeToken.get(componentType);
                Adapter adapter = getAdapter(tt);
                List<Object> items = new ArrayList<>();
                while (!reader.hasNext()) {
                    if (reader.peek() == ']') {
                        reader.skipBy(1);
                        break;
                    }
                    items.add(adapter.read(reader, reader.readNext(), null, tt));
                }
                obj = Array.newInstance(tt.getRawType(), items.size());
                for (int i = 0; i < items.size(); i++) {
                    Array.set(obj, i, items.get(i));
                }
                return obj;
            } else if (Collection.class.isAssignableFrom(raw)) {
                if (obj == null) {
                    if (raw == List.class) {
                        obj = ArrayList.class.newInstance();
                    } else {
                        obj = raw.newInstance();
                    }
                }
                if (reader.peek() == '[' && reader.next() == ']') {
                    return obj;
                }
                Type elementType = ReflectUtils.getCollectionElementType(token.getType(), raw);
                TypeToken<?> tt = TypeToken.get(elementType);
                Adapter adapter = getAdapter(tt);
                Collection collection = (Collection) obj;
                while (!reader.hasNext()) {
                    if (reader.peek() == ']') {
                        reader.skipBy(1);
                        break;
                    }
                    collection.add(adapter.read(reader, reader.readNext(), null, tt));
                }
            }
            return obj;
        }

        @Override
        public void write(StringBuilder builder, String name, Object obj, TypeToken<?> token) throws Exception {
            if (name != null) {
                builder.append('"').append(name).append('"').append(":");
                if (obj == null) {
                    builder.append("null");
                    return;
                }
            }
            builder.append("[");

            Class<?> raw = token.getRawType();
            if (raw.isArray()) {
                Type componentType = ReflectUtils.getArrayComponentType(token.getType());
                TypeToken<?> tt = TypeToken.get(componentType);
                Adapter adapter = getAdapter(tt);
                for (int i = 0; i < Array.getLength(obj); i++) {
                    adapter.write(builder, null, Array.get(obj, i), tt);
                }
            } else if (List.class.isAssignableFrom(raw) || Set.class.isAssignableFrom(raw)) {
                Type elementType = ReflectUtils.getCollectionElementType(token.getType(), raw);
//                System.out.println("type=" + token.getType() + " raw=" + raw + " elementType=" + elementType);
                TypeToken<?> tt = TypeToken.get(elementType);
                Adapter adapter = getAdapter(tt);
                Collection<?> collection = (Collection<?>) obj;
                for (Object item : collection) {
                    adapter.write(builder, null, item, tt);
                }
            }

            if (builder.lastIndexOf(",") == builder.length() - 1) {
                builder.deleteCharAt(builder.length() - 1);
            }
            builder.append("],");
        }
    }

    private static class StringAdapter implements Adapter {

        private static final String[] HTML_SAFE_REPLACEMENT_CHARS;

        static {
            HTML_SAFE_REPLACEMENT_CHARS = new String[128];
            for (int i = 0; i <= 0x1f; i++) {
                HTML_SAFE_REPLACEMENT_CHARS[i] = String.format("\\u%04x", (int) i);
            }
            HTML_SAFE_REPLACEMENT_CHARS['"'] = "\\\"";
            HTML_SAFE_REPLACEMENT_CHARS['\\'] = "\\\\";
            HTML_SAFE_REPLACEMENT_CHARS['\t'] = "\\t";
            HTML_SAFE_REPLACEMENT_CHARS['\b'] = "\\b";
            HTML_SAFE_REPLACEMENT_CHARS['\n'] = "\\n";
            HTML_SAFE_REPLACEMENT_CHARS['\r'] = "\\r";
            HTML_SAFE_REPLACEMENT_CHARS['\f'] = "\\f";
//            HTML_SAFE_REPLACEMENT_CHARS['<'] = "\\u003c";
//            HTML_SAFE_REPLACEMENT_CHARS['>'] = "\\u003e";
//            HTML_SAFE_REPLACEMENT_CHARS['&'] = "\\u0026";
//            HTML_SAFE_REPLACEMENT_CHARS['='] = "\\u003d";
//            HTML_SAFE_REPLACEMENT_CHARS['\''] = "\\u0027";
        }

        @Override
        public boolean is(TypeToken<?> token) {
            return token.getRawType() == String.class || token.getRawType().isEnum();
        }

        @Override
        public Object read(JsonReader reader, String value, Object obj, TypeToken<?> token) throws Exception {
            if (token.getRawType().isEnum()) {
                if (value != null) {
                    for (Field field : token.getRawType().getDeclaredFields()) {
                        if (field.isEnumConstant()) {
                            if (!field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            if (value.equals(ReflectUtils.getSerializeName(field))) {
                                return field.get(null);
                            }
                        }
                    }
                }
                return null;
            }
            return value;
        }

        @Override
        public void write(StringBuilder builder, String name, Object obj, TypeToken<?> token) throws Exception {
            if (name != null) {
                builder.append('"').append(name).append('"').append(":");
            }
            if (obj == null) {
                builder.append("null");
            } else {
                if (token.getRawType().isEnum()) {
                    builder.append('"');
                    for (Field field : token.getRawType().getDeclaredFields()) {
                        if (field.isEnumConstant()) {
                            if (!field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            if (field.get(null) == obj) {
                                builder.append(ReflectUtils.getSerializeName(field));
                                break;
                            }
                        }
                    }
                    builder.append('"');
                } else {
                    writeString(builder, obj.toString());
                }
            }
            builder.append(",");
        }

        private void writeString(StringBuilder builder, String value) {
            builder.append('"');
            int last = 0;
            int length = value.length();
            for (int i = 0; i < length; i++) {
                char c = value.charAt(i);
                String replacement;
                if (c < 128) {
                    replacement = HTML_SAFE_REPLACEMENT_CHARS[c];
                    if (replacement == null) {
                        continue;
                    }
                } else if (c == '\u2028') {
                    replacement = "\\u2028";
                } else if (c == '\u2029') {
                    replacement = "\\u2029";
                } else {
                    continue;
                }
                if (last < i) {
                    builder.append(value, last, i);
                }
                builder.append(replacement);
                last = i + 1;
            }
            if (last < length) {
                builder.append(value, last, length);
            }
            builder.append('"');
        }

    }

    private static class NumberAdapter implements Adapter {

        @Override
        public boolean is(TypeToken<?> token) {
            Class<?> raw = token.getRawType();
            return raw == char.class || raw == Character.class
                    || raw == int.class || raw == Integer.class
                    || raw == float.class || raw == Float.class
                    || raw == long.class || raw == Long.class
                    || raw == double.class || raw == Double.class
                    || raw == short.class || raw == Short.class
                    || Number.class.isAssignableFrom(raw);
        }

        @Override
        public Object read(JsonReader reader, String value, Object obj, TypeToken<?> token) throws Exception {
            Class<?> raw = token.getRawType();
            if (raw == char.class || raw == Character.class) {
                return (char) Integer.parseInt(value);
            } else if (raw == int.class || raw == Integer.class) {
                return Integer.valueOf(value);
            } else if (raw == float.class || raw == Float.class) {
                return Float.valueOf(value);
            } else if (raw == long.class || raw == Long.class) {
                return Long.valueOf(value);
            } else if (raw == double.class || raw == Double.class) {
                return Double.valueOf(value);
            } else if (raw == short.class || raw == Short.class) {
                return Short.valueOf(value);
            }
            if (obj == null) {
                obj = token.getRawType().newInstance();
            }
            return obj;
        }

        @Override
        public void write(StringBuilder builder, String name, Object obj, TypeToken<?> token) throws Exception {
            if (name != null) {
                builder.append('"').append(name).append('"').append(":");
            }
            if (obj == null) {
                builder.append("null");
            } else {
                if (token.getRawType() == char.class || token.getRawType() == Character.class) {
                    builder.append((int) ((char) obj));
                } else {
                    builder.append(obj);
                }
            }
            builder.append(",");
        }
    }

    private static class BooleanAdapter implements Adapter {

        @Override
        public boolean is(TypeToken<?> token) {
            return token.getRawType() == boolean.class || token.getRawType() == Boolean.class;
        }

        @Override
        public Object read(JsonReader reader, String value, Object obj, TypeToken<?> token) throws Exception {
            if ("null".equals(value)) {
                return null;
            } else {
                return Boolean.parseBoolean(value);
            }
        }

        @Override
        public void write(StringBuilder builder, String name, Object obj, TypeToken<?> token) throws Exception {
            if (name != null) {
                builder.append('"').append(name).append('"').append(":");
            }
            String value;
            if (obj == null) {
                value = "false";
            } else {
                value = Boolean.toString((boolean) obj);
            }
            builder.append(value).append(",");
        }
    }

}
