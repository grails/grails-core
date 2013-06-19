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
package org.codehaus.groovy.grails.web.binding.bindingsource.hal.json

import groovy.transform.CompileStatic;

import org.grails.databinding.DataBindingSource

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken

@CompileStatic
class HalJsonDataBindingSource implements DataBindingSource {

    protected Map data

    HalJsonDataBindingSource(String json) {
        data = jsonToMap(new JsonReader(new StringReader(json)))
    }

    protected Map jsonToMap(JsonReader reader) {
        def map = [:]
        while(reader.hasNext()) {
            def nextToken = reader.peek()
            switch(nextToken) {
                case JsonToken.NAME:
                    def name = reader.nextName()
                    def tokenAfterName = reader.peek()
                    if(!'_embedded'.equals(name)) {
                        switch(tokenAfterName) {
                            case JsonToken.BEGIN_OBJECT:
                                map[name] = jsonToMap reader
                                reader.endObject()
                                break
                            case JsonToken.STRING:
                            case JsonToken.NUMBER:
                                map[name] = reader.nextString()
                                break
                            case JsonToken.BOOLEAN:
                                map[name] = reader.nextBoolean()
                                break
                            case JsonToken.NULL:
                                map[name] = null
                                reader.nextNull()
                                break
                        }
                    }
                    break
                case JsonToken.BEGIN_OBJECT:
                    reader.beginObject()
                    break
                default:
                    reader.skipValue()
            }
        }
        map
    }

    @Override
    Set getPropertyNames() {
        data.keySet()
    }

    @Override
    def getPropertyValue(String propertyName) {
        data[propertyName]
    }

    @Override
    def getAt(String propertyName) {
        getPropertyValue propertyName
    }

    @Override
    boolean containsProperty(String propertyName) {
        data.containsKey propertyName
    }

    @Override
    boolean hasIdentifier() {
        data.containsKey('id')
    }

    @Override 
    def getIdentifierValue() {
        data['id']
    }
    
    @Override
    int size() {
        data.size()
    }
}
