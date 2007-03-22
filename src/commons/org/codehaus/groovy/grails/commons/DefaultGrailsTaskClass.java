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

import groovy.lang.Closure;

/** 
 * @author Micha?? K??ujszo
 * @author Marcel Overdijk
 * @author Sergey Nebolsin
 * 
 * @since 20-Apr-2006
 */
public class DefaultGrailsTaskClass extends AbstractInjectableGrailsClass implements GrailsTaskClass, GrailsTaskClassProperty {
	
	public static final String JOB = "Job";
	
	public static final long DEFAULT_TIMEOUT = 60000l;	// one minute
	public static final long DEFAULT_START_DELAY = 30000l; // let grails application to startup	
	public static final String DEFAULT_CRON_EXPRESSION = "0 0 6 * * ?";
	public static final String DEFAULT_GROUP = "GRAILS_JOBS";
	public static final boolean DEFAULT_CONCURRENT = true;
	public static final boolean	DEFAULT_SESSION_REQUIRED = true;
	
	public DefaultGrailsTaskClass(Class clazz) {
		super(clazz, JOB);
		// Validate startDelay and timeout property types
		Object obj = getPropertyValue(TIMEOUT);
		if( obj != null && !(obj instanceof Integer || obj instanceof Long)) {
			throw new IllegalArgumentException("Timeout property for job class " + getClazz().getName() + " must be Integer or Long");
		}
		obj = getPropertyValue(START_DELAY);
		if( obj != null && !(obj instanceof Integer || obj instanceof Long)) {
			throw new IllegalArgumentException("Start delay property for job class " + getClazz().getName() + " must be Integer or Long");
		}
	}

	public void execute() {
		((Closure)getPropertyOrStaticPropertyOrFieldValue(EXECUTE, Closure.class)).call();
	}

	public long getTimeout() {
		Object obj = getPropertyValue(TIMEOUT);
		if( obj == null ) return DEFAULT_TIMEOUT;
		return ((Number)obj).longValue();
	}

	public long getStartDelay() {
		Object obj = getPropertyValue(START_DELAY);
		if( obj == null ) return DEFAULT_START_DELAY;
		return ((Number)obj).longValue();
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

	// not certain about this... feels messy
	public boolean isCronExpressionConfigured() {
		String cronExpression = (String)getPropertyOrStaticPropertyOrFieldValue(CRON_EXPRESSION, String.class);
		if( cronExpression == null ) return false;
		return true;
	}

	public boolean isConcurrent() {
		Boolean concurrent = (Boolean)getPropertyValue(CONCURRENT, Boolean.class);
		if ( concurrent == null ) return DEFAULT_CONCURRENT;
		return concurrent.booleanValue();
	}	

	public boolean isSessionRequired() {
		Boolean concurrent = (Boolean)getPropertyValue(SESSION_REQUIRED, Boolean.class);
		if ( concurrent == null ) return DEFAULT_SESSION_REQUIRED;
		return concurrent.booleanValue();
	}	
}