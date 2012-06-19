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
package org.codehaus.groovy.grails.commons;

import grails.util.Environment;
import groovy.lang.Closure;

import javax.servlet.ServletContext;

@SuppressWarnings("serial")
public class DefaultGrailsBootstrapClass extends AbstractGrailsClass implements GrailsBootstrapClass {

    public static final String BOOT_STRAP = "BootStrap";

    private static final String INIT_CLOSURE = "init";
    private static final String DESTROY_CLOSURE = "destroy";
    @SuppressWarnings("rawtypes")
    private static final Closure BLANK_CLOSURE = new Closure(DefaultGrailsBootstrapClass.class) {
        @Override
        public Object call(Object... args) { return null; }
    };

    public DefaultGrailsBootstrapClass(Class<?> clazz) {
        super(clazz, BOOT_STRAP);
    }

    public Closure<?> getInitClosure() {
        Object obj = getPropertyValueObject(INIT_CLOSURE);
        if (obj instanceof Closure) {
            return (Closure<?>)obj;
        }
        return BLANK_CLOSURE;
    }

    public Closure<?> getDestroyClosure() {
        Object obj = getPropertyValueObject(DESTROY_CLOSURE);
        if (obj instanceof Closure) {
            return (Closure<?>)obj;
        }
        return BLANK_CLOSURE;
    }

    public void callInit(ServletContext servletContext) {
        Closure<?> init = getInitClosure();
        if (init != null) {
            init = init.curry(new Object[]{servletContext});
            Environment.executeForCurrentEnvironment(init);
        }
    }

    public void callDestroy() {
        Closure<?> destroy = getDestroyClosure();
        if (destroy != null) {
            Environment.executeForCurrentEnvironment(destroy);
        }
    }
}
