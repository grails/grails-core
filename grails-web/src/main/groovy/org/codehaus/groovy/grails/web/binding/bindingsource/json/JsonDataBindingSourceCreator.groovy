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
package org.codehaus.groovy.grails.web.binding.bindingsource.json

import groovy.transform.CompileStatic

import org.grails.databinding.DataBindingSource
import org.grails.databinding.SimpleMapDataBindingSource

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken


@CompileStatic
class JsonDataBindingSourceCreator {

    DataBindingSource createDataBindingSource(String json) {
        def jsonReader = new JsonReader(new StringReader(json))
        jsonReader.setLenient true
        jsonToBindingSource(jsonReader)
    }
    
    protected DataBindingSource jsonToBindingSource(JsonReader reader) {
        def map = [:]
        while(reader.hasNext()) {
            def nextToken = reader.peek()
            switch(nextToken) {
                case JsonToken.NAME:
                    def name = reader.nextName()
                    processToken(reader, map, name)
                    break
                case JsonToken.BEGIN_OBJECT:
                    reader.beginObject()
                    break
                default:
                    reader.skipValue()
            }
        }
        new SimpleMapDataBindingSource(map)
    }

    protected processToken(JsonReader reader, Map map, String name) {
        def tokenAfterName = reader.peek()
        def valueToAdd = getValueForToken(tokenAfterName, reader)
        if(valueToAdd instanceof List) {
            valueToAdd.eachWithIndex { item, idx ->
                map["${name}[${idx}]".toString()] = item
            }
        } else {
            map[name] = valueToAdd
        }
    }

    protected getValueForToken(JsonToken currentToken, JsonReader reader) {
        def valueToAdd
        switch(currentToken) {
            case JsonToken.BEGIN_OBJECT:
                valueToAdd = jsonToBindingSource reader
                reader.endObject()
                break
            case JsonToken.STRING:
            case JsonToken.NUMBER:
                valueToAdd = reader.nextString()
                break
            case JsonToken.BOOLEAN:
                valueToAdd = reader.nextBoolean()
                break
            case JsonToken.NULL:
                valueToAdd = null
                reader.nextNull()
                break
            case JsonToken.BEGIN_ARRAY:
               reader.beginArray()
               def list = []
               while(reader.hasNext()) {
                   def nextToken = reader.peek()
                   list << getValueForToken(nextToken, reader)
               }
               valueToAdd = list
               reader.endArray()
               break
        }
         valueToAdd
    }
}
