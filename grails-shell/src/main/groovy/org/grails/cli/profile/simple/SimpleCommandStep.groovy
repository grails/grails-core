package org.grails.cli.profile.simple

import grails.build.logging.GrailsConsole

import org.codehaus.groovy.grails.cli.parsing.CommandLine
import org.grails.cli.profile.Profile

abstract class SimpleCommandStep {
    Map<String, String> commandParameters
    SimpleCommand command
    
    /**
     * @param commandLine
     * @param console
     * @return true if should continue executing other steps
     */
    abstract public boolean handleStep(CommandLine commandLine, GrailsConsole console)
}
