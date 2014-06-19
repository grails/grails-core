/*
 * Copyright 2002-2008 the original author or authors.
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
package org.codehaus.groovy.grails.beans.factory

import groovy.transform.CompileStatic
import org.springframework.beans.factory.ListableBeanFactory

/**
 * A fork of the Spring 2.5.6 GenericBeanFactoryAccess class that was removed from Spring 3.0.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated Will be removed in a future version of Grails
 *
 */
@Deprecated
@CompileStatic
class GenericBeanFactoryAccessor extends org.grails.spring.beans.factory.GenericBeanFactoryAccessor{
    /**
     * Constructs a <code>GenericBeanFactoryAccessor</code> that wraps the supplied {@link ListableBeanFactory}.
     */
    GenericBeanFactoryAccessor(ListableBeanFactory beanFactory) {
        super(beanFactory)
    }
}
