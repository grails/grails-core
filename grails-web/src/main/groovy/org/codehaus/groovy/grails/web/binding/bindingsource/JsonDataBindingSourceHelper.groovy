/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.binding.bindingsource

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonReader
import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.web.mime.MimeType
import org.grails.databinding.DataBindingSource
import org.grails.databinding.SimpleMapDataBindingSource
import org.grails.databinding.bindingsource.AbstractRequestBodyDataBindingSourceHelper
import org.springframework.beans.factory.annotation.Autowired

import java.util.regex.Pattern

/**
 * Creates DataBindingSource objects from JSON in the request body
 * 
 * @since 2.3
 * @author Jeff Brown
 * @author Graeme Rocher
 *
 * @see DataBindingSource
 * @see DataBindingSourceHelper
 */
@CompileStatic
class JsonDataBindingSourceHelper extends AbstractRequestBodyDataBindingSourceHelper {

    private static final Pattern INDEX_PATTERN = ~/^(\S+)\[(\d+)\]$/

    @Autowired(required = false)
    Gson gson = new Gson()

    @Override
    public MimeType[] getMimeTypes() {
        [MimeType.JSON, MimeType.TEXT_JSON] as MimeType[]
    }
    
    @Override
    protected DataBindingSource createBindingSource(InputStream inputStream) {
        def jsonReader = new JsonReader(new InputStreamReader(inputStream))
        jsonReader.setLenient true
        def parser = new JsonParser()
        final jsonElement = parser.parse(jsonReader)

        Map result = createJsonObjectMap(jsonElement)

        return new SimpleMapDataBindingSource(result)
    }

    /**
     * Returns a map for the given JsonElement. Subclasses can override to customize the format of the map
     *
     * @param jsonElement The JSON element
     * @return The map
     */
    protected Map createJsonObjectMap(JsonElement jsonElement) {
        jsonElement instanceof JsonObject ? new JsonObjectMap(jsonElement, gson) : [:]
    }


    Object getValueForJsonElement(JsonElement value, Gson gson) {
        if (value == null || value.isJsonNull()) {
            return null
        } else if (value.isJsonPrimitive()) {
            JsonPrimitive prim = (JsonPrimitive) value
            if (prim.isNumber()) {
                return value.asNumber
            } else if (prim.isBoolean()) {
                return value.asBoolean
            } else {
                return value.asString
            }
        } else if (value.isJsonObject()) {
            return new SimpleMapDataBindingSource(createJsonObjectMap((JsonObject) value))
        } else if(value.isJsonArray()) {
            return new JsonArrayList((JsonArray)value, gson)
        }

    }

    @CompileStatic
    class JsonObjectMap implements Map {

        JsonObject jsonObject
        Gson gson

        JsonObjectMap(JsonObject jsonObject, Gson gson) {
            this.jsonObject = jsonObject
            this.gson = gson
        }

        @Override
        int size() {
            jsonObject.entrySet().size()
        }

        @Override
        boolean isEmpty() {
            jsonObject.entrySet().isEmpty()
        }

        @Override
        boolean containsKey(Object o) {
            jsonObject.has(o.toString())
        }

        @Override
        boolean containsValue(Object o) {
            get(o) != null
        }

        @Override
        Object get(Object o) {
            final key = o.toString()
            final value = jsonObject.get(key)
            if(value != null) {
                return getValueForJsonElement(value, gson)
            }
            else {
                final matcher = INDEX_PATTERN.matcher(key)
                if(matcher.find()) {
                    String newKey = matcher.group(1)
                    final listValue = jsonObject.get(newKey)
                    if(listValue.isJsonArray()) {
                        JsonArray array = (JsonArray)listValue
                        int index = matcher.group(2).toInteger()
                        getValueForJsonElement(array.get(index), gson)
                    }
                }
            }
        }


        @Override
        Object put(Object k, Object v) {
            jsonObject.add(k.toString(), gson.toJsonTree(v))
        }

        @Override
        Object remove(Object o) {
            jsonObject.remove(o.toString())
        }

        @Override
        void putAll(Map map) {
            for(entry in map.entrySet()) {
                put(entry.key, entry.value)
            }
        }

        @Override
        void clear() {
            for(entry in entrySet())  {
                remove(entry.key)
            }
        }

        @Override
        Set keySet() {
            jsonObject.entrySet().collect{ Map.Entry entry -> entry.key }.toSet()
        }

        @Override
        Collection values() {
            jsonObject.entrySet().collect{ Map.Entry entry -> entry.value}
        }

        @Override
        Set<Map.Entry> entrySet() {
            jsonObject.entrySet()
        }
    }

    @CompileStatic
    class JsonArrayList extends AbstractList {

        JsonArray jsonArray
        Gson gson

        JsonArrayList(JsonArray jsonArray, Gson gson) {
            this.jsonArray = jsonArray
            this.gson = gson
        }

        @Override
        int size() {
            jsonArray.size()
        }

        @Override
        Object get(int i) {
            final jsonElement = jsonArray.get(i)
            return getValueForJsonElement(jsonElement, gson)
        }
    }
}

