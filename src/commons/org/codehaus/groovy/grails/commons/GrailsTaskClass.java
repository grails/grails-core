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
 * <p>Represents a task class in Grails, provides default task configuration</p> 
 * 
 * @author Micha?? K??ujszo
 * @author Graeme Rocher
 * 
 * @since 0.2
 * 20-Apr-2006
 */
public interface GrailsTaskClass extends InjectableGrailsClass {
	
	/**
	 * <p>Method which is executed by the task scheduler</p>
	 */
	public void execute();
	
	/**
	 * @return task timeout between executions, defaults to 1 minute
	 */
	public String getTimeout();
	
	/**
	 * @return start delay before first execution after starting scheduler, defaults to 0
	 */
	public String getStartDelay();
	
	/** 
	 * @return cron expression used for configuring scheduler, defaults to "0 0 6 * * ?"
	 */
	public String getCronExpression();
	
	/**
	 * @return group name used for configuring scheduler, defaults to "GRAILS_TASKS"
	 */
	public String getGroup();
	
	/**
	 * @return task type, can be memory ( tasks stored in server memory ), jdbc or jdctx
	 */
	public String getType();
	
	/**
	 * <p>If cronExpression property is set returns true</p>
	 * @return should scheduler configure task using cron expression or timeout and startDelay properties.
	 */
	public boolean isCronExpressionConfigured();
	
	/**
	 * Return whether jobs can be executed concurrently or not
	 * 
	 * @return True if they can be executed concurrently
	 */
	public boolean isConcurrent();

	public static final String JOB = "Job";
	
	public static final String DEFAULT_TIMEOUT = "60000";	// one minute
	public static final String DEFAULT_START_DELAY = "0";	
	public static final String DEFAULT_GROUP = "GRAILS_JOBS";
	public static final String DEFAULT_CRON_EXPRESSION = "0 0 6 * * ?";
	public static final String DEFAULT_TYPE = "memory";	/* memory | jdbc | jdbctx */
	public static final String DEFAULT_CONCURRENT = "true";
}
