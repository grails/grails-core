/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.web.taglib;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GString;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.grails.web.pages.GroovyPageOutputStack;
import org.codehaus.groovy.grails.web.pages.GroovyPageUtils;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.StreamCharBuffer;

/**
 * A closure that represents the body of a tag and captures its output returning the result when invoked
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Apr 19, 2007
 *        Time: 2:21:38 PM
 */
public class GroovyPageTagBody extends Closure {
    private Closure bodyClosure;
    private GrailsWebRequest webRequest;
    private Binding binding;
    private static final String BLANK_STRING = "";
    private boolean writeStringResult = false;

    public GroovyPageTagBody(Object owner, GrailsWebRequest webRequest, Closure bodyClosure) {
        this(owner, webRequest, false, bodyClosure);
    }

    public GroovyPageTagBody(Object owner, GrailsWebRequest webRequest, boolean writeStringResult, Closure bodyClosure) {
        super(owner);

        if(bodyClosure == null) throw new IllegalStateException("Argument [bodyClosure] cannot be null!");
        if(webRequest == null) throw new IllegalStateException("Argument [webRequest] cannot be null!");

        this.bodyClosure = bodyClosure;
        this.webRequest = webRequest;
        this.binding = GroovyPageUtils.findPageScopeBinding(owner, webRequest);

        this.writeStringResult=writeStringResult;
    }

    private Object captureClosureOutput(Object args) {
        final GroovyPageTagWriter capturedOut =  new GroovyPageTagWriter();
        try {
            GroovyPageOutputStack.currentStack().push(capturedOut);
            
            Object bodyResult;

            if(args!=null) {
                if(args instanceof Map) {
                    // The body can be passed a set of variables as a map that
                    // are then made available in the binding. This allows the
                    // contents of the body to reference any of these variables
                    // directly.
                    //
                    // For example, body(foo: 1, bar: 'test') would allow this
                    // GSP fragment to work:
                    //
                    //   <td>Foo: ${foo} and bar: ${bar}</td>
                    //
                    // Note that any variables with the same name as one of the
                    // new ones will be overridden for the scope of the host
                    // tag's body.

                    // GRAILS-2675: Copy the current binding so that we can restore
                    // it to its original state.
                    Map currentBinding = null;
                    Map originalBinding = null;

                    if(binding!=null) {
                        currentBinding = binding.getVariables();
                        originalBinding = new HashMap(currentBinding);
                        // Add the extra variables passed into the body to the
                        // current binding.
                        currentBinding.putAll((Map) args);
                    }

                    try {
                        bodyResult = executeClosure(bodyClosure, args);
                    }
                    finally {
                        if(binding!=null) {
                            // GRAILS-2675: Restore the original binding.
                            currentBinding.clear();
                            currentBinding.putAll(originalBinding);
                        }
                    }
                }
                else {
                    bodyResult = executeClosure(bodyClosure, args);
                }
            }
            else {
                bodyResult = executeClosure(bodyClosure, null);
            }

            StreamCharBuffer buffer=capturedOut.getBuffer();
            if(buffer.charsAvailable()==0 && bodyResult != null && !(bodyResult instanceof Writer)) {
       			return bodyResult;
            } 
            return buffer;
        } finally {
        	GroovyPageOutputStack.currentStack().pop();
        }
    }

	private Object executeClosure(Closure bodyClosure, Object args) {
		Object bodyResult=null;
		if(args != null) {
			bodyResult=bodyClosure.call(args);
		} else {
			bodyResult=bodyClosure.call();
		}
		return bodyResult;
	}

    public Object doCall() {
        return captureClosureOutput(null);
    }

    public Object doCall(Object arguments) {
        return captureClosureOutput(arguments);
    }

    public Object call() {
        return captureClosureOutput(null);
    }

    public Object call(Object[] args) {
        return captureClosureOutput(args);
    }

    public Object call(Object arguments) {
        return captureClosureOutput(arguments);
    }
}
