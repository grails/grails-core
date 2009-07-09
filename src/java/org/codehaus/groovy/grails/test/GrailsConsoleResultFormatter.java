package org.codehaus.groovy.grails.test;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

import java.io.OutputStream;
import java.io.PrintStream;

public class GrailsConsoleResultFormatter implements JUnitResultFormatter {
    private PrintStream out;
    private int failureCount;

    public void startTestSuite(JUnitTest test) {
        out.print("Running test " + test.getName() + "...");
        failureCount = 0;
    }

    public void endTestSuite(JUnitTest test) {
        if (failureCount == 0) out.println("PASSED");
    }

    public void setOutput(OutputStream outputStream) {
        if (outputStream instanceof PrintStream) {
            this.out = (PrintStream) outputStream;
        }
        else {
            this.out = new PrintStream(outputStream);
        }
    }

    public void setSystemOutput(String out) {

    }

    public void setSystemError(String err) {

    }

    public void addError(Test test, Throwable throwable) {
        failureCount++;
        if (test instanceof TestCase) {
            printFailedTest((TestCase) test);
        }
    }

    public void addFailure(Test test, AssertionFailedError assertionFailedError) {
        failureCount++;
        if (test instanceof TestCase) {
            printFailedTest((TestCase) test);
        }
    }

    public void endTest(Test test) {
    }

    public void startTest(Test test) {
    }

    private void printFailedTest(TestCase test) {
        if (failureCount == 1) out.println();
        out.println("                    " + test.getName() + "...FAILED");
    }
}
