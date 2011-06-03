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
import org.codehaus.groovy.runtime.typehandling.NumberMath;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Stack;

import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.Erase.FORWARD;
import static org.fusesource.jansi.Ansi.ansi;

 /**
 * Utility class for delivery console output
 *
 * @author Graeme Rocher
 * @since 1.4
 */

public class GrailsConsole {

    private static GrailsConsole instance;
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
     public static final String CATEGORY_SEPARATOR = " > ";
     private StringBuilder maxIndicatorString;

     public static GrailsConsole getInstance() {
        if(instance == null) {
            try {
                instance = new GrailsConsole();
            } catch (IOException e) {
                throw new RuntimeException("Cannot create grails console: " + e.getMessage(), e);
            }
        }
        return instance;
    }

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
			if(size() == 1) return peek() + CATEGORY_SEPARATOR;
			else {
				return DefaultGroovyMethods.join(this, CATEGORY_SEPARATOR) + CATEGORY_SEPARATOR;
			}
		}
	};

	private GrailsConsole() throws IOException {

        this.out = new PrintStream(AnsiConsole.wrapOutputStream(System.out));

        System.setOut(new GrailsJConsolePrintStream(this.out));

        terminal = Terminal.setupTerminal();
        reader = new ConsoleReader();
        category.add("grails");
        // bit of a WTF this, but see no other way to allow a customization indicator
        this.maxIndicatorString = new StringBuilder().append(indicator).append(indicator).append(indicator).append(indicator).append(indicator);

		out.println();
	}

     public InputStream getInput() {
            return reader.getInput();
     }

     public String getLastMessage() {
         return lastMessage;
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
        if(StringUtils.hasText(lastMessage)) {
            if(!lastMessage.contains(maxIndicatorString))
                updateStatus(lastMessage + indicator);
        }
	}

    /**
     * Indicate progress for a number and total
     *
     * @param number The current number
     * @param total The total number
     */
	public void indicateProgress(int number, int total) {
		String currMsg = lastMessage;
		try {
			updateStatus(new StringBuilder(currMsg).append(' ').append(number).append(" of ").append(total).toString());
		}
		finally {
			lastMessage = currMsg;
		}

	}
    /**
     * Indicates progress as a precentage for the given number and total
     *
     * @param number The number
     * @param total The total
     */
	public void indicateProgressPercentage(long number, long total) {
		String currMsg = lastMessage;
		try {
            int percentage = Math.round(NumberMath.multiply(NumberMath.divide(number, total), 100).floatValue());
            String message = new StringBuilder(currMsg).append(' ').append(percentage).append('%').toString();
            updateStatus(message);
		}
		finally {
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
		}
		finally {
			lastMessage = currMsg;
		}

	}
    /**
     * Updates the current state message
     *
     * @param msg The message
     */
	public void updateStatus(String msg) {
        if(hasNewLines(msg)) {
            printMessageOnNewLine(msg);
            lastMessage = "";
        }
        else {
            final String categoryName = category.toString();
            lastStatus = ansi().cursorUp(1).cursorLeft(categoryName.length() + lastMessage.length()).eraseLine(FORWARD).fg(YELLOW).a(categoryName).fg(Color.DEFAULT).a(msg).reset();
            out.println(lastStatus);
            lastMessage = msg;
        }
	}

    /**
     * Prints an error message
     *
     * @param msg The error message
     */
    public void error(String msg) {
        if(hasNewLines(msg)) {
            out.println( ansi().fg(RED).a(category.toString()).reset());
            out.println();
            out.println(ansi().fg(Color.DEFAULT).a(msg).reset());
            out.println();
            //updateStatus();
        }
        else {
            out.println( ansi().fg(RED).a(category.toString()).fg(Color.DEFAULT).a(msg).reset() );
        }

	}

    public void log(String msg) {
        if(hasNewLines(msg)) {
            out.println(msg);
            //updateStatus();
        }
        else {
            out.println(msg);
            //updateStatus();
        }
    }

     private void printMessageOnNewLine(String msg) {
         out.println(ansi().fg(YELLOW).a(category.toString()).newline().fg(DEFAULT).a(msg).reset());
     }

     public void echoStatus() {
         if(lastStatus != null) {
             updateStatus(lastStatus.toString());
         }
     }

     boolean hasNewLines(String msg) {
        return msg.contains(LINE_SEPARATOR);
    }

    /**
     * Prompts for user input
     * @param msg The message for the prompt
     * @return The input value
     */
	public String userInput(String msg) throws IOException {
		updateStatus(msg);
		return reader.readLine("> ");
	}
}