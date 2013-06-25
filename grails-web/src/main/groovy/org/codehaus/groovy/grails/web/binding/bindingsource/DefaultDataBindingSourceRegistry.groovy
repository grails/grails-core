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
import org.grails.databinding.bindingsource.DataBindingSourceCreator
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
class DefaultDataBindingSourceRegistry implements DataBindingSourceRegistry {

    @Autowired(required=false)
    Set<DataBindingSourceCreator> helpers

    protected DataBindingSourceCreator getDataBindingSourceCreator(MimeType mimeType, Class targetType, Object bindingSource) {
        def creator = null
        creator = helpers?.find { DataBindingSourceCreator dbsh -> dbsh.mimeTypes.any { MimeType mt -> mt  == mimeType  }}
        if(creator == null) {
            creator = new DefaultDataBindingSourceCreator()
        }
        creator
    }

    @Override
    public DataBindingSource createDataBindingSource(MimeType mimeType,
            Object bindingTarget, Object bindingSource) {
        def helper = getDataBindingSourceCreator(mimeType, bindingTarget.getClass(), bindingSource)
        helper.createDataBindingSource(mimeType, bindingTarget, bindingSource)
    }
}
