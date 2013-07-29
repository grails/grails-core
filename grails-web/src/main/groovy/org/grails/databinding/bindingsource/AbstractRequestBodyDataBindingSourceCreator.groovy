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

import javax.servlet.http.HttpServletRequest

import org.codehaus.groovy.grails.web.binding.bindingsource.DefaultDataBindingSourceCreator
import org.codehaus.groovy.grails.web.mime.MimeType
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.grails.databinding.CollectionDataBindingSource
import org.grails.databinding.DataBindingSource

@CompileStatic
abstract class AbstractRequestBodyDataBindingSourceCreator extends DefaultDataBindingSourceCreator {

    @Override
    Class getTargetType() {
        Object
    }

    @Override
    DataBindingSource createDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource) throws DataBindingSourceCreationException {
        try {
            if(bindingSource instanceof GrailsParameterMap) {
                def req = bindingSource.getRequest()
                def is = req.getInputStream()
                return createBindingSource(is)
            }
            if(bindingSource instanceof HttpServletRequest) {
                def req = (HttpServletRequest)bindingSource
                def is = req.getInputStream()
                return createBindingSource(is)
            }
            if(bindingSource instanceof InputStream) {
                def is = (InputStream)bindingSource
                return createBindingSource(is)
            }
            if(bindingSource instanceof Reader) {
                def is = (Reader)bindingSource
                return createBindingSource(is)
            }

            return super.createDataBindingSource(mimeType, bindingTargetType, bindingSource)
        } catch (Exception e) {
            throw new DataBindingSourceCreationException(e)
        }
    }

    @Override
    CollectionDataBindingSource createCollectionDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource) throws DataBindingSourceCreationException {
        try {
            if(bindingSource instanceof GrailsParameterMap) {
                def req = bindingSource.getRequest()
                def is = req.getInputStream()
                return createCollectionBindingSource(is)
            }
            if(bindingSource instanceof HttpServletRequest) {
                def req = (HttpServletRequest)bindingSource
                def is = req.getInputStream()
                return createCollectionBindingSource(is)
            }
            if(bindingSource instanceof InputStream) {
                def is = (InputStream)bindingSource
                return createCollectionBindingSource(is)
            }
            if(bindingSource instanceof Reader) {
                def is = (Reader)bindingSource
                return createCollectionBindingSource(is)
            }

            return super.createCollectionDataBindingSource(mimeType, bindingTargetType, bindingSource)
        } catch (Exception e) {
            throw new DataBindingSourceCreationException(e)
        }
    }

    protected DataBindingSource createBindingSource(InputStream inputStream){
        return createBindingSource(new InputStreamReader(inputStream))
    }

    protected abstract DataBindingSource createBindingSource(Reader reader)

    protected CollectionDataBindingSource createCollectionBindingSource(InputStream inputStream){
        return createCollectionBindingSource(new InputStreamReader(inputStream))
    }

    protected abstract CollectionDataBindingSource createCollectionBindingSource(Reader reader)
}
