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

import groovy.transform.CompileStatic

import java.util.regex.Pattern

import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.mime.MimeType
import org.grails.databinding.CollectionDataBindingSource
import org.grails.databinding.DataBindingSource
import org.grails.databinding.SimpleMapDataBindingSource
import org.grails.databinding.bindingsource.AbstractRequestBodyDataBindingSourceCreator
import org.springframework.beans.factory.annotation.Autowired

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonReader

/**
 * Creates DataBindingSource objects from JSON in the request body
 *
 * @since 2.3
 * @author Jeff Brown
 * @author Graeme Rocher
 *
 * @see DataBindingSource
 * @see org.grails.databinding.bindingsource.DataBindingSourceCreator
 */
@CompileStatic
class JsonDataBindingSourceCreator extends AbstractRequestBodyDataBindingSourceCreator {

    private static final Pattern INDEX_PATTERN = ~/^(\S+)\[(\d+)\]$/

    @Autowired(required = false)
    Gson gson = new Gson()

    @Override
    MimeType[] getMimeTypes() {
        [MimeType.JSON, MimeType.TEXT_JSON] as MimeType[]
    }

    @Override
    DataBindingSource createDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource) {
        if(bindingSource instanceof JsonObject) {
            return new SimpleMapDataBindingSource(createJsonObjectMap((JsonObject)bindingSource))
        }
        else if(bindingSource instanceof JSONObject) {
            return new SimpleMapDataBindingSource((JSONObject)bindingSource)
        }
        else {
            return super.createDataBindingSource(mimeType, bindingTargetType, bindingSource)
        }
    }

    @Override
    protected CollectionDataBindingSource createCollectionBindingSource(Reader reader) {
        def jsonReader = new JsonReader(reader)
        jsonReader.setLenient true
        def parser = new JsonParser()

        // TODO Need to decide what to do if the root element is not a JsonArray
        JsonArray jsonElement = (JsonArray)parser.parse(jsonReader)
        def dataBindingSources = jsonElement.collect { JsonElement element ->
            new SimpleMapDataBindingSource(createJsonObjectMap(element))
        }
        return new CollectionDataBindingSource() {
            List<DataBindingSource> getDataBindingSources() {
                dataBindingSources
            }
        }
    }

    @Override
    protected DataBindingSource createBindingSource(Reader reader) {
        def jsonReader = new JsonReader(reader)
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
        }

        if (value.isJsonPrimitive()) {
            JsonPrimitive prim = (JsonPrimitive) value
            if (prim.isNumber()) {
                return value.asNumber
            }
            if (prim.isBoolean()) {
                return value.asBoolean
            }
            return value.asString
        }

        if (value.isJsonObject()) {
            return createJsonObjectMap((JsonObject) value)
        }

        if (value.isJsonArray()) {
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

        int size() {
            jsonObject.entrySet().size()
        }

        boolean isEmpty() {
            jsonObject.entrySet().isEmpty()
        }

        boolean containsKey(Object o) {
            jsonObject.has(o.toString())
        }

        boolean containsValue(Object o) {
            get(o) != null
        }

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


        Object put(Object k, Object v) {
            jsonObject.add(k.toString(), gson.toJsonTree(v))
        }

        Object remove(Object o) {
            jsonObject.remove(o.toString())
        }

        void putAll(Map map) {
            for(entry in map.entrySet()) {
                put(entry.key, entry.value)
            }
        }

        void clear() {
            for(entry in entrySet())  {
                remove(entry.key)
            }
        }

        Set keySet() {
            jsonObject.entrySet().collect{ Map.Entry entry -> entry.key }.toSet()
        }

        Collection values() {
            jsonObject.entrySet().collect{ Map.Entry entry -> entry.value}
        }

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

        int size() {
            jsonArray.size()
        }

        Object get(int i) {
            final jsonElement = jsonArray.get(i)
            return getValueForJsonElement(jsonElement, gson)
        }
    }
}
