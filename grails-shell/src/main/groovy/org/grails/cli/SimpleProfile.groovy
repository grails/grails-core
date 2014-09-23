package org.grails.cli

import groovy.transform.CompileStatic
import jline.console.completer.Completer

@CompileStatic
class SimpleProfile implements Profile {
    File profileDir
    String name
    List<CommandLineHandler> commandLineHandlers = []
    
    public SimpleProfile(String name, File profileDir) {
        this.name = name
        this.profileDir = profileDir 
    }

    @Override
    public Iterable<Completer> getCompleters() {
        null
    }

    @Override
    public Iterable<CommandLineHandler> getCommandLineHandlers() {
        null
    }    
}
