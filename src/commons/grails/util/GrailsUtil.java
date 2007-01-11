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
package grails.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * 
 * Grails utility methods for command line and GUI applications
 *
 * @author Graeme Rocher
 * @since 0.2
 * 
 * @version $Revision$
 * First Created: 02-Jun-2006
 * Last Updated: $Date$
 *
 */
public class GrailsUtil {

	private static final Log LOG  = LogFactory.getLog(GrailsUtil.class);
	
	public static ApplicationContext bootstrapGrailsFromClassPath() {
		LOG.info("Loading Grails environment");
		ApplicationContext parent = new ClassPathXmlApplicationContext("applicationContext.xml");
		DefaultGrailsApplication application = (DefaultGrailsApplication)parent.getBean("grailsApplication", DefaultGrailsApplication.class);
		
		GrailsRuntimeConfigurator config = new GrailsRuntimeConfigurator(application,parent);
		MockServletContext servletContext = new MockServletContext();
		ConfigurableApplicationContext appCtx = (ConfigurableApplicationContext)config.configure(servletContext);
		servletContext.setAttribute( GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx);
		Assert.notNull(appCtx);
		return appCtx;
	}
	
	public static void bindMockWebRequest(GrailsWebApplicationContext ctx) {
		GrailsWebRequest webRequest = new GrailsWebRequest(
												new MockHttpServletRequest(),
												new MockHttpServletResponse(),
												ctx.getServletContext()
											);
		RequestContextHolder.setRequestAttributes(webRequest);
	}
}
