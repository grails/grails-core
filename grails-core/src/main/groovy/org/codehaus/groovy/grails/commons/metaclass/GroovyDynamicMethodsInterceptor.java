/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.commons.metaclass;

import groovy.lang.GroovyObject;

/**
 * For dynamic methods applied to a Groovy object that registers itself with the GroovyObject instances
 * meta class instance.
 *
 * @author Graeme Rocher
 */
public class GroovyDynamicMethodsInterceptor extends AbstractDynamicMethodsInterceptor {

    private ProxyMetaClass pmc;

    public GroovyDynamicMethodsInterceptor(GroovyObject go) {
        super(go.getClass(), false);
        pmc = ProxyMetaClass.getInstance(go.getClass());
        pmc.setInterceptor(this);
        go.setMetaClass(pmc);
    }
}
