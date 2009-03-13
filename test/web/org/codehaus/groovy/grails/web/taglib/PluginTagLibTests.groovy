package org.codehaus.groovy.grails.web.taglib
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Feb 6, 2009
 */

public class PluginTagLibTests extends AbstractGrailsTagTests{

    void testPluginTagLib() {
        def template = '<plugin:isAvailable name="core">printme</plugin:isAvailable>'

        assertOutputEquals "printme", template

        template = '<plugin:isNotAvailable name="core">printme</plugin:isNotAvailable>'

        assertOutputEquals "", template

        template = '<plugin:isNotAvailable name="hibernate">printme</plugin:isNotAvailable>'

        assertOutputEquals "printme", template
    }

}