/*
 * Copyright 2004-2006 Graeme Rocher
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
package org.codehaus.groovy.grails.support;

/**
 * A  interface for a class to implement that will setup the persistent context
 * before and after a Grails operation is invoked. @see grails.util.RunTests
 * 
 * @author Graeme Rocher
 * @since 0.4
 *
 */
public interface PersistenceContextInterceptor  {

	/**
	 * Called to intialisation the persistent context
	 */
	void init();
	
	/**
	 * Called to finalize the persistent context
	 */
	void destroy();

    /**
     *  Flushes any pending changes to the DB
     */
    void flush();    
                 
	/**
	 * Clear any pending changes
	 */
	void clear();
}
