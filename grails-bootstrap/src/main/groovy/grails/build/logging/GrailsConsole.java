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
import jline.History;
import jline.Terminal;
import jline.UnsupportedTerminal;
import jline.WindowsTerminal;
import org.apache.tools.ant.BuildException;
import org.codehaus.groovy.grails.cli.ScriptExitException;
import org.codehaus.groovy.grails.cli.interactive.CandidateListCompletionHandler;
import org.codehaus.groovy.grails.cli.logging.GrailsConsoleErrorPrintStream;
import org.codehaus.groovy.grails.cli.logging.GrailsConsolePrintStream;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.codehaus.groovy.runtime.typehandling.NumberMath;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.util.StringUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Stack;

import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.Erase.FORWARD;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * Utility class for delivering console output in a nicely formatted way.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class GrailsConsole {

    private static GrailsConsole instance;
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String CATEGORY_SEPARATOR = "|";
    public static final String PROMPT = "grails> ";
    public static final String SPACE = " ";
    public static final String ERROR = "Error";
    public static final String WARNING = "Warning";
    public static final String HISTORYFILE = ".grails_history";
    public static final String STACKTRACE_FILTERED_MESSAGE = " (NOTE: Stack trace has been filtered. Use --verbose to see entire trace.)";
    public static final String STACKTRACE_MESSAGE = " (Use --stacktrace to see the full trace)";
    public static final Character SECURE_MASK_CHAR = new Character('*');
    private StringBuilder maxIndicatorString;
    private int cursorMove;

    /**
     * Whether to enable verbose mode
     */
    private boolean verbose;

    /**
     * Whether to show stack traces
     */
    private boolean stacktrace = Boolean.getBoolean("grails.show.stacktrace");

    private boolean progressIndicatorActive = false;

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

    History history;

    /**
     * The category of the current output
     */
    @SuppressWarnings("serial")
    Stack<String> category = new Stack<String>() {
        @Override
        public String toString() {
            if (size() == 1) return peek() + CATEGORY_SEPARATOR;
            return DefaultGroovyMethods.join(this, CATEGORY_SEPARATOR) + CATEGORY_SEPARATOR;
        }
    };

    /**
     * Whether ANSI should be enabled for output
     */
    private boolean ansiEnabled = true;

    /**
     * Whether user input is currently active
     */
    private boolean userInputActive;

    protected GrailsConsole() throws IOException {
        cursorMove = 1;
        out = new PrintStream(ansiWrap(System.out));

        System.setOut(new GrailsConsolePrintStream(out));
        System.setErr(new GrailsConsoleErrorPrintStream(ansiWrap(System.err)));

        reader = createConsoleReader();
        reader.setBellEnabled(false);
        reader.setCompletionHandler(new CandidateListCompletionHandler());

        terminal = createTerminal();

        history = prepareHistory();
        reader.setHistory(history);

        // bit of a WTF this, but see no other way to allow a customization indicator
        maxIndicatorString = new StringBuilder(indicator).append(indicator).append(indicator).append(indicator).append(indicator);

        out.println();
    }

    protected ConsoleReader createConsoleReader() throws IOException {
        return new ConsoleReader();
    }

    /**
     * Creates the instance of Terminal used directly in GrailsConsole. Note that there is also
     * another terminal instance created implicitly inside of ConsoleReader. That instance
     * is controlled by the jline.terminal system property.
     */
    protected Terminal createTerminal() {
        @SuppressWarnings("hiding") Terminal terminal;
        if (isWindows()) {
            terminal = new WindowsTerminal() {
                @Override
                public boolean isANSISupported() {
                    return true;
                }
            };
            try {
                terminal.initializeTerminal();
                terminal.enableEcho();
                fixCtrlC();
            } catch (Exception e) {
                terminal = new UnsupportedTerminal();
            }
        }
        else {
            terminal = Terminal.setupTerminal();
        }
        return terminal;
    }

    /**
     * Prepares a history file to be used by the ConsoleReader. This file
     * will live in the home directory of the user.
     */
    protected History prepareHistory() throws IOException {
        String historyFile = System.getProperty("user.home") + File.separator + HISTORYFILE;
        history = new History(new File(historyFile));
        return history;
    }

    /**
     * Hook method that allows controlling whether or not output streams should be wrapped by
     * AnsiConsole.wrapOutputStream. Unfortunately, Eclipse consoles will look to the AnsiWrap
     * like they do not understand ansi, even if we were to implement support in Eclipse to'
     * handle it and the wrapped stream will not pass the ansi chars on to Eclipse).
     */
    protected OutputStream ansiWrap(@SuppressWarnings("hiding") OutputStream out) {
        return AnsiConsole.wrapOutputStream(out);
    }

    // hack to workaround JLine bug - see https://issues.apache.org/jira/browse/GERONIMO-3978 for source of fix
    private void fixCtrlC() {
        try {
            Field f = ConsoleReader.class.getDeclaredField("keybindings");
            f.setAccessible(true);
            short[] keybindings = (short[])f.get(reader);
            if (keybindings[3] == -48) {
                keybindings[3] = 3;
            }
        }
        catch (Exception ignored) {
            // shouldn't happen
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().indexOf("windows") != -1;
    }

    public static synchronized GrailsConsole getInstance() {
        if (instance == null) {
            try {
                instance = createInstance();
            } catch (IOException e) {
                throw new RuntimeException("Cannot create grails console: " + e.getMessage(), e);
            }
        }

        if (!(System.out instanceof GrailsConsolePrintStream)) {
            System.setOut(new GrailsConsolePrintStream(instance.out));
        }
        return instance;
    }

    public static GrailsConsole createInstance() throws IOException {
        String className = System.getProperty("grails.console.class");
        if (className!=null) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends GrailsConsole> klass = (Class<? extends GrailsConsole>) Class.forName(className);
                return klass.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new GrailsConsole();
    }

    public void setAnsiEnabled(boolean ansiEnabled) {
        this.ansiEnabled = ansiEnabled;
    }

    /**
     * @param verbose Sets whether verbose output should be used
     */
    public void setVerbose(boolean verbose) {
        if (verbose) {
            // enable big traces in verbose mode
            // note - can't use StackTraceFilterer#SYS_PROP_DISPLAY_FULL_STACKTRACE as it is in grails-core
            System.setProperty("grails.full.stacktrace", "true");
        }
        this.verbose = verbose;
    }

    /**
     * @param stacktrace Sets whether to show stack traces on errors
     */
    public void setStacktrace(boolean stacktrace) {
        this.stacktrace = stacktrace;
    }

    /**
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
     * Indicates progress with the default progress indicator
     */
    public void indicateProgress() {
        progressIndicatorActive = true;
        if (isAnsiEnabled()) {
            if (StringUtils.hasText(lastMessage)) {
                if (!lastMessage.contains(maxIndicatorString)) {
                    updateStatus(lastMessage + indicator);
                }
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
        progressIndicatorActive = true;
        String currMsg = lastMessage;
        try {
            updateStatus(currMsg + ' '+ number + " of " + total);
        } finally {
            lastMessage = currMsg;
        }
    }

    /**
     * Indicates progress as a percentage for the given number and total
     *
     * @param number The number
     * @param total  The total
     */
    public void indicateProgressPercentage(long number, long total) {
        progressIndicatorActive = true;
        String currMsg = lastMessage;
        try {
            int percentage = Math.round(NumberMath.multiply(NumberMath.divide(number, total), 100).floatValue());

            if (!isAnsiEnabled()) {
                out.print("..");
                out.print(percentage + '%');
            }
            else {
                updateStatus(currMsg + ' ' + percentage + '%');
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
        progressIndicatorActive = true;
        String currMsg = lastMessage;
        try {
            if (isAnsiEnabled()) {
                updateStatus(currMsg + ' ' + number);
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
        if (msg == null || msg.trim().length() == 0) return;
        try {
            if (isAnsiEnabled()) {


                out.print(erasePreviousLine(CATEGORY_SEPARATOR));
                lastStatus = outputCategory(ansi(), CATEGORY_SEPARATOR)
                        .fg(Color.DEFAULT).a(msg).reset();
                out.println(lastStatus);
                if (!userInputActive) {
                    cursorMove = replaceCount;
                }
            } else {
                if (lastMessage != null && lastMessage.equals(msg)) return;

                if (progressIndicatorActive) {
                    out.println();
                }

                out.print(CATEGORY_SEPARATOR);
                out.println(msg);
            }
            lastMessage = msg;
        } finally {
            postPrintMessage();
        }
    }

    private Ansi moveDownToSkipPrompt() {
           return ansi()
                   .cursorDown(1)
                   .cursorLeft(PROMPT.length());
    }

    private void postPrintMessage() {
        progressIndicatorActive = false;
        if(userInputActive) {
            showPrompt();
        }
    }

    /**
     * Keeps doesn't replace the status message
     *
     * @param msg The message
     */
    public void addStatus(String msg) {
        outputMessage(msg, 0);
        lastMessage="";
    }

    /**
     * Prints an error message
     *
     * @param msg The error message
     */
    public void error(String msg) {
        error(ERROR, msg);
    }

    /**
     * Prints an error message
     *
     * @param msg The error message
     */
    public void warning(String msg) {
        error(WARNING, msg);
    }

    /**
     * Prints a warn message
     *
     * @param msg The message
     */
    public void warn(String msg) {
        warning(msg);
    }

    private void logSimpleError(String msg) {
        if (progressIndicatorActive) {
            out.println();
        }
        out.println(CATEGORY_SEPARATOR);
        out.println(msg);
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
        try {
            if ((verbose||stacktrace) && error != null) {
                printStackTrace(msg, error);
                error(ERROR, msg);
            }
            else {
                error(ERROR, msg + STACKTRACE_MESSAGE);
            }
        } finally {
            postPrintMessage();
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
        if (error instanceof ScriptExitException) {
            return; // don't bother with exit exceptions
        }

        if ((error instanceof BuildException) && error.getCause() != null) {
            error = error.getCause();
        }
        if (!isVerbose() && !Boolean.getBoolean("grails.full.stacktrace")) {
            StackTraceUtils.deepSanitize(error);
        }
        StringWriter sw = new StringWriter();
        PrintWriter ps = new PrintWriter(sw);
        message = message != null ? message : error.getMessage();
        if(!isVerbose()) {
            message = message + STACKTRACE_FILTERED_MESSAGE;
        }
        ps.println(message);
        error.printStackTrace(ps);
        error(sw.toString());
    }

    /**
     * Logs a message below the current status message
     *
     * @param msg The message to log
     */
    public void log(String msg) {
        try {
            if (msg.endsWith(LINE_SEPARATOR)) {
                out.print(msg);
            }
            else {
                out.println(msg);
            }
            cursorMove = 0;
        } finally {
            postPrintMessage();
        }
    }

    /**
     * Synonym for #log
     *
     * @param msg The message to log
     */
    public void info(String msg) {
        log(msg);
    }

    public void verbose(String msg) {
        try {
            if (verbose) {
                out.println(msg);
                cursorMove = 0;
            }
        } finally {
            postPrintMessage();
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
        return doUserInput(msg, false);
    }

    /**
     * Like {@link #userInput(String)} except that the user's entered characters will be replaced with ‘*’ on the CLI,
     * masking the input (i.e. suitable for capturing passwords etc.).
     *
     * @param msg The message/question to display.
     * @return The line of text entered by the user. May be a blank
     * string.
     */
    public String secureUserInput(String msg) {
        return doUserInput(msg, true);
    }

    private String doUserInput(String msg, boolean secure) {
        // Add a space to the end of the message if there isn't one already.
        if (!msg.endsWith(" ") && !msg.endsWith("\t")) {
            msg += ' ';
        }

        lastMessage = "";
        msg = isAnsiEnabled() ? outputCategory(ansi(), ">").fg(DEFAULT).a(msg).toString() : msg;
        try {
            return readLine(msg, secure);
        } finally {
            cursorMove = 0;
        }
    }

    /**
     * Shows the prompt to request user input
     * @param prompt The prompt to use
     * @return The user input prompt
     */
    private String showPrompt(String prompt) {
        cursorMove = 0;
        if (!userInputActive) {
            return readLine(prompt, false);
        }

        out.print(prompt);
        return null;
    }

    private String readLine(String prompt, boolean secure) {
        userInputActive = true;
        try {
            return secure ? reader.readLine(prompt, SECURE_MASK_CHAR) : reader.readLine(prompt);
        } catch (IOException e) {
            throw new RuntimeException("Error reading input: " + e.getMessage());
        }finally {
            userInputActive = false;
        }
    }

    /**
     * Shows the prompt to request user input
     * @return The user input prompt
     */
    public String showPrompt() {
        String prompt = isAnsiEnabled() ? ansiPrompt(PROMPT).toString() : PROMPT;
        return showPrompt(prompt);
    }

    private Ansi ansiPrompt(String prompt) {
        return ansi()
                .a(Ansi.Attribute.INTENSITY_BOLD)
                .fg(YELLOW)
                .a(prompt)
                .a(Ansi.Attribute.INTENSITY_BOLD_OFF)
                .fg(DEFAULT);
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
        }

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

    private String createQuestion(String message, String[] validResponses) {
        return message + "[" + DefaultGroovyMethods.join(validResponses, ",") + "] ";
    }

    private Ansi outputCategory(Ansi ansi, String categoryName) {
        return ansi
                .a(Ansi.Attribute.INTENSITY_BOLD)
                .fg(YELLOW)
                .a(categoryName)
                .a(SPACE)
                .a(Ansi.Attribute.INTENSITY_BOLD_OFF);
    }

    private Ansi outputErrorLabel(Ansi ansi, String label) {
        return ansi
                .a(Ansi.Attribute.INTENSITY_BOLD)
                .fg(RED)
                .a(CATEGORY_SEPARATOR)
                .a(SPACE)
                .a(label)
                .a(" ")
                .a(Ansi.Attribute.INTENSITY_BOLD_OFF)
                .fg(Color.DEFAULT);
    }

    private Ansi erasePreviousLine(String categoryName) {
        @SuppressWarnings("hiding") int cursorMove = this.cursorMove;
        if(userInputActive) cursorMove++;
        if (cursorMove > 0) {
            int moveLeftLength = categoryName.length() + lastMessage.length();
            if (userInputActive) {
                moveLeftLength += PROMPT.length();
            }
            return ansi()
                    .cursorUp(cursorMove)
                    .cursorLeft(moveLeftLength)
                    .eraseLine(FORWARD);

        }
        return ansi();
    }

    public void error(String label, String message) {
        if (message != null) {
            cursorMove = 0;
            try {
                if (isAnsiEnabled()) {
                    Ansi ansi = outputErrorLabel(userInputActive ? moveDownToSkipPrompt()  : ansi(), label).a(message);

                    if (message.endsWith(LINE_SEPARATOR)) {
                        out.print(ansi);
                    }
                    else {
                        out.println(ansi);
                    }
                }
                else {
                    out.print(label);
                    out.print(" ");
                    logSimpleError(message);
                }
            } finally {
                postPrintMessage();
            }
        }
    }
}
