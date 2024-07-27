package org.grails.cli;

import jline.TerminalSupport;

public class TestTerminal extends TerminalSupport {
    public TestTerminal() {
        super(true);
        setAnsiSupported(false);
        setEchoEnabled(false);
    }
}
