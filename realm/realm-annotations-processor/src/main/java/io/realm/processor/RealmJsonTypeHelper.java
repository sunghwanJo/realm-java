/*
 * Copyright 2014 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.processor;

import com.squareup.javawriter.JavaWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for converting between Json types and data types in Java that are supported by Realm.
 */
public class RealmJsonTypeHelper {
    private static final Map<String, JsonToRealmFieldTypeConverter> JAVA_TO_JSON_TYPES;
    static {
        JAVA_TO_JSON_TYPES = new HashMap<String, JsonToRealmFieldTypeConverter>();
        JAVA_TO_JSON_TYPES.put("byte", new SimpleTypeConverter("byte", "Int"));
        JAVA_TO_JSON_TYPES.put("short", new SimpleTypeConverter("short", "Int"));
        JAVA_TO_JSON_TYPES.put("int", new SimpleTypeConverter("int", "Int"));
        JAVA_TO_JSON_TYPES.put("long", new SimpleTypeConverter("long", "Long"));
        JAVA_TO_JSON_TYPES.put("float", new SimpleTypeConverter("float", "Double"));
        JAVA_TO_JSON_TYPES.put("double", new SimpleTypeConverter("double", "Double"));
        JAVA_TO_JSON_TYPES.put("boolean", new SimpleTypeConverter("boolean", "Boolean"));
        JAVA_TO_JSON_TYPES.put("java.lang.Byte", new SimpleTypeConverter("byte", "Int"));
        JAVA_TO_JSON_TYPES.put("java.lang.Short", new SimpleTypeConverter("short", "Int"));
        JAVA_TO_JSON_TYPES.put("java.lang.Integer", new SimpleTypeConverter("int", "Int"));
        JAVA_TO_JSON_TYPES.put("java.lang.Long", new SimpleTypeConverter("long", "Long"));
        JAVA_TO_JSON_TYPES.put("java.lang.Float", new SimpleTypeConverter("float", "Double"));
        JAVA_TO_JSON_TYPES.put("java.lang.Double", new SimpleTypeConverter("double", "Double"));
        JAVA_TO_JSON_TYPES.put("java.lang.Boolean", new SimpleTypeConverter("boolean", "Boolean"));
        JAVA_TO_JSON_TYPES.put("java.lang.String", new SimpleTypeConverter("String", "String"));
        JAVA_TO_JSON_TYPES.put("java.util.Date", new JsonToRealmFieldTypeConverter() {
            @Override
            public void emitTypeConversion(String setter, String fieldName, String fieldType, JavaWriter writer)
                    throws IOException {
                writer
                    .beginControlFlow("if (json.has(\"%s\"))", fieldName)
                        .beginControlFlow("if (json.isNull(\"%s\"))", fieldName)
                            .emitStatement("obj.%s(null)", setter)
                        .nextControlFlow("else")
                            .emitStatement("Object timestamp = json.get(\"%s\")", fieldName)
                            .beginControlFlow("if (timestamp instanceof String)")
                               .emitStatement("obj.%s(JsonUtils.stringToDate((String) timestamp))", setter)
                            .nextControlFlow("else")
                                .emitStatement("obj.%s(new Date(json.getLong(\"%s\")))", setter, fieldName)
                            .endControlFlow()
                        .endControlFlow()
                    .endControlFlow();
            }

            @Override
            public void emitStreamTypeConversion(String setter, String fieldName, String fieldType, JavaWriter writer)
                    throws IOException {
                writer
                    .beginControlFlow("if (reader.peek() == JsonToken.NULL)")
                        .emitStatement("reader.skipValue()")
                        .emitStatement("obj.%s(null)", setter)
                    .nextControlFlow("else if (reader.peek() == JsonToken.NUMBER)")
                        .emitStatement("long timestamp = reader.nextLong()", fieldName)
                        .beginControlFlow("if (timestamp > -1)")
                            .emitStatement("obj.%s(new Date(timestamp))", setter)
                        .endControlFlow()
                    .nextControlFlow("else")
                        .emitStatement("obj.%s(JsonUtils.stringToDate(reader.nextString()))", setter)
                    .endControlFlow();
            }
        });
        JAVA_TO_JSON_TYPES.put("byte[]", new JsonToRealmFieldTypeConverter() {
            @Override
            public void emitTypeConversion(String setter, String fieldName, String fieldType, JavaWriter writer)
                    throws IOException {
                writer
                    .beginControlFlow("if (json.has(\"%s\"))", fieldName)
                        .beginControlFlow("if (json.isNull(\"%s\"))", fieldName)
                            .emitStatement("obj.%s(null)", setter)
                        .nextControlFlow("else")
                            .emitStatement("obj.%s(JsonUtils.stringToBytes(json.getString(\"%s\")))", setter, fieldName)
                        .endControlFlow()
                    .endControlFlow();
            }

            @Override
            public void emitStreamTypeConversion(String setter, String fieldName, String fieldType, JavaWriter writer)
                    throws IOException {
                writer
                    .beginControlFlow("if (reader.peek() == JsonToken.NULL)")
                        .emitStatement("reader.skipValue()")
                        .emitStatement("obj.%s(null)", setter)
                    .nextControlFlow("else")
                        .emitStatement("obj.%s(JsonUtils.stringToBytes(reader.nextString()))", setter)
                    .endControlFlow();
            }
        });
    }

    public static void emitFillJavaTypeWithJsonValue(String setter, String fieldName, String qualifiedFieldType,
                                                     JavaWriter writer) throws IOException {
        JsonToRealmFieldTypeConverter typeEmitter = JAVA_TO_JSON_TYPES.get(qualifiedFieldType);
        if (typeEmitter != null) {
            typeEmitter.emitTypeConversion(setter, fieldName, qualifiedFieldType, writer);
        }
    }

    public static void emitFillRealmObjectWithJsonValue(String setter, String fieldName, String qualifiedFieldType,
                                                        String proxyClass, JavaWriter writer) throws IOException {
        writer
            .beginControlFlow("if (json.has(\"%s\"))", fieldName)
                .beginControlFlow("if (json.isNull(\"%s\"))", fieldName)
                    .emitStatement("obj.%s(null)", setter)
                .nextControlFlow("else")
                    .emitStatement("%s %sObj = %s.createOrUpdateUsingJsonObject(realm, json.getJSONObject(\"%s\"), update)",
                            qualifiedFieldType, fieldName, proxyClass, fieldName)
                    .emitStatement("obj.%s(%sObj)", setter, fieldName)
                .endControlFlow()
            .endControlFlow();
    }

    public static void emitFillRealmListWithJsonValue(String getter, String setter, String fieldName,
                                                      String fieldTypeCanonicalName, String proxyClass,
                                                      JavaWriter writer) throws IOException {
        writer
            .beginControlFlow("if (json.has(\"%s\"))", fieldName)
                .beginControlFlow("if (json.isNull(\"%s\"))", fieldName)
                    .emitStatement("obj.%s(null)", setter)
                .nextControlFlow("else")
                    .emitStatement("obj.%s().clear()", getter)
                    .emitStatement("JSONArray array = json.getJSONArray(\"%s\")", fieldName)
                    .beginControlFlow("for (int i = 0; i < array.length(); i++)")
                        .emitStatement("%s item = %s.createOrUpdateUsingJsonObject(realm, array.getJSONObject(i), update)",
                                fieldTypeCanonicalName, proxyClass, fieldTypeCanonicalName)
                        .emitStatement("obj.%s().add(item)", getter)
                    .endControlFlow()
                .endControlFlow()
            .endControlFlow();
    }


    public static void emitFillJavaTypeFromStream(String setter, String fieldName, String fieldType, JavaWriter writer)
            throws IOException {
        if (JAVA_TO_JSON_TYPES.containsKey(fieldType)) {
            JAVA_TO_JSON_TYPES.get(fieldType).emitStreamTypeConversion(setter, fieldName, fieldType, writer);
        }
    }

    public static void emitFillRealmObjectFromStream(String setter, String fieldName, String fieldTypeCanonicalName,
                                                     String proxyClass, JavaWriter writer) throws IOException {
        writer
            .beginControlFlow("if (reader.peek() == JsonToken.NULL)")
                .emitStatement("reader.skipValue()")
                .emitStatement("obj.%s(null)", setter)
            .nextControlFlow("else")
                .emitStatement("%s %sObj = %s.createUsingJsonStream(realm, reader)", fieldTypeCanonicalName, fieldName,
                        proxyClass)
                .emitStatement("obj.%s(%sObj)", setter, fieldName)
            .endControlFlow();
    }

    public static void emitFillRealmListFromStream(String getter, String setter, String fieldTypeCanonicalName,
                                                   String proxyClass, JavaWriter writer) throws IOException {
        writer
            .beginControlFlow("if (reader.peek() == JsonToken.NULL)")
                .emitStatement("reader.skipValue()")
                .emitStatement("obj.%s(null)", setter)
            .nextControlFlow("else")
                .emitStatement("reader.beginArray()")
                .beginControlFlow("while (reader.hasNext())")
                    .emitStatement("%s item = %s.createUsingJsonStream(realm, reader)", fieldTypeCanonicalName, proxyClass)
                    .emitStatement("obj.%s().add(item)", getter)
                .endControlFlow()
                .emitStatement("reader.endArray()")
            .endControlFlow();
    }

    private static class SimpleTypeConverter implements JsonToRealmFieldTypeConverter {

        private final String castType;
        private final String jsonType;

        /**
         * Creates a conversion between simple types which can be expressed as
         * RealmObject.setFieldName((<castType>) json.get<jsonType>) or
         * RealmObject.setFieldName((<castType>) reader.next<jsonType>
         *
         * @param castType  Java type to cast to.
         * @param jsonType  JsonType to get data from.
         */
        private SimpleTypeConverter(String castType, String jsonType) {
            this.castType = castType;
            this.jsonType = jsonType;
        }

        @Override
        public void emitTypeConversion(String setter, String fieldName, String fieldType, JavaWriter writer)
                throws IOException {
            String statementSetNullOrThrow;
            if (Utils.isPrimitiveType(fieldType)) {
                // Only throw exception for primitive types. For boxed types and String, exception will be thrown in
                // the setter.
                statementSetNullOrThrow = String.format(Constants.STATEMENT_EXCEPTION_ILLEGAL_NULL_VALUE, fieldName);
            } else {
                statementSetNullOrThrow = String.format("obj.%s(null)", setter);
            }
            writer
                .beginControlFlow("if (json.has(\"%s\"))", fieldName)
                    .beginControlFlow("if (json.isNull(\"%s\"))", fieldName)
                        .emitStatement(statementSetNullOrThrow)
                    .nextControlFlow("else")
                        .emitStatement("obj.%s((%s) json.get%s(\"%s\"))", setter, castType, jsonType, fieldName)
                    .endControlFlow()
                .endControlFlow();
        }

        @Override
        public void emitStreamTypeConversion(String setter, String fieldName, String fieldType, JavaWriter writer)
                throws IOException {
            String statementSetNullOrThrow;
            if (Utils.isPrimitiveType(fieldType)) {
                // Only throw exception for primitive types. For boxed types and String, exception will be thrown in
                // the setter.
                statementSetNullOrThrow = String.format(Constants.STATEMENT_EXCEPTION_ILLEGAL_NULL_VALUE, fieldName);
            } else {
                statementSetNullOrThrow = String.format("obj.%s(null)", setter);
            }
            writer
                .beginControlFlow("if (reader.peek() == JsonToken.NULL)")
                    .emitStatement("reader.skipValue()")
                    .emitStatement(statementSetNullOrThrow)
                .nextControlFlow("else")
                    .emitStatement("obj.%s((%s) reader.next%s())", setter, castType, jsonType)
                .endControlFlow();
        }
    }

    private interface JsonToRealmFieldTypeConverter {
        void emitTypeConversion(String setter, String fieldName, String fieldType, JavaWriter writer)
                throws IOException;
        void emitStreamTypeConversion(String setter, String fieldName, String fieldType, JavaWriter writer)
                throws IOException;
    }
}
