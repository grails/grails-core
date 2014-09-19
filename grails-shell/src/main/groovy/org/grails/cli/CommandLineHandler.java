package org.grails.cli;

import grails.build.logging.GrailsConsole;

import org.codehaus.groovy.grails.cli.parsing.CommandLine;

public interface CommandLineHandler {
    boolean handleCommand(CommandLine commandLine, GrailsConsole console); 
    boolean handleHelp(String command);
}
