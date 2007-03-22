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
 * <p>Represents a task class in Grails</p> 
 * 
 * @author Micha?? K??ujszo
 * @author Graeme Rocher
 * @author Marcel Overdijk
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
	 * @return task timeout between executions
	 */
	public long getTimeout();
	
	/**
	 * @return start delay before first execution after starting scheduler
	 */
	public long getStartDelay();
	
	/** 
	 * @return cron expression used for configuring scheduler
	 */
	public String getCronExpression();
	
	/**
	 * @return group name used for configuring scheduler
	 */
	public String getGroup();
	
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

	/**
	 * @return True task requires Hibernate Session bounded to thread
	 */
	public boolean isSessionRequired();
}
