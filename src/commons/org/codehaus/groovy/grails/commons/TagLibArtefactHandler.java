/*
* Copyright 2004-2005 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.codehaus.groovy.grails.commons;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An ArtefactHandlerAdapter that configures tag libraries within namespaces in Grails
 * 
 * @author Marc Palmer (marc@anyware.co.uk)
 * @author Graeme Rocher
 * @author a.shneyderman
 *
 * @since 0.5
*/
public class TagLibArtefactHandler extends ArtefactHandlerAdapter {

    private static Log LOG = LogFactory.getLog(TagLibArtefactHandler.class);
    public static final String TYPE = "TagLib";

    private Map tag2libMap = new HashMap();
    private Map namespace2tagLibMap = new HashMap();


    public TagLibArtefactHandler() {
        super(TYPE, GrailsTagLibClass.class, DefaultGrailsTagLibClass.class, DefaultGrailsTagLibClass.TAG_LIB);
    }

    /**
     * Creates a map of tags (keyed on "${namespace}:${tagName}") to tag libraries
     */
    public void initialize(ArtefactInfo artefacts) {
        this.tag2libMap = new HashMap();
        GrailsClass[] classes = artefacts.getGrailsClasses();
        for (int i = 0; i < classes.length; i++) {
            GrailsTagLibClass taglibClass = (GrailsTagLibClass) classes[i];
            String namespace = taglibClass.getNamespace();
            namespace2tagLibMap.put(namespace, taglibClass);
            for (Iterator j = taglibClass.getTagNames().iterator(); j.hasNext();) {
               	String tagName = namespace + ":" + (String) j.next();
                if (!tag2libMap.containsKey(tagName)) {
                    this.tag2libMap.put(tagName, taglibClass);
                }
                else {
                    GrailsTagLibClass current = (GrailsTagLibClass) tag2libMap.get(tagName);
                    if (!taglibClass.equals(current)) {
                       	LOG.info("There are conflicting tags: " + taglibClass.getFullName() + "." + tagName + " vs. " + current.getFullName() + "." + tagName + ". The former will take precedence.");
                       	this.tag2libMap.put(tagName, taglibClass);
                    }
                    else {
                        throw new GrailsConfigurationException("Cannot configure tag library [" + taglibClass.getName() + "]. Library [" + current.getName() + "] already contains a tag called [" + tagName + "]");
                    }
                }
            }
        }
    }

    /**
     * This will look-up a tag library by using either a full qualified tag name such as g:link or via namespace such as "g"
     *
     * @param feature The tag name or namespace
     * @return A GrailsClass instance representing the tag library
     */
    public GrailsClass getArtefactForFeature(Object feature) {
        final Object tagLib = tag2libMap.get(feature);
        if(tagLib!= null)
            return (GrailsClass) tagLib;
        else {
             return (GrailsClass)namespace2tagLibMap.get(feature);
        }
    }
}
