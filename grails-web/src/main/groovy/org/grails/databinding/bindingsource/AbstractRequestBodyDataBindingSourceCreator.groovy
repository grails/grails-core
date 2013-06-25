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
package org.grails.databinding.bindingsource

import groovy.transform.CompileStatic
import org.grails.databinding.SimpleMapDataBindingSource

import javax.servlet.http.HttpServletRequest

import org.codehaus.groovy.grails.web.mime.MimeType
import org.grails.databinding.DataBindingSource

@CompileStatic
abstract class AbstractRequestBodyDataBindingSourceCreator implements DataBindingSourceCreator {

    @Override
    Class getTargetType() {
        Object
    }

    @Override
    public DataBindingSource createDataBindingSource(MimeType mimeType, Object bindingTarget, Object bindingSource) {
        if(bindingSource instanceof HttpServletRequest) {
            def req = (HttpServletRequest)bindingSource
            def is = req.getInputStream()
            return createBindingSource(is)
        }
        else if(bindingSource instanceof Map) {
            return new SimpleMapDataBindingSource(bindingSource)
        }
    }

    protected abstract DataBindingSource createBindingSource(InputStream inputStream)
}
