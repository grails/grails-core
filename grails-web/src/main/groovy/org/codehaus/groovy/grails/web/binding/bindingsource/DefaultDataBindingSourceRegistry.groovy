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

import groovy.transform.CompileStatic;

import org.codehaus.groovy.grails.web.mime.MimeType

@CompileStatic
class DefaultDataBindingSourceRegistry implements DataBindingSourceRegistry {

    Set<DataBindingSourceHelper> helpers = new HashSet<DataBindingSourceHelper>()

    DefaultDataBindingSourceRegistry() {
        helpers.add(new XmlDataBindingSourceHelper());
        helpers.add(new JsonDataBindingSourceHelper());
        helpers.add(new HalJsonDataBindingSourceHelper());
    }

    @Override
    DataBindingSourceHelper getDataBindingSourceHelper(MimeType mimeType, Class targetType, Object bindingSource) {
        DataBindingSourceHelper helper = null
        helper = helpers.find { DataBindingSourceHelper dbsh -> dbsh.mimeTypes.any { MimeType mt -> mt  == mimeType  }}
        if(helper == null) {
            helper = new DefaultDataBindingSourceHelper()
        }
        helper
    }
}
