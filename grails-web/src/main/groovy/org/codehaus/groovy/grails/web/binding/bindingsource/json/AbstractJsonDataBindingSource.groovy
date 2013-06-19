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

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken

@CompileStatic
abstract class AbstractJsonDataBindingSource implements DataBindingSource {
    
    protected Map data
    
    protected Map jsonToMap(JsonReader reader) {
        def map = [:]
        while(reader.hasNext()) {
            def nextToken = reader.peek()
            switch(nextToken) {
                case JsonToken.NAME:
                    def name = reader.nextName()
                    def tokenAfterName = reader.peek()
                    map[name] = getValueForToken(tokenAfterName, reader)
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

    protected getValueForToken(JsonToken currentToken, JsonReader reader) {
        def valueToAdd
        switch(currentToken) {
            case JsonToken.BEGIN_OBJECT:
                valueToAdd = jsonToMap reader
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
        }
         valueToAdd
    }

    @Override
    Set getPropertyNames() {
        data.keySet()
    }

    @Override
    Object getPropertyValue(String propertyName) {
        data[propertyName]
    }

    @Override
    Object getAt(String propertyName) {
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
    
    @Override getIdentifierValue() {
        data['id']
    }
    
    @Override
    int size() {
        data.size()
    }
}
