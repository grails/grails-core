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
package org.codehaus.groovy.grails.commons.metaclass;

import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * An implementation of DynamicMethodInvocation that invoces a closure
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Feb 2, 2007
 *        Time: 6:55:57 PM
 */
public class ClosureInvokingDynamicMethod implements DynamicMethodInvocation,StaticMethodInvocation, Cloneable {
    private Closure callable;
    private Pattern pattern;


    /**
     * For thread safety when using a ClosureInvokingDynamicMethod it should ALWAYS be cloned first
     * Weird behaviour will occur if a unique cloned instance is not used for each invocation
     *
     * @return A cloned instance
     * @throws CloneNotSupportedException
     */
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public ClosureInvokingDynamicMethod(String pattern, Closure closure) {
        if(StringUtils.isBlank(pattern)) {
            throw new IllegalArgumentException("Argument [pattern] must be a valid regular expression");
        }
        if(closure == null) {
            throw new IllegalArgumentException("Argument [closure] cannot be null");
        }

        this.pattern = Pattern.compile(pattern);
        this.callable = closure;
    }

    public boolean isMethodMatch(String methodName) {
        return pattern.matcher(methodName).find();
    }

    public Object invoke(Class clazz, String methodName, Object[] arguments) {
        Closure c = (Closure)callable.clone();
        c.setDelegate(clazz);

        return invokeMethod(methodName,arguments, c);  
    }



    public Object invoke(Object target, String methodName, Object[] arguments) {
        Closure c = (Closure)callable.clone();
        c.setDelegate(target);
        return invokeMethod(methodName, arguments,c);
    }

    private Object invokeMethod(String methodName, Object[] arguments, Closure c) {

        Matcher matcher = pattern.matcher(methodName);
        matcher.find();

        switch(c.getParameterTypes().length) {
             case 0:
                   return c.call();
             case 1:
                   return c.call(matcher);
             case 2:
                   return c.call(new Object[]{matcher, arguments});
             default:
                   return c.call(new Object[]{matcher, arguments});
        }
    }
}
