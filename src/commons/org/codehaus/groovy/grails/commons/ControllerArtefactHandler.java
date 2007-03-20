package org.codehaus.groovy.grails.commons;

/**
 * @author Marc Palmer (marc@anyware.co.uk)
*/
public class ControllerArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "Controller";
    private GrailsClass[] controllerClasses;


    public ControllerArtefactHandler() {
        super(TYPE, GrailsControllerClass.class, DefaultGrailsControllerClass.class,
            DefaultGrailsControllerClass.CONTROLLER,
            false);
    }

    public void initialize(ArtefactInfo artefacts) {
        controllerClasses = artefacts.getGrailsClasses();
    }

    public GrailsClass getArtefactForFeature(Object feature) {
        String uri = feature.toString();
        for (int i = 0; i < controllerClasses.length; i++) {
            if (((GrailsControllerClass)controllerClasses[i]).mapsToURI(uri)) {
                return controllerClasses[i];
            }
        }
        return null;
    }


}
