/*
 * Copyright 2004-2005 the original author or authors.
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
package grails.util;

import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.support.GrailsTestSuite;
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Modifier;

/**
 * 
 * 
 * @author Steven Devijver
 * @author Graeme Rocher
 * 
 * @since 0.1
 * 
 * Created: Aug 8, 2005
 */
public class RunTests {

	private static Log log = LogFactory.getLog(RunTests.class);
	
	public static void main(String[] args) {
		int exitCode = 0;
		try {     
			log.info("Bootstrapping Grails from classpath");
			ConfigurableApplicationContext appCtx = (ConfigurableApplicationContext)GrailsUtil.bootstrapGrailsFromClassPath();
			GrailsApplication application = (GrailsApplication)appCtx.getBean(GrailsApplication.APPLICATION_ID);

			Class[] allClasses = application.getAllClasses();
	        log.debug("Going through ["+allClasses.length+"] classes");
	        TestSuite s = new TestSuite();
			for (int i = 0; i < allClasses.length; i++) {
				Class clazz = allClasses[i];
				if (TestCase.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
					log.debug("Adding test [" + clazz.getName() + "]");
					s.addTest(new GrailsTestSuite(appCtx, clazz));
				} else {
					log.debug("[" + clazz.getName() + "] is not a test case.");
				}
			}
			String[] beanNames = appCtx.getBeanNamesForType(PersistenceContextInterceptor.class);
			PersistenceContextInterceptor interceptor = null;
			if(beanNames.length > 0) {
				interceptor = (PersistenceContextInterceptor)appCtx.getBean(beanNames[0]);
			}
			
			try {
				if(interceptor!=null) {
					interceptor.init();
				}
				TestResult r = TestRunner.run(s);
				exitCode = r.errorCount() + r.failureCount();
				if(exitCode > 0) {
					System.err.println("Tests failed!");
				}
                if(interceptor !=null)
                    interceptor.flush();                
            }
			finally {
				if(interceptor !=null)
					interceptor.destroy();
			}
		} 
		catch(Exception e) {
			log.error("Error executing tests: " + e.getMessage(), e);
			exitCode = 1;
		}
		finally {
			System.exit(exitCode);
		}
	}
}
