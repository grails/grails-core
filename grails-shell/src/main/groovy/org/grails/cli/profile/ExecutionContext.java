package org.grails.cli.profile;

import grails.build.logging.GrailsConsole;

import org.codehaus.groovy.grails.cli.parsing.CommandLine;

public interface ExecutionContext {
    CommandLine getCommandLine();
    GrailsConsole getConsole();
}
