package org.codehaus.groovy.grails.cli;

interface UserInterface {
    void statusUpdate(String msg);

    void statusFinal(String msg);

    void statusBegin(String msg);

    void statusEnd(String msg);

    void progressString(String currentProgressValue);

    void progressTicker(String charToAppend);

    void inputPrompt(String message, String responses);

    void message(String message);
    
    void finished();
}