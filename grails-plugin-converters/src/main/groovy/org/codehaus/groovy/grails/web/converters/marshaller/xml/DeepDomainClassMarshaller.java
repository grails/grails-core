/*
 * Copyright 2004-2008 the original author or authors.
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
package org.codehaus.groovy.grails.web.converters.marshaller.xml;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.support.proxy.ProxyHandler;

/**
 * @author Siegfried Puchbauer
 * @author Graeme Rocher
 *
 * @since 1.1
 */
public class DeepDomainClassMarshaller extends DomainClassMarshaller {

    public DeepDomainClassMarshaller(boolean includeVersion, GrailsApplication application) {
        super(includeVersion, application);
    }

    public DeepDomainClassMarshaller(GrailsApplication application) {
        super(application);
    }

    public DeepDomainClassMarshaller(boolean includeDomainVersion, ProxyHandler proxyHandler, GrailsApplication application) {
        super(includeDomainVersion, proxyHandler, application);
    }

    @Override
    protected boolean isRenderDomainClassRelations() {
        return true;
    }
}
