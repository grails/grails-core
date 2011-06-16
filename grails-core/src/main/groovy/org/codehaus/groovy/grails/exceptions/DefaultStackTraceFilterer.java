/*
 * Copyright 2011 SpringSource
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

package org.codehaus.groovy.grails.exceptions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Default implementation of StackTraceFilterer.
 *
 * @since 1.4
 * @author Graeme Rocher
 */
public class DefaultStackTraceFilterer implements StackTraceFilterer {

    public static final Log STACK_LOG = LogFactory.getLog("StackTrace");

    private static final String[] DEFAULT_INTERNAL_PACKAGES = new String[] {
        "org.codehaus.groovy.grails.",
        "gant.",
        "org.codehaus.groovy.runtime.",
        "org.codehaus.groovy.reflection.",
        "org.codehaus.groovy.ast.",
        "org.codehaus.gant.",
        "groovy.",
        "org.mortbay.",
        "org.apache.catalina.",
        "org.apache.coyote.",
        "org.apache.tomcat.",
        "net.sf.cglib.proxy.",
        "sun.",
        "java.lang.reflect.",
        "org.springframework.",
        "com.springsource.loaded.",
        "com.opensymphony.",
        "org.hibernate.",
        "javax.servlet."
    };

    private List<String> packagesToFilter = new ArrayList<String>();
    private boolean shouldFilter = !Boolean.getBoolean(SYS_PROP_DISPLAY_FULL_STACKTRACE);
    private String cutOffPackage = null;

    public DefaultStackTraceFilterer() {
        this(true);
    }

    public DefaultStackTraceFilterer(boolean shouldFilter) {
        this.shouldFilter = shouldFilter;
        packagesToFilter.addAll(Arrays.asList(DEFAULT_INTERNAL_PACKAGES));
    }

    public void addInternalPackage(String name) {
        if (name == null) throw new IllegalArgumentException("Package name cannot be null");
        packagesToFilter.add(name);
    }

    public void setCutOffPackage(String cutOffPackage) {
        this.cutOffPackage = cutOffPackage;
    }

    public Throwable filter(Throwable source, boolean recursive) {
        if (recursive) {
            Throwable current = source;
            while (current.getCause() != null) {
                current = filter(current.getCause());
            }
        }
        return filter(source);
    }

    public Throwable filter(Throwable source) {
        if (shouldFilter) {
            StackTraceElement[] trace = source.getStackTrace();
            List<StackTraceElement> newTrace = new ArrayList<StackTraceElement>();
            for (StackTraceElement stackTraceElement : trace) {
                String className = stackTraceElement.getClassName();
                if (cutOffPackage != null && className.startsWith(cutOffPackage)) break;
                if (isApplicationClass(className)) {
                    if (stackTraceElement.getLineNumber() > -1) {
                        newTrace.add(stackTraceElement);
                    }
                }
            }

            // Only trim the trace if there was some application trace on the stack
            // if not we will just skip sanitizing and leave it as is
            if (newTrace.size() > 0) {
                // We don't want to lose anything, so log it
                STACK_LOG.error(FULL_STACK_TRACE_MESSAGE, source);
                StackTraceElement[] clean = new StackTraceElement[newTrace.size()];
                newTrace.toArray(clean);
                source.setStackTrace(clean);
            }
        }
        return source;
    }

    /**
     * Whether the given class name is an internal class and should be filtered
     * @param className The class name
     * @return True if is internal
     */
    protected boolean isApplicationClass(String className) {
        for (String packageName : packagesToFilter) {
            if (className.startsWith(packageName)) return false;
        }
        return true;
    }

    public void setShouldFilter(boolean shouldFilter) {
        this.shouldFilter = shouldFilter;
    }
}
