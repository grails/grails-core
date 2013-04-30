/*
 * Copyright 2009 the original author or authors.
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
package org.codehaus.groovy.grails.test

import groovy.transform.CompileStatic
import org.springframework.util.AntPathMatcher
import java.util.regex.Pattern

/**
 * A pattern passed to `grails test-app` that targets one or more tests and potentially a single method
 *
 * Examples:
 * <li>SomeController // filePattern: {@code &#042;&#042;&#047;SomeController}, classPattern: {@code SomeController}, methodName: {@code null}
 * <li>*Controller* // filePattern: {@code &#042;&#042;&#047;*Controller*}, classPattern: {@code *Controller*}, methodName: {@code null}
 * <li>SomeController.testSomeAction // filePattern: {@code &#042;&#042;&#047;SomeController}, classPattern: {@code SomeController}, methodName: {@code testSomeAction}
 * <li>org.SomeController // filePattern: {@code org/SomeController}, classPattern: {@code org.SomeController}, methodName: {@code null}
 *
 * Note: the interpretation of a target pattern is largely the responsibility of a test type.
 */
@CompileStatic
class GrailsTestTargetPattern {

    /**
     * The pattern, unchanged
     */
    final String rawPattern

    /**
     * The target pattern as it applies to the file system
     * (i.e. suitable for use with a PathMatchingResourcePatternResolver)
     */
    final String filePattern

    /**
     * The target pattern as it applies to class names, without a methodName component
     */
    final String classPattern

    /**
     * The method name component if it is present
     */
    final String methodName

    GrailsTestTargetPattern(String pattern) {
        rawPattern = pattern

        if (containsMethodName(rawPattern)) {
            int pos = rawPattern.lastIndexOf('.')
            methodName = rawPattern.substring(pos + 1)
            classPattern = rawPattern.substring(0, pos)
        }
        else {
            classPattern = rawPattern
            methodName = null
        }

        filePattern = classPatternToFilePattern(classPattern)
    }

    String toString() {
        "[raw: $rawPattern, filePattern: $filePattern, classPattern: $classPattern, methodName: $methodName]"
    }

    boolean matches(String className, String methodName, String[] suffixes) {
        matchesClass(className, suffixes) && matchesMethod(methodName)
    }

    boolean matchesClass(String className, String[] suffixes) {
        matchesClassWithExtension(className) || matchesClassWithoutExtension(className, suffixes)
    }

    boolean matchesMethod(String methodName) {
        this.methodTargeting && this.methodName == methodName
    }

    boolean isMethodTargeting() {
        this.methodName != null
    }

    protected boolean matchesClassWithExtension(String className) {
        new AntPathMatcher().match(filePattern, className.replace('.', '/'))
    }

    protected boolean matchesClassWithoutExtension(String className, String[] suffixes) {
        if (suffixes) {
            def suffixesAsPattern = suffixes.collect { String it -> Pattern.quote(it) }.join('|')
            className = className.replaceAll("(${suffixesAsPattern})\$", "")
        }
        def classNameAsPath = className.replace('.', '/')
        new AntPathMatcher().match(filePattern, classNameAsPath)
    }

    protected boolean containsMethodName(String pattern) {
        pattern.contains('.') && Character.isLowerCase(pattern.charAt(pattern.lastIndexOf('.') + 1))
    }

    protected classPatternToFilePattern(String pattern) {
        (pattern.indexOf('.') != -1) ? pattern.replace('.', '/') : "**/" + pattern
    }
}