package org.grails.cli.profile;

import grails.build.logging.GrailsConsole;

import java.io.File;

import org.codehaus.groovy.grails.cli.parsing.CommandLine;

public interface ExecutionContext {
    CommandLine getCommandLine();
    GrailsConsole getConsole();
    File getBaseDir();
    String navigateConfig(String... path);
    <T> T navigateConfigForType(Class<T> requiredType, String... path);
}
