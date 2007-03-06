package org.codehaus.groovy.grails.commons;

import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;

import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Marc Palmer (marc@anyware.co.uk)
*/
public class TagLibArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "TagLib";

    private HashMap tag2libMap;


    public TagLibArtefactHandler() {
        super(TYPE, GrailsTagLibClass.class, DefaultGrailsTagLibClass.class, DefaultGrailsTagLibClass.TAG_LIB);
    }

    /**
     * Creates a map of tags to tag libraries
     */
    public void initialize(ArtefactInfo artefacts) {
        this.tag2libMap = new HashMap();
        GrailsClass[] classes = artefacts.getGrailsClasses();
        for (int i = 0; i < classes.length; i++) {
            GrailsTagLibClass taglibClass = (GrailsTagLibClass) classes[i];
            for (Iterator j = taglibClass.getTagNames().iterator(); j.hasNext();) {
                String tagName = (String) j.next();
                if (!tag2libMap.containsKey(tagName)) {
                    this.tag2libMap.put(tagName, taglibClass);
                }
                else {
                    GrailsTagLibClass current = (GrailsTagLibClass) tag2libMap.get(tagName);
                    if (!taglibClass.equals(current)) {
                        this.tag2libMap.put(tagName, taglibClass);
                    }
                    else {
                        throw new GrailsConfigurationException("Cannot configure tag library [" + taglibClass.getName() + "]. Library [" + current.getName() + "] already contains a tag called [" + tagName + "]");
                    }
                }
            }
        }
    }

    public GrailsClass getArtefactForFeature(Object feature) {
        return (GrailsClass) tag2libMap.get(feature);
    }
}
