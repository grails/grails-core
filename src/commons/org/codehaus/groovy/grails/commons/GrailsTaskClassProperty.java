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
package org.codehaus.groovy.grails.commons;

/**
 * <p>Holds properties names from <code>GrailsTaskClass</code></p> 
 * 
 * @see GrailsTaskClass
 * @author Micha?? K??ujszo
 * @author Graeme Rocher
 * @author Marcel Overdijk
 * 
 * @since 0.2
 * 
 * Created: 20-Apr-2006
 */
public interface GrailsTaskClassProperty {

	public static final String EXECUTE = "execute";
	
	public static final String TIMEOUT = "timeout";
	
	public static final String START_DELAY = "startDelay";
	
	public static final String CRON_EXPRESSION = "cronExpression";
	
	public static final String GROUP = "group";		
	
	public static final String CONCURRENT = "concurrent";
	
	public static final String SESSION_REQUIRED = "sessionRequired";
}
