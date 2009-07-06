package org.codehaus.groovy.grails.test;

import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * <p>Runs a JUnit test suite, printing the results to the console and
 * also generating reports in selected formats.</p>
 * <p><b>Note</b> This class is currently written in Java because the
 * Groovy compiler can't cope with "ant-junit.jar" in ANT_HOME and
 * "junit.jar" in the "lib" directory.</p>
 *
 * @author Peter Ledbrook
 */
public class DefaultGrailsTestRunner implements GrailsTestRunner {
    private PrintStream savedOut;
    private PrintStream savedErr;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;
    private List<FormattedOutput> formattedOutputs;

    private File reportsDir;
    private List<String> formats;

    public DefaultGrailsTestRunner(File reportsDir, List<String> formats) {
        this.reportsDir = reportsDir;

        // Defensive copy.
        this.formats = new ArrayList<String>(formats);
    }

    public TestResult runTests(TestSuite suite) {
        GrailsConsoleResultFormatter consoleFormatter = new GrailsConsoleResultFormatter();
        TestResult result = new TestResult();
        result.addListener(consoleFormatter);

        for (Enumeration tests = suite.tests(); tests.hasMoreElements();) {
            TestSuite test = (TestSuite) tests.nextElement();
            reset();

            JUnitTest junitTest = new JUnitTest(test.getName());
            try {
                replaceStandardStreams();
                prepareReports(test);

                consoleFormatter.setOutput(savedOut);
                consoleFormatter.startTestSuite(junitTest);
                for (FormattedOutput output : formattedOutputs) {
                    result.addListener(output.getFormatter());
                    output.start(junitTest);
                }

                // Starting...now!
                long start = System.currentTimeMillis();
                int runCount = result.runCount();
                int failureCount = result.failureCount();
                int errorCount = result.errorCount();

                for (int i = 0; i < test.testCount(); i++) {
                    TestCase t = (TestCase) test.testAt(i);
                    System.out.println("--Output from " + t.getName() + "--");
                    System.err.println("--Output from " + t.getName() + "--");

                    test.runTest(t, result);
                }

                junitTest.setCounts(
                        result.runCount() - runCount,
                        result.failureCount() - failureCount,
                        result.errorCount() - errorCount);
                junitTest.setRunTime(System.currentTimeMillis() - start);
            }
            finally {
                for (FormattedOutput output : formattedOutputs) {
                    output.end(junitTest, out.toString(), err.toString());
                }
                consoleFormatter.endTestSuite(junitTest);
                restoreStandardStreams();
            }
        }

        return result;
    }

    public void reset() {
        this.formattedOutputs = null;
        this.savedOut = null;
        this.savedErr = null;
        this.out = null;
        this.err = null;
    }

    public void prepareReports(TestSuite test) {
        formattedOutputs = new ArrayList<FormattedOutput>(formats.size());
        for (String format : formats) {
            formattedOutputs.add(createFormatter(format, test));
        }
    }

    public FormattedOutput createFormatter(String type, TestSuite test) {
        if (type.equals("xml")) {
            return new FormattedOutput(
                    new File(reportsDir, "TEST-" + test.getName() + ".xml"),
                    new XMLFormatter());
        }
        else if (type.equals("plain")) {
            return new FormattedOutput(
                    new File(reportsDir, "plain/TEST-" + test.getName() + ".txt"),
                    new PlainFormatter());
        }
        else {
            throw new RuntimeException("Unknown formatter type: $type");
        }
    }

    private void replaceStandardStreams() {
        this.savedOut = System.out;
        this.savedErr = System.err;

        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();

        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
    }

    private void restoreStandardStreams() {
        if (this.savedOut != null) System.setOut(this.savedOut);
        if (this.savedErr != null) System.setErr(this.savedErr);
    }
}
