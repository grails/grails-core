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
package org.grails.databinding.xml

import grails.databinding.CollectionDataBindingSource;
import grails.databinding.DataBindingSource;
import grails.databinding.SimpleMapDataBindingSource;
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult

@CompileStatic
class GPathResultCollectionDataBindingSource implements CollectionDataBindingSource {

    protected List<? extends DataBindingSource> dataBindingSources

    GPathResultCollectionDataBindingSource(GPathResult gpath) {
        dataBindingSources = gpath?.children()?.collect { child ->
            def map = new GPathResultMap((GPathResult)child)
            DataBindingSource bindingSource = new SimpleMapDataBindingSource(map)
            bindingSource
        }
    }

    @Override
    List<DataBindingSource> getDataBindingSources() {
        dataBindingSources
    }
}
