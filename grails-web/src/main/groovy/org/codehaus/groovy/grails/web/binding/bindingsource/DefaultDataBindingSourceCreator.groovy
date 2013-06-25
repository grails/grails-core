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

import javax.servlet.http.HttpServletRequest

import org.apache.commons.beanutils.BeanMap
import org.codehaus.groovy.grails.web.binding.DataBindingUtils
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.grails.databinding.DataBindingSource
import org.grails.databinding.SimpleMapDataBindingSource
import org.grails.databinding.bindingsource.DataBindingSourceCreator

@CompileStatic
class DefaultDataBindingSourceCreator implements DataBindingSourceCreator {

    @Override
    public MimeType[] getMimeTypes() {
        [MimeType.ALL] as MimeType[]
    }

    @Override
    Class getTargetType() {
        Object
    }

    @Override
    DataBindingSource createDataBindingSource(MimeType mimeType, Object bindingTarget, Object bindingSource) {
        final DataBindingSource dataBindingSource
        if(bindingSource instanceof DataBindingSource) {
            dataBindingSource = (DataBindingSource) bindingSource
        } else if(bindingSource instanceof HttpServletRequest) {
            dataBindingSource = createDataBindingSource(bindingTarget, (HttpServletRequest)bindingSource)
        } else if(bindingSource instanceof Map) {
            dataBindingSource = new SimpleMapDataBindingSource(DataBindingUtils.convertPotentialGStrings((Map) bindingSource))
        } else {
            dataBindingSource = new SimpleMapDataBindingSource(new BeanMap(bindingSource))
        }
        dataBindingSource
    }

    protected  DataBindingSource createDataBindingSource(Object bindingTarget, HttpServletRequest req) {
        new SimpleMapDataBindingSource(new GrailsParameterMap(req))
    }
}
