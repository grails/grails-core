package org.codehaus.groovy.grails.test;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

import java.io.*;

/**
 *
 */
public class FormattedOutput {
    private File file;
    private JUnitResultFormatter formatter;
    private OutputStream output;

    public FormattedOutput(File file, JUnitResultFormatter formatter) {
        this.file = file;
        this.formatter = formatter;
    }

    public File getFile() {
        return file;
    }

    public JUnitResultFormatter getFormatter() {
        return formatter;
    }

    public void start(JUnitTest test) {
        try {
            output = new BufferedOutputStream(new FileOutputStream(file));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        formatter.setOutput(output);
        formatter.startTestSuite(test);
    }

    public void end(JUnitTest test, String out, String err) {
        // Blatant hack for "plain" formatter.
        if (file.getName().endsWith(".txt")) {
            String baseName = file.getName().substring(0, file.getName().length() - 4);
            writeToFile(new File(file.getParentFile(), baseName + "-out.txt"), out);
            writeToFile(new File(file.getParentFile(), baseName + "-err.txt"), err);
        }

        formatter.setSystemOutput(out);
        formatter.setSystemError(err);
        formatter.endTestSuite(test);

        if (output != null) {
            try { output.close(); } catch (IOException ex) {}
        }
    }

    /**
     * Writes a string of text to a file, creating the file if it doesn't
     * exist or overwriting the existing contents if it does.
     * @param file The file to write to.
     * @param text The text to write into the file.
     */
    private void writeToFile(File file, String text) {
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
