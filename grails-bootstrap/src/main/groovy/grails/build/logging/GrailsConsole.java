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

package grails.build.logging;

import jline.ConsoleReader;
import jline.Terminal;
import org.codehaus.groovy.grails.cli.interactive.CandidateListCompletionHandler;
import org.codehaus.groovy.grails.cli.logging.GrailsConsolePrintStream;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.codehaus.groovy.runtime.typehandling.NumberMath;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.Stack;

import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.Erase.FORWARD;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * Utility class for delivering console output in a nicely formatted way
 *
 * @author Graeme Rocher
 * @since 1.4
 */

public class GrailsConsole {

    private static GrailsConsole instance;
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String CATEGORY_SEPARATOR = " > ";
    private StringBuilder maxIndicatorString;
    private int cursorMove;

    /**
     * Whether to enable verbose mode
     */
    private boolean verbose;

    /**
     * The progress indicator to use
     */
    String indicator = ".";
    /**
     * The last message that was printed
     */
    String lastMessage = "";

    Ansi lastStatus = null;
    /**
     * The reader to read info from the console
     */
    ConsoleReader reader;

    Terminal terminal;

    PrintStream out;

    /**
     * The category of the current output
     */
    Stack<String> category = new Stack<String>() {
        public String toString() {
            if (size() == 1) return peek() + CATEGORY_SEPARATOR;
            else {
                return DefaultGroovyMethods.join(this, CATEGORY_SEPARATOR) + CATEGORY_SEPARATOR;
            }
        }
    };

    /**
     * Whether ANSI should be enabled for output
     */
    private boolean ansiEnabled = true;

    protected GrailsConsole() throws IOException {
        this.cursorMove = 1;
        this.out = new PrintStream(AnsiConsole.wrapOutputStream(System.out));

        System.setOut(new GrailsConsolePrintStream(this.out));

        terminal = Terminal.setupTerminal();
        reader = new ConsoleReader();
        reader.setCompletionHandler(new CandidateListCompletionHandler());
        category.add("grails");
        // bit of a WTF this, but see no other way to allow a customization indicator
        this.maxIndicatorString = new StringBuilder().append(indicator).append(indicator).append(indicator).append(indicator).append(indicator);

        out.println();
    }

    public static synchronized GrailsConsole getInstance() {
        if (instance == null) {
            try {
                instance = new GrailsConsole();
            } catch (IOException e) {
                throw new RuntimeException("Cannot create grails console: " + e.getMessage(), e);
            }
        }
        return instance;
    }

    public void setAnsiEnabled(boolean ansiEnabled) {
        this.ansiEnabled = ansiEnabled;
    }

    /**
     *
     * @param verbose Sets whether verbose output should be used
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     *
     * @return Whether verbose output is being used
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * @return The input stream being read from
     */
    public InputStream getInput() {
        return reader.getInput();
    }

    /**
     *
     * @return The last message logged
     */
    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public ConsoleReader getReader() {
        return reader;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public PrintStream getOut() {
        return out;
    }

    public Stack<String> getCategory() {
        return category;
    }

    /**
     * Indicates progresss with the default progress indicator
     */
    public void indicateProgress() {
        if(isAnsiEnabled()) {
            if (StringUtils.hasText(lastMessage)) {
                if (!lastMessage.contains(maxIndicatorString))
                    updateStatus(lastMessage + indicator);
            }

        }
        else {
            out.print(indicator);
        }
    }

    /**
     * Indicate progress for a number and total
     *
     * @param number The current number
     * @param total  The total number
     */
    public void indicateProgress(int number, int total) {
        String currMsg = lastMessage;
        try {
            updateStatus(new StringBuilder(currMsg).append(' ').append(number).append(" of ").append(total).toString());
        } finally {
            lastMessage = currMsg;
        }

    }

    /**
     * Indicates progress as a precentage for the given number and total
     *
     * @param number The number
     * @param total  The total
     */
    public void indicateProgressPercentage(long number, long total) {
        String currMsg = lastMessage;
        try {
            int percentage = Math.round(NumberMath.multiply(NumberMath.divide(number, total), 100).floatValue());

            if(!isAnsiEnabled()) {
                out.print("..");
                out.print(percentage+'%');
            }
            else {
                String message = new StringBuilder(currMsg).append(' ').append(percentage).append('%').toString();
                updateStatus(message);
            }
        } finally {
            lastMessage = currMsg;
        }
    }

    /**
     * Indicates progress by number
     *
     * @param number The number
     */
    public void indicateProgress(int number) {
        String currMsg = lastMessage;
        try {
            if(isAnsiEnabled()) {
                updateStatus(new StringBuilder(currMsg).append(' ').append(number).toString());
            }
            else {
                out.print("..");
                out.print(number);
            }
        } finally {
            lastMessage = currMsg;
        }

    }

    /**
     * Updates the current state message
     *
     * @param msg The message
     */
    public void updateStatus(String msg) {
        outputMessage(msg, 1);
    }

    private void outputMessage(String msg, int replaceCount) {
        if (hasNewLines(msg)) {
            printMessageOnNewLine(msg);
            lastMessage = "";
        } else {
            final String categoryName = category.toString();
            if (isAnsiEnabled()) {

                lastStatus = outputCategory(erasePreviousLine(categoryName), categoryName)
                        .fg(Color.DEFAULT).a(msg).reset();
                out.println(lastStatus);
                cursorMove = replaceCount;
            } else {
                out.println(categoryName + msg);
            }
            lastMessage = msg;
        }
    }

    /**
     * Keeps doesn't replace the status message
     *
     * @param msg The message
     */
    public void addStatus(String msg) {
        outputMessage(msg, 0);
    }



    /**
     * Prints an error message
     *
     * @param msg The error message
     */
    public void error(String msg) {
        cursorMove = 0;
        if (hasNewLines(msg)) {
            if (isAnsiEnabled()) {
                out.println(ansi().a(Ansi.Attribute.INTENSITY_BOLD).fg(RED).a(category.toString()).reset());
                out.println(ansi().fg(Color.DEFAULT).a(msg).reset());
            } else {
                out.println(category);
                out.println(msg);
            }
            //updateStatus();
        } else {
            if (isAnsiEnabled()) {
                out.println(ansi().a(Ansi.Attribute.INTENSITY_BOLD).fg(RED).a(category.toString()).fg(Color.DEFAULT).a(msg).reset());
            } else {
                out.print(category.toString());
                out.println(msg);
            }
        }

    }

    public boolean isAnsiEnabled() {
        return Ansi.isEnabled() && terminal.isANSISupported() && ansiEnabled;
    }

    /**
     * Use to log an error
     *
     * @param msg The message
     * @param error The error
     */
    public void error(String msg, Throwable error) {
       if(verbose && error != null) {
           StackTraceUtils.deepSanitize(error);
           printStackTrace(msg, error);
       }
       else {
           error(msg);
       }
    }

    /**
     * Use to log an error
     *
     * @param error The error
     */
    public void error(Throwable error) {
        printStackTrace(null, error);
    }

    private void printStackTrace(String message, Throwable error) {
        StringWriter sw = new StringWriter();
        PrintWriter ps = new PrintWriter(sw);
        if(message != null) {
            ps.println(message);
        }
        else {
            ps.println(error.getMessage());
        }
        error.printStackTrace(ps);
        error(sw.toString());
    }


    /**
     * Logs a message below the current status message
     *
     * @param msg The message to log
     */
    public void log(String msg) {
         if (hasNewLines(msg)) {
            out.println(msg);
            cursorMove = 0;
            //updateStatus();
        } else {
            out.println(msg);
            cursorMove = 0;
        }
    }

    public void verbose(String msg) {
        if(verbose) {
            if (hasNewLines(msg)) {
                out.println(msg);
                cursorMove = 0;
                //updateStatus();
            } else {
                out.println(msg);
                cursorMove = 0;
            }
        }
    }


    /**
     * Replays the last status message
     */
    public void echoStatus() {
        if (lastStatus != null) {
            updateStatus(lastStatus.toString());
        }
    }


    /**
     * Replacement for AntBuilder.input() to eliminate dependency of
     * GrailsScriptRunner on the Ant libraries. Prints a message and
     * returns whatever the user enters (once they press &lt;return&gt;).
     * @param msg The message/question to display.
     * @return The line of text entered by the user. May be a blank
     * string.
     */
    public String userInput(String msg) {
        addStatus(msg);
        lastMessage = "";
        try {
            cursorMove = 0;
            return reader.readLine("> ");
        } catch (IOException e) {
            throw new RuntimeException("Error reading input: " + e.getMessage());
        }
    }

    /**
     * Replacement for AntBuilder.input() to eliminate dependency of
     * GrailsScriptRunner on the Ant libraries. Prints a message and
     * list of valid responses, then returns whatever the user enters
     * (once they press &lt;return&gt;). If the user enters something
     * that is not in the array of valid responses, the message is
     * displayed again and the method waits for more input. It will
     * display the message a maximum of three times before it gives up
     * and returns <code>null</code>.
     * @param message The message/question to display.
     * @param validResponses An array of responses that the user is
     * allowed to enter. Displayed after the message.
     * @return The line of text entered by the user, or <code>null</code>
     * if the user never entered a valid string.
     */
    public String userInput(String message, String[] validResponses) {
        if (validResponses == null) {
            return userInput(message);
        } else {
            String question = createQuestion(message, validResponses);
            String response = userInput(question);
            for (String validResponse : validResponses) {
                if (response != null && response.equalsIgnoreCase(validResponse)) {
                    return response;
                }
            }
            cursorMove = 0;
            return userInput("Invalid input. Must be one of ", validResponses);
        }

    }

    private String createQuestion(String message, String[] validResponses) {
        return new StringBuilder(message).append("[").append(DefaultGroovyMethods.join(validResponses, ",")).append("] ").toString();
    }


    private void printMessageOnNewLine(String msg) {
        out.println(outputCategory(ansi(), category.toString())
                .newline()
                .fg(DEFAULT).a(msg).reset());
    }

    private boolean hasNewLines(String msg) {
        return msg.contains(LINE_SEPARATOR);
    }

    private Ansi outputCategory(Ansi ansi, String categoryName) {
        return ansi
                .a(Ansi.Attribute.INTENSITY_BOLD)
                .fg(YELLOW)
                .a(categoryName)
                .a(Ansi.Attribute.INTENSITY_BOLD_OFF);
    }

    private Ansi erasePreviousLine(String categoryName) {
        if(cursorMove > 0) {
            return ansi()
                    .cursorUp(cursorMove)
                    .cursorLeft(categoryName.length() + lastMessage.length())
                    .eraseLine(FORWARD);

        }
        return ansi();
    }
}