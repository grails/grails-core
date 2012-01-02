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

import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.grails.web.pages.AbstractGroovyPageBinding;
import org.codehaus.groovy.grails.web.pages.GroovyPageBinding;
import org.codehaus.groovy.grails.web.pages.GroovyPageOutputStack;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.util.Assert;

/**
 * Represents the body of a tag and captures its output returning the result
 * when invoked.
 * 
 * @author Graeme Rocher
 * @since 0.5
 */
@SuppressWarnings("rawtypes")
public class GroovyPageTagBody extends Closure {

    private static final long serialVersionUID = 4396762064131558457L;
    private Closure<?> bodyClosure;
    private GrailsWebRequest webRequest;

    public GroovyPageTagBody(Object owner, GrailsWebRequest webRequest,
            Closure<?> bodyClosure) {
        super(owner);

        Assert.notNull(bodyClosure, "Argument [bodyClosure] cannot be null!");
        Assert.notNull(webRequest, "Argument [webRequest] cannot be null!");

        this.bodyClosure = bodyClosure;
        this.webRequest = webRequest;

    }

    @SuppressWarnings("unchecked")
    private Object captureClosureOutput(Object args) {
        final GroovyPageTagWriter capturedOut = new GroovyPageTagWriter();
        Binding currentBinding = (Binding)webRequest.getCurrentRequest().getAttribute(
                GrailsApplicationAttributes.PAGE_SCOPE);
        Map<String,Object> savedVariablesMap = null;
        Set<String> bodyArgumentKeys = null;
        boolean itChanged = false;
        Object originalIt = null;
        try {
            pushCapturedOut(capturedOut);

            Object bodyResult;

            if (args != null) {
                Map<String,Object> variablesMap = (currentBinding instanceof AbstractGroovyPageBinding) ? ((AbstractGroovyPageBinding)currentBinding)
                        .getVariablesMap() : currentBinding.getVariables();
                originalIt = variablesMap.get("it");
                variablesMap.put("it", args);
                itChanged = true;

                if (args instanceof Map && ((Map)args).size() > 0) {
                    // The body can be passed a set of variables as a map that
                    // are then made available in the binding. This allows the
                    // contents of the body to reference any of these variables
                    // directly.
                    //
                    // For example, body(foo: 1, bar: 'test') would allow this
                    // GSP fragment to work:
                    //
                    // <td>Foo: ${foo} and bar: ${bar}</td>
                    //
                    // Note that any variables with the same name as one of the
                    // new ones will be overridden for the scope of the host
                    // tag's body.

                    // GRAILS-2675: Copy the current binding so that we can
                    // restore
                    // it to its original state.

                    savedVariablesMap = new LinkedHashMap<String,Object>(variablesMap);
                    bodyArgumentKeys = new HashSet<String>();
                    for (Iterator<Object> i = ((Map)args).keySet().iterator(); i.hasNext(); ) {
                        bodyArgumentKeys.add(String.valueOf(i.next()));
                    }
                    ((GroovyPageBinding)currentBinding).addMap((Map)args);
                    // Binding is only changed currently when body gets a map
                    // argument
                }
            }
            bodyResult = executeClosure(args);

            if (!capturedOut.isUsed() && bodyResult != null && !(bodyResult instanceof Writer)) {
                return bodyResult;
            }
            return capturedOut.getBuffer();
        }
        finally {
            Map<String,Object> variablesMap = (currentBinding instanceof AbstractGroovyPageBinding) ? ((AbstractGroovyPageBinding)currentBinding)
                    .getVariablesMap() : currentBinding.getVariables();
            restoreVariables(variablesMap, savedVariablesMap, bodyArgumentKeys);
            if (itChanged) {
                variablesMap.put("it", originalIt);
            }
            popCapturedOut();
        }
    }

    protected void restoreVariables(Map<String,Object> variablesMap, Map<String, Object> savedVariablesMap, Set<String> bodyArgumentKeys) {

        if (savedVariablesMap != null) {
            for(Iterator<Map.Entry<String, Object>> mapIt = variablesMap.entrySet().iterator();mapIt.hasNext();) {
                Map.Entry<String, Object> entry = mapIt.next();
                String varname = entry.getKey();
                if(!savedVariablesMap.containsKey(varname)) {
                    mapIt.remove();
                } else if (bodyArgumentKeys.contains(varname)) {
                    entry.setValue(savedVariablesMap.get(varname));
                }
            }
            for(Map.Entry<String, Object> entry : savedVariablesMap.entrySet()) {
                if(!variablesMap.containsKey(entry.getKey())) {
                    variablesMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private void popCapturedOut() {
        if (webRequest != null && webRequest.isActive()) {
            GroovyPageOutputStack.currentStack(webRequest).pop();
        }
        else {
            GroovyPageOutputStack.currentStack().pop();
        }
    }

    private void pushCapturedOut(GroovyPageTagWriter capturedOut) {
        if (webRequest != null && webRequest.isActive()) {
            GroovyPageOutputStack.currentStack(webRequest).push(capturedOut);
        }
        else {
            GroovyPageOutputStack.currentStack().push(capturedOut);
        }
    }

    private Object executeClosure(Object args) {
        if (args != null && bodyClosure.getMaximumNumberOfParameters() > 0) {
            return bodyClosure.call(args);
        }
        return bodyClosure.call();
    }

    public Object doCall() {
        return captureClosureOutput(null);
    }

    public Object doCall(Object arguments) {
        return captureClosureOutput(arguments);
    }

    @Override
    public Object call() {
        return captureClosureOutput(null);
    }

    @Override
    public Object call(Object... args) {
        return captureClosureOutput(args);
    }

    @Override
    public Object call(Object arguments) {
        return captureClosureOutput(arguments);
    }
}
