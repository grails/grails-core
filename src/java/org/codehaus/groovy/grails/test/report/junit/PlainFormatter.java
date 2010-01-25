package org.codehaus.groovy.grails.test.report.junit;

import java.io.*;
import junit.framework.*;
import org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.codehaus.groovy.grails.test.support.TestStacktraceSanitizer;

/**
 * JUnit plain text formatter that sanitises the stack traces generated
 * by tests.
 */
public class PlainFormatter extends PlainJUnitResultFormatter {
    
    protected String name;
    protected File file;
    
    protected String systemOutput;
    protected String systemError;
    
    public PlainFormatter(String name, File file) {
        this.name = name;
        this.file = file;
        try {
            super.setOutput(new BufferedOutputStream(new FileOutputStream(file)));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void setOutput(OutputStream out) {
        throw new IllegalStateException("This should not be called");
    }
    
    public void setSystemError(String out) {
        systemError = out;
        super.setSystemError(out);
    }

    public void setSystemOutput(String out) {
        systemOutput = out;
        super.setSystemOutput(out);
    }

    public void addFailure(Test test, Throwable throwable) {
        TestStacktraceSanitizer.sanitize(throwable);
        super.addFailure(test, throwable);
    }

    public void addError(Test test, Throwable throwable) {
        TestStacktraceSanitizer.sanitize(throwable);
        super.addError(test, throwable);
    }
    
    public void endTestSuite(JUnitTest suite) {
        super.endTestSuite(suite);
        File parentFile = file.getParentFile();
        writeToFile(new File(parentFile, name + "-out.txt"), systemOutput);
        writeToFile(new File(parentFile, name + "-err.txt"), systemError);
    }
    
    protected void writeToFile(File file, String text) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(text);
            writer.close();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        finally {
            if (writer != null) {
                try { writer.close(); } catch (IOException ex) {}
            }
        }
    }
    
}