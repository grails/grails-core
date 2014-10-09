package org.grails.cli.profile;

import grails.build.logging.GrailsConsole;

import org.grails.build.parsing.CommandLine;

public interface ExecutionContext extends ProjectContext {
    CommandLine getCommandLine();
    String getUnparsedCommandLine();
    GrailsConsole getConsole();
    void cancel();
    void addCancelledListener(CommandCancelledListener listener);
}
