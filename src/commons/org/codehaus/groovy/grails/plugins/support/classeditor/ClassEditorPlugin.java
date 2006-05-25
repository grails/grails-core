package org.codehaus.groovy.grails.plugins.support.classeditor;

import org.codehaus.groovy.grails.plugins.support.OrderedAdapter;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.beans.propertyeditors.ClassEditor;

/**
 * <p>Plugin that registers {@link org.codehaus.groovy.grails.support.ClassEditor}
 * with the Grails <code>ClassLoader</code>.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class ClassEditorPlugin extends OrderedAdapter implements GrailsPlugin {
    public void doWithGenericApplicationContext(GenericApplicationContext applicationContext, GrailsApplication application) {
        ClassEditor editor = new ClassEditor(applicationContext.getClassLoader());
        applicationContext.getBeanFactory().registerCustomEditor(Class.class, editor);
    }
}
