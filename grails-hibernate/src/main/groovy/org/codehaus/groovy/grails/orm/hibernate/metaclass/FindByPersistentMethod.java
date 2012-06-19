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
package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore;
import org.hibernate.SessionFactory;

import java.util.regex.Pattern;

/**
 * The "findBy*" static persistent method. This method allows querying for
 * instances of grails domain classes based on their properties. This method returns the first result of the query
 *
 * eg.
 * Account.findByHolder("Joe Blogs"); // Where class "Account" has a property called "holder"
 * Account.findByHolderAndBranch("Joe Blogs", "London"); // Where class "Account" has a properties called "holder" and "branch"
 *
 * @author Graeme Rocher
 * @since 31-Aug-2005
 */
public class FindByPersistentMethod extends AbstractFindByPersistentMethod {

    private static final String METHOD_PATTERN = "(findBy)([A-Z]\\w*)";

    /**
     * Constructor.
     * @param application
     * @param sessionFactory
     * @param classLoader
     */
    public FindByPersistentMethod(HibernateDatastore datastore, GrailsApplication application,SessionFactory sessionFactory, ClassLoader classLoader) {
        super(datastore,application,sessionFactory, classLoader, Pattern.compile(METHOD_PATTERN), OPERATORS);
    }
}
