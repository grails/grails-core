package org.codehaus.groovy.grails.commons;

/**
 * @author Marc Palmer (marc@anyware.co.uk)
*/
public class TaskArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "Task";


    public TaskArtefactHandler() {
        super(TYPE, GrailsTaskClass.class, DefaultGrailsTaskClass.class, null);
    }

    public boolean isArtefactClass( Class clazz ) {
        if(clazz == null) return false;
        try {
            clazz.getDeclaredMethod( GrailsTaskClassProperty.EXECUTE , new Class[]{});
        } catch (SecurityException e) {
            return false;
        } catch (NoSuchMethodException e) {
            return false;
        }

        return clazz.getName().endsWith(DefaultGrailsTaskClass.JOB);
    }
}
