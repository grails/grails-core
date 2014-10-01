package org.grails.cli.profile;

import grails.build.logging.GrailsConsole;

import org.codehaus.groovy.grails.cli.parsing.CommandLine;

public interface ExecutionContext extends ProjectContext {
    CommandLine getCommandLine();
    String getUnparsedCommandLine();
    GrailsConsole getConsole();
}
