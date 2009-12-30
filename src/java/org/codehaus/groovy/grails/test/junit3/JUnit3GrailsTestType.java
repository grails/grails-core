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

package org.codehaus.groovy.grails.test.junit3;

import org.codehaus.groovy.grails.test.GrailsTestTypeResult;
import org.codehaus.groovy.grails.test.GrailsTestTargetPattern;
import org.codehaus.groovy.grails.test.support.GrailsTestTypeSupport;

import junit.framework.TestSuite;
import junit.framework.TestCase;

import org.codehaus.groovy.grails.test.event.GrailsTestEventPublisher;

import org.codehaus.groovy.grails.test.report.junit.JUnitReportsFactory;

import java.lang.reflect.Modifier;
import java.io.IOException;
import java.io.File;

import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.LinkedList;

import groovy.lang.Binding;

/**
 * An {@code GrailsTestType} for JUnit3 tests.
 */
public class JUnit3GrailsTestType extends GrailsTestTypeSupport {

    public static final String TESTS_SUFFIX = "Tests";
     
    protected TestSuite wholeTestSuite;
    protected JUnit3GrailsTestTypeMode mode;
    
    public JUnit3GrailsTestType(String name, String sourceDirectory) {
        this(name, sourceDirectory, null);
    }
    
    public JUnit3GrailsTestType(String name, String sourceDirectory, JUnit3GrailsTestTypeMode mode) {
        super(name, sourceDirectory);
        this.mode = mode;
    }

    protected List<String> getTestSuffixes() { 
        List<String> testSuffixes = new LinkedList<String>();
        testSuffixes.add(TESTS_SUFFIX);
        return testSuffixes;
    }

    protected int doPrepare() {
        wholeTestSuite = createWholeTestSuite();
        return wholeTestSuite.testCount();
    }
    
    protected TestSuite createWholeTestSuite() {
        TestSuite theWholeTestSuite = new TestSuite("Grails Test Suite");
        
        for (GrailsTestTargetPattern targetPattern : this.getTestTargetPatterns()) {
            for (File sourceFile : findSourceFiles(targetPattern)) {
                
                String className = sourceFileToClassName(sourceFile);
                Class clazz = sourceFileToClass(sourceFile);

                if (TestCase.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                    TestSuite testSuite = null;

                    String targetMethodName = targetPattern.getMethodName();
                    if (targetMethodName != null) {
                        try {
                            clazz.getMethod(targetMethodName); // make sure that the method exists
                            testSuite = createTestSuite();
                            testSuite.setName(className);
                            testSuite.addTest(TestSuite.createTest(clazz, targetMethodName));
                        } catch (NoSuchMethodException e) {}
                    } else {
                        testSuite = createTestSuite(clazz);
                    }

                    if (testSuite != null) {
                        theWholeTestSuite.addTest(testSuite);
                    }
                }
            }
        }
        
        return theWholeTestSuite;
    }   

    protected ApplicationContext getApplicationContext() {
        Binding buildBinding = getBuildBinding();
        if (buildBinding.getVariables().containsKey("appCtx")) {
            return (ApplicationContext)buildBinding.getProperty("appCtx");
        } else {
            throw new IllegalStateException("ApplicationContext requested, but is not present in the build binding");
        }
    }
    
    protected TestSuite createTestSuite(Class clazz) {
        if (mode == null) {
            return new TestSuite(clazz);
        } else {
            return new JUnit3GrailsEnvironmentTestSuite(clazz, getApplicationContext(), mode);
        }
    }

    protected TestSuite createTestSuite() {
        if (mode == null) {
            return new TestSuite();
        } else {
            return new JUnit3GrailsEnvironmentTestSuite(getApplicationContext(), mode);
        }
    }
    
    protected JUnit3GrailsTestTypeRunner createRunner(GrailsTestEventPublisher eventPublisher) {
        return new JUnit3GrailsTestTypeRunner(JUnitReportsFactory.createFromBuildBinding(getBuildBinding()), eventPublisher, createSystemOutAndErrSwapper());
    }

    protected GrailsTestTypeResult doRun(GrailsTestEventPublisher eventPublisher) {
        return new JUnit3GrailsTestTypeResult(createRunner(eventPublisher).runTests(wholeTestSuite));
    }

}