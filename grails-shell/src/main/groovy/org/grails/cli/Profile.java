package org.grails.cli;

import jline.console.completer.Completer;

interface Profile {
    String getName();
    Iterable<Completer> getCompleters();
    Iterable<CommandLineHandler> getCommandLineHandlers();
}
