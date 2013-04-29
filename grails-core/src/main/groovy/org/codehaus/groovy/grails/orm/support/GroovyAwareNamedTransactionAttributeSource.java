/*
 * Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.orm.support;

import grails.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Set;

import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * @author Graeme Rocher
 * @since 1.1.1 Don't match Groovy synthetic methods
 */
public class GroovyAwareNamedTransactionAttributeSource extends NameMatchTransactionAttributeSource {

    private static final long serialVersionUID = 3519687998898725875L;
    private static final Set<String> NONTRANSACTIONAL_GROOVY_METHODS = CollectionUtils.newSet(
            "invokeMethod",
            "getMetaClass",
            "getProperty",
            "setProperty");

    @SuppressWarnings("rawtypes")
    @Override
    public TransactionAttribute getTransactionAttribute(Method method, Class targetClass) {
        if (method.isSynthetic()) return null;
        return super.getTransactionAttribute(method, targetClass);
    }

    @Override
    protected boolean isMatch(String methodName, String mappedName) {
        if (NONTRANSACTIONAL_GROOVY_METHODS.contains(methodName)) return false;
        return super.isMatch(methodName, mappedName);
    }

    public void setTransactionalAttributes(Properties properties) {
        super.setProperties(properties);
    }
}
