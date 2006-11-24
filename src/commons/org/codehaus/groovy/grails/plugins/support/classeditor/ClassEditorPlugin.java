package org.codehaus.groovy.grails.plugins.support.classeditor;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.AbstractGrailsPlugin;
import org.codehaus.groovy.grails.plugins.GrailsPlugin;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.propertyeditors.ClassEditor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * <p>Plugin that registers {@link org.codehaus.groovy.grails.support.ClassEditor}
 * with the Grails <code>ClassLoader</code>.</p>
 *
 * @author Steven Devijver
 * @since 0.2
 */
public class ClassEditorPlugin extends AbstractGrailsPlugin implements GrailsPlugin {
    public ClassEditorPlugin(Class pluginClass, GrailsApplication application) {
		super(pluginClass, application);
	}

	protected BeanWrapper pluginBean;

	public void doWithApplicationContext(ApplicationContext applicationContext) {
    	if(applicationContext instanceof GenericApplicationContext) {
    		GenericApplicationContext ctx = (GenericApplicationContext)applicationContext;

    		ClassEditor editor = new ClassEditor(applicationContext.getClassLoader());
    		ctx.getBeanFactory().registerCustomEditor(Class.class, editor);
    	}
    }

	public void doWithRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
		// do nothing
	}
}
