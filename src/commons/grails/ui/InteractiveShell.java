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
 * WITHOUT c;pWARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.ui;

import grails.util.GrailsUtil;
import groovy.lang.Binding;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.context.ApplicationContext;
/**
 * 
 * Extends regular Groovy interactive shell and bootstraps Grails environment before launch
 * to allow interaction with the Grails domain model
 * 
 *
 * @author Graeme Rocher
 * @since 0.2
 * 
 * @version $Revision$
 * First Created: 02-Jun-2006
 * Last Updated: $Date$
 *
 */

public class InteractiveShell {
	
	public static void main(String[] args) throws Exception {		
		ApplicationContext ctx = GrailsUtil.bootstrapGrailsFromClassPath();
		GrailsApplication app = (GrailsApplication)ctx.getBean(GrailsApplication.APPLICATION_ID);

		Class[] loadedClasses = app.getAllClasses();		
		Binding b = new Binding();
		
		for (int i = 0; i < loadedClasses.length; i++) {
			b.setVariable(loadedClasses[i].getName(), loadedClasses[i]);
		}
		
		final groovy.ui.InteractiveShell shell = new groovy.ui.InteractiveShell(app.getClassLoader(),b, System.in,System.out,System.err);
		shell.run(args);
	}
}
