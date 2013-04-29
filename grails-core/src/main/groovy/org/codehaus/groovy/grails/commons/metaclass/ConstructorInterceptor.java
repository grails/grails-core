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
package org.codehaus.groovy.grails.commons.metaclass;

/**
 * Extends interceptor interface to allow interception of constructors
 *
 * @author Graeme Rocher
 * @since 0.2
 */
public interface ConstructorInterceptor extends Interceptor {

    /**
     * Executed before the real constructor. The callback object should
     * be marked if invokation of the real constructor should be performed.
     *
     * @param args The constructor args
     * @param callback The callback object
     * @return The instantiated object or null
     */
    Object beforeConstructor(Object[] args, InvocationCallback callback);

    /**
     * Executed after the constructor passing the args and the instantiated instance.
     *
     * @param args The arguments
     * @param instantiatedInstance The instantiated instance
     * @return The instantiated or replaced instance
     */
    Object afterConstructor(Object[] args, Object instantiatedInstance);
}
