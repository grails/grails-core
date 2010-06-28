/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.scaffolding;

import java.util.Comparator;

import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.springframework.util.Assert;

/**
 * Comparator used when Hibernate isn't installed.
 *
 * @author Burt Beckwith
 */
@SuppressWarnings("unchecked")
public class SimpleDomainClassPropertyComparator implements Comparator {

    private GrailsDomainClass domainClass;

    public SimpleDomainClassPropertyComparator(GrailsDomainClass domainClass) {
        Assert.notNull(domainClass, "Argument 'domainClass' is required!");
        this.domainClass = domainClass;
    }

    public int compare(Object o1, Object o2) {
        if (o1.equals(domainClass.getIdentifier())) {
            return -1;
        }
        if (o2.equals(domainClass.getIdentifier())) {
            return 1;
        }
        return 0;
    }
}
