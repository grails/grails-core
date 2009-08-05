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
import org.hibernate.SessionFactory;

import java.util.regex.Pattern;

/**
 * The "find<booleanProperty>By*" static persistent method. This method allows querying for
 * instances of grails domain classes based on a boolean property and any other arbitrary
 * properties. This method returns the first result of the query.
 *
 * eg.
 * Account.findActiveByHolder("Joe Blogs"); // Where class "Account" has a properties called "active" and "holder"
 * Account.findActiveByHolderAndBranch("Joe Blogs", "London" ); // Where class "Account" has a properties called "active', "holder" and "branch"
 *
 * In both of those queries, the query will only select Account objects where active=true.
 *
 * @author Jeff Brown
 *
 */
public class FindByBooleanPropertyPersistentMethod extends FindByPersistentMethod {

	private static final String METHOD_PATTERN = "(find)(\\w+)(By)(\\w+)";

	public FindByBooleanPropertyPersistentMethod(GrailsApplication application,SessionFactory sessionFactory, ClassLoader classLoader) {
 		super(application,sessionFactory, classLoader);
        setPattern(Pattern.compile( METHOD_PATTERN ));
	}

    @Override
    protected boolean firstExpressionIsRequiredBoolean() {
        return true;
    }
}