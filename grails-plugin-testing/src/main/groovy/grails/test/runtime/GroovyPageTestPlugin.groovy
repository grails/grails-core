/*
 * Copyright 2014 the original author or authors.
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

package grails.test.runtime

import groovy.transform.CompileStatic
import org.grails.web.pages.GroovyPageBinding
import org.grails.web.taglib.WebRequestTemplateVariableBinding
import org.grails.web.util.GrailsApplicationAttributes
import org.grails.web.servlet.mvc.GrailsWebRequest

/**
 * a TestPlugin for TestRuntime for supporting GSP tests
 * 
 * @author Lari Hotari
 * @since 2.4.0
 *
 */
@CompileStatic
class GroovyPageTestPlugin implements TestPlugin {
    String[] requiredFeatures = ['controller']
    String[] providedFeatures = ['groovyPage']
    int ordinal = 0

    protected void bindPageScope(TestRuntime runtime) {
        GrailsWebRequest webRequest = (GrailsWebRequest) runtime.getValue("webRequest")
        GroovyPageBinding pageScope = new GroovyPageBinding(new WebRequestTemplateVariableBinding(webRequest))
        webRequest.request.setAttribute(GrailsApplicationAttributes.PAGE_SCOPE, pageScope)
        runtime.putValue("pageScope", pageScope)
    }

    protected void clearPageScope(TestRuntime runtime) {
        runtime.removeValue("pageScope")
    }
    
    public void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'before':
                bindPageScope(event.runtime)
                break
            case 'after':
                clearPageScope(event.runtime)
                break
        }
    }
    
    public void close(TestRuntime runtime) {
        
    }
}