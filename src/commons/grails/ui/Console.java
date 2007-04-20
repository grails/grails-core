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
import groovy.lang.Closure;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.Iterator;

/**
 * 
 * Extends regular Groovy console and bootstraps Grails environment before launch
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
public class Console extends groovy.ui.Console {
		
	public Console(ClassLoader parent, Binding binding) {
		super(parent, binding);

    }



    public static void main(String[] args) {
		final ApplicationContext ctx = GrailsUtil.bootstrapGrailsFromClassPath();
		GrailsApplication app = (GrailsApplication)ctx.getBean(GrailsApplication.APPLICATION_ID);
		
		Binding b = new Binding();
		b.setVariable(GrailsApplication.APPLICATION_ID, app);
		b.setVariable("ctx", ctx);
		Console c = new Console(app.getClassLoader(), b);
        c.setBeforeExecution(new Closure(c) {
            public Object doCall() {
                Map beans = ctx.getBeansOfType(PersistenceContextInterceptor.class);
                for (Iterator i = beans.values().iterator(); i.hasNext();) {
                    PersistenceContextInterceptor interceptor = (PersistenceContextInterceptor) i.next();
                    interceptor.init();
                }
                return null;
            }
        });
        c.setAfterExecution(new Closure(c) {
            public Object doCall() {
                Map beans = ctx.getBeansOfType(PersistenceContextInterceptor.class);
                for (Iterator i = beans.values().iterator(); i.hasNext();) {
                    PersistenceContextInterceptor interceptor = (PersistenceContextInterceptor) i.next();
                    interceptor.flush();
                    interceptor.destroy();
                }
                return null;
            }

            public Object call() {
                return doCall();
            }

            public Object call(Object[] args) {
                return doCall();
            }

            public Object call(Object arguments) {
                return doCall();
            }
        });
        c.run();
    }
}
