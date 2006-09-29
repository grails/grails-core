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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import groovy.lang.Closure;

/** 
 * @author Micha?? K??ujszo
 * @since 20-Apr-2006
 */
public class DefaultGrailsTaskClass extends AbstractInjectableGrailsClass 
	implements GrailsTaskClass, GrailsTaskClassProperty {
	
	public DefaultGrailsTaskClass(Class clazz) {
		super(clazz, JOB);
		Log LOG = LogFactory.getLog(DefaultGrailsTaskClass.class);
		LOG.debug("instantiating: " + this.getClazz());
	}

	public void execute() {
		((Closure)getPropertyOrStaticPropertyOrFieldValue(EXECUTE, Closure.class)).call();
	}

	public String getTimeout() {
		String timeOut = (String)getPropertyOrStaticPropertyOrFieldValue(TIMEOUT, String.class);
		if( timeOut == null || "".equals(timeOut) ) return DEFAULT_TIMEOUT;
		return timeOut;
	}

	public String getStartDelay() {
		String startDelay = (String)getPropertyOrStaticPropertyOrFieldValue(START_DELAY, String.class);
		if( startDelay == null || "".equals(startDelay) ) return DEFAULT_START_DELAY;
		return startDelay;	
	}

	public String getCronExpression() {
		String cronExpression = (String)getPropertyOrStaticPropertyOrFieldValue(CRON_EXPRESSION, String.class);
		if( cronExpression == null || "".equals(cronExpression) ) return DEFAULT_CRON_EXPRESSION;
		return cronExpression;	
	}

	public String getGroup() {
		String group = (String)getPropertyOrStaticPropertyOrFieldValue(GROUP, String.class);
		if( group == null || "".equals(group) ) return DEFAULT_GROUP;
		return group;	
	}

	public String getType() {
		String type = (String)getPropertyOrStaticPropertyOrFieldValue(TYPE, String.class);
		if( type == null || "".equals(type) ) return DEFAULT_TYPE;
		return type;	
	}

	// not certain about this... feels messy
	public boolean isCronExpressionConfigured() {
		String cronExpression = (String)getPropertyOrStaticPropertyOrFieldValue(CRON_EXPRESSION, String.class);
		if( cronExpression == null ) return false;
		return true;
	}

}