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

import java.io.OutputStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;

import org.codehaus.groovy.grails.test.event.GrailsTestEventPublisher;

public class JUnit3ListenerEventPublisherAdapter implements JUnitResultFormatter {

    protected GrailsTestEventPublisher eventPublisher;
    protected String out;
    protected String err;
    
    public JUnit3ListenerEventPublisherAdapter(GrailsTestEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    public void setOutput(OutputStream outputStream) {}
    
    public void setSystemOutput(String out) {}
    
    public void setSystemError(String err) {}

    public void startTestSuite(JUnitTest test) {
        eventPublisher.testCaseStart(test.getName());
    }

    public void startTest(Test test) {
        eventPublisher.testStart(((TestCase)test).getName());
    }

    public void addError(Test test, Throwable throwable) {
        eventPublisher.testFailure(((TestCase)test).getName(), throwable, true);
    }

    public void addFailure(Test test, AssertionFailedError assertionFailedError) {
        eventPublisher.testFailure(((TestCase)test).getName(), assertionFailedError);
    }

    public void endTest(Test test) {
        eventPublisher.testEnd(((TestCase)test).getName());
    }

    public void endTestSuite(JUnitTest test, String out, String err) {
        eventPublisher.testCaseEnd(test.getName(), out, err);
        out = null;
        err = null;
    }
    
    public void endTestSuite(JUnitTest test) {
        throw new IllegalStateException("should not be called, use endTestSuite(JUnitTest, String, String) instead");
    }

}