package org.grails.cli.profile;

import jline.console.completer.Completer;

public interface Profile {
    String getName();
    Iterable<Completer> getCompleters();
    Iterable<CommandLineHandler> getCommandLineHandlers();
}
