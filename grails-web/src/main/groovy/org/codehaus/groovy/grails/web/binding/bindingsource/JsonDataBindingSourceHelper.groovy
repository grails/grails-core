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

import org.codehaus.groovy.grails.web.binding.bindingsource.json.JsonDataBindingSourceCreator
import org.codehaus.groovy.grails.web.mime.MimeType
import org.grails.databinding.DataBindingSource
import org.grails.databinding.bindingsource.AbstractRequestBodyDataBindingSourceHelper

/**
 * Creates DataBindingSource objects from JSON in the request body
 * 
 * @since 2.3
 * @see DataBindingSource
 * @see DataBindingSourceHelper
 * @see JsonDataBindingSourceCreator
 */
@CompileStatic
class JsonDataBindingSourceHelper extends AbstractRequestBodyDataBindingSourceHelper {

    @Override
    public MimeType[] getMimeTypes() {
        [MimeType.JSON, MimeType.TEXT_JSON] as MimeType[]
    }
    
    @Override
    protected DataBindingSource createBindingSource(InputStream inputStream) {
        return new JsonDataBindingSourceCreator().createDataBindingSource(inputStream)
    }

}

