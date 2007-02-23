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
package org.codehaus.groovy.grails.metaclass;

import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodInvocation;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.regex.Pattern;

/**
 * A dynamic method that evaluates the id of a domain class whatever the property name may be. Called via
 * domainInstance.ident()
 *
 * @author Graeme Rocher
 * @since Oct 24, 2005
 */

public class IdentDynamicMethod extends AbstractDynamicMethodInvocation {
    public static final String METHOD_SIGNATURE = "ident";
    public static final Pattern METHOD_PATTERN = Pattern.compile('^'+METHOD_SIGNATURE+'$');
    private GrailsApplication application;

    public IdentDynamicMethod(GrailsApplication application) {
        super(METHOD_PATTERN);
        this.application = application;
    }

    public Object invoke(Object target, String methodName, Object[] arguments) {
        BeanWrapper bean = new BeanWrapperImpl(target);
        GrailsDomainClass domainClass = (GrailsDomainClass) application.getArtefact(
            DomainClassArtefactHandler.TYPE, target.getClass().getName());
        return bean.getPropertyValue(domainClass.getIdentifier().getName());
    }
}
