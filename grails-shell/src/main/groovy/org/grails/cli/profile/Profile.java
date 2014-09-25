package org.grails.cli.profile;

import jline.console.completer.Completer;

import org.grails.cli.CommandLineHandler;

public interface Profile {
    String getName();
    Iterable<Completer> getCompleters();
    Iterable<CommandLineHandler> getCommandLineHandlers();
}
