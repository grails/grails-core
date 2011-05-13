package org.codehaus.groovy.grails.cli;

import java.io.PrintStream;

class DefaultScriptUserInterface implements UserInterface {
    PrintStream consoleOut;
    boolean lastWasPrintln = true;
    boolean inStatusRun = false;
    String lastVerbosePrefix;
    boolean verbose;
    boolean lastWasContinuous;
    
    DefaultScriptUserInterface(PrintStream output, boolean verbose) {
        consoleOut = output;
        this.verbose = verbose;
    }

    protected void print(String s) {
        consoleOut.print(s);
        lastWasContinuous = false;
        lastWasPrintln = false;
    }

    protected void println(String s) {
        consoleOut.println(s);
        lastWasPrintln = true;
        lastWasContinuous = false;
    }

    protected void forceNewLine() {
        if (!lastWasPrintln) {
            consoleOut.println();
        }
    }
    
    public void statusBegin(String msg) {
        forceNewLine();
        print( " # " + msg + "... ");
        inStatusRun = true;
        if (verbose) {
            lastVerbosePrefix = msg;
        }
        lastWasContinuous = false;
    }

    public void statusEnd(String msg) {
        if (msg == null) {
            msg = "Done";
        }
        if (lastVerbosePrefix != null) {
            // Re-print the last status prefix because a bunch of output might have come out
            statusBegin(lastVerbosePrefix);
        }
        if (!inStatusRun) {
            // Script is old / forgot to call statusBegin first
            statusBegin(msg);
            inStatusRun = false;
        } else {
            if (lastWasContinuous) {
                consoleOut.print(' '); // space before final status
            }
            println(msg);
            inStatusRun = false;
        }
        lastVerbosePrefix = null;
    }

    public void statusUpdate(String msg) {
        if (!inStatusRun) {
            // Script is old / forgot to call statusBegin first
            statusBegin(msg);
        } else {
            if (lastVerbosePrefix != null) {
                // Re-print the last status prefix because a bunch of output might have come out
                statusBegin(lastVerbosePrefix);
            }
            if (lastWasContinuous) {
                consoleOut.print(' '); // space before final status
            }
            print(msg + "... ");
            inStatusRun = true;
        }
    }

    public void statusFinal(String msg) {
        println(msg);
    }

    public void progressTicker(String charToAppend) {
        print(charToAppend);
        lastWasContinuous = true;
    }
    
    public void progressString(String currentProgressValue) {
        // @todo make this use cursor control to overwrite back to the previous non-progress location
        // and then overwrite with this
        print(currentProgressValue);
        lastWasContinuous = true;
    }
    
    public void inputPrompt(String message, String responses) {
        forceNewLine();
        println(message);
        if (responses != null) {
            print(" ["+responses+"]");
        } else {
            print("> ");
        }
    }

    public void message(String message) {
        forceNewLine();
        println(message);
    }
    
    public void finished() {
        forceNewLine();
    }
    
}