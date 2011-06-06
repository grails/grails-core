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

package org.codehaus.groovy.grails.cli.logging;

import jline.ConsoleReader;
import jline.Terminal;
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

    private GrailsConsole() throws IOException {
        this.cursorMove = 1;
        this.out = new PrintStream(AnsiConsole.wrapOutputStream(System.out));

        System.setOut(new GrailsJConsolePrintStream(this.out));

        terminal = Terminal.setupTerminal();
        reader = new ConsoleReader();
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

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public InputStream getInput() {
        return reader.getInput();
    }

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
    void indicateProgress() {
        if (StringUtils.hasText(lastMessage)) {
            if (!lastMessage.contains(maxIndicatorString))
                updateStatus(lastMessage + indicator);
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
            String message = new StringBuilder(currMsg).append(' ').append(percentage).append('%').toString();
            updateStatus(message);
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
            updateStatus(new StringBuilder(currMsg).append(' ').append(number).toString());
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
            if (Ansi.isEnabled()) {

                lastStatus = outputCategory(erasePreviousLine(categoryName), categoryName)
                        .fg(Color.DEFAULT).a(msg).reset();
                out.println(lastStatus);
                cursorMove = replaceCount;
            } else {
                out.print(categoryName + msg);
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

    /**
     * Prints an error message
     *
     * @param msg The error message
     */
    public void error(String msg) {
        cursorMove = 0;
        if (hasNewLines(msg)) {
            if (Ansi.isEnabled()) {
                out.println(ansi().a(Ansi.Attribute.INTENSITY_BOLD).fg(RED).a(category.toString()).reset());
                out.println();
                out.println(ansi().fg(Color.DEFAULT).a(msg).reset());
                out.println();
            } else {
                out.println(category);
                out.println();
                out.println(msg);
                out.println();
            }
            //updateStatus();
        } else {
            if (Ansi.isEnabled()) {
                out.println(ansi().a(Ansi.Attribute.INTENSITY_BOLD).fg(RED).a(category.toString()).fg(Color.DEFAULT).a(msg).reset());
            } else {
                out.print(category.toString());
                out.println(msg);
            }
        }

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
           StringWriter sw = new StringWriter();
           PrintWriter ps = new PrintWriter(sw);
           ps.println(msg);
           error.printStackTrace(ps);
           error(sw.toString());
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
       StringWriter sw = new StringWriter();
       PrintWriter ps = new PrintWriter(sw);
       ps.println(error.getMessage());
       error.printStackTrace(ps);
       error(sw.toString());
    }


    public void log(String msg) {
         if (hasNewLines(msg)) {
            out.println(msg);
            out.println();
            cursorMove = 0;
            //updateStatus();
        } else {
            out.println(msg);
            out.println();
            cursorMove = 0;
        }
    }

    public void verbose(String msg) {
        if(verbose) {
            if (hasNewLines(msg)) {
                out.println(msg);
                out.println();
                cursorMove = 0;
                //updateStatus();
            } else {
                out.println(msg);
                out.println();
                cursorMove = 0;
            }
        }
    }

    private void printMessageOnNewLine(String msg) {
        out.println(outputCategory(ansi(), category.toString())
                .newline()
                .fg(DEFAULT).a(msg).reset());
    }

    /**
     * Replays the last status message
     */
    public void echoStatus() {
        if (lastStatus != null) {
            updateStatus(lastStatus.toString());
        }
    }

    private boolean hasNewLines(String msg) {
        return msg.contains(LINE_SEPARATOR);
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
        updateStatus(msg);
        try {
            cursorMove += 1;
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
            cursorMove += 2;
            return userInput("Invalid input. Must be one of ", validResponses);
        }

    }

    private String createQuestion(String message, String[] validResponses) {
        return new StringBuilder(message).append("[").append(DefaultGroovyMethods.join(validResponses, ",")).append("] ").toString();
    }
}