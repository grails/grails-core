package org.codehaus.groovy.grails.cli;

/**
 * Utility methods for use on the command line, including method to accept user input etc.
 *
 * @author Marc Palmer
 * @since 1.4
 */
public class UserInterfaceCommandLineHelper extends CommandLineHelper {
    UserInterface ui;
    
    public UserInterfaceCommandLineHelper(UserInterface ui) {
        super();
        this.ui = ui;
    }
    
    protected void showInputMessage(String message, String responses) {
        ui.inputPrompt(message, responses);
    }

    protected void printMessage(String message) {
        ui.message(message);
    }
}