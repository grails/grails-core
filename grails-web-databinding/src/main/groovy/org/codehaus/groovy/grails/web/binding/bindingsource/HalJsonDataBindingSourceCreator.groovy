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

import org.codehaus.groovy.grails.web.mime.MimeType
import org.grails.databinding.DataBindingSource

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Creates DataBindingSource objects from HAL JSON in the request body
 *
 * @since 2.3
 *
 * @author Jeff Brown
 * @author Graeme Rocher
 *
 * @see DataBindingSource
 * @see DataBindingSourceCreator
 */
@CompileStatic
class HalJsonDataBindingSourceCreator extends JsonDataBindingSourceCreator {

    public static final String HAL_EMBEDDED_ELEMENT = "_embedded"

    @Override
    MimeType[] getMimeTypes() {
        [MimeType.HAL_JSON] as MimeType[]
    }

    @Override
    protected Map createJsonObjectMap(JsonElement jsonElement) {
        jsonElement instanceof JsonObject ? new HalJsonObjectMap(jsonElement, gson) : [:]
    }

    @CompileStatic
    class HalJsonObjectMap extends JsonDataBindingSourceCreator.JsonObjectMap {

        HalJsonObjectMap(JsonObject jsonObject, Gson gson) {
            super(jsonObject, gson)

            if (!jsonObject.has(HAL_EMBEDDED_ELEMENT)) {
                return
            }

            final embeddedObject = jsonObject.get(HAL_EMBEDDED_ELEMENT)
            jsonObject.remove(HAL_EMBEDDED_ELEMENT)
            if (embeddedObject instanceof JsonObject) {
                JsonObject embeddedJson = (JsonObject)embeddedObject

                for (entry in embeddedJson.entrySet()) {
                    jsonObject.add(entry.key, entry.value)
                }
            }
        }
    }
}
