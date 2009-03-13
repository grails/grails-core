package org.codehaus.groovy.grails.commons;

/**
 * Original author: marc
 */
public class TestBean
{
    static private String welcomeMessage = "hello";

    private String userName = "marc";

    public String favouriteArtist = "Cardiacs";

    public static String favouriteFood = "indian";

    public String getUserName()
    {
        return userName;
    }

    public static String getWelcomeMessage()
    {
        return welcomeMessage;
    }
}
