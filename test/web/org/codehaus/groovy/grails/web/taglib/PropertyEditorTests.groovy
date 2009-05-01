package org.codehaus.groovy.grails.web.taglib

import org.springframework.beans.PropertyEditorRegistrar
import org.springframework.beans.PropertyEditorRegistry
import java.beans.PropertyEditor
import java.beans.PropertyEditorSupport
import org.springframework.beans.factory.support.RootBeanDefinition

/**
 * @author Graeme Rocher
 * @since 1.1.1
 * 
 * Created: May 1, 2009
 */

public class PropertyEditorTests extends AbstractGrailsTagTests{

    public void onSetUp() {
        gcl = new GroovyClassLoader(this.getClass().classLoader)
        gcl.parseClass('''
import grails.persistence.*
import org.codehaus.groovy.grails.web.taglib.*

@Entity
class PropertyEditorDomain {
    CustomProperty custom
}
''')
    }

    void testUseCustomPropertyEditor() {
        appCtx.registerBeanDefinition("testCustomEditorRegistrar", new RootBeanDefinition(TestCustomPropertyEditorRegistrar))

        def obj = ga.getDomainClass("PropertyEditorDomain").clazz.newInstance()

        obj.properties = [custom:"good"]

        assertEquals "good",obj.custom.name

        def template = '<g:fieldValue bean="${obj}" field="custom" />'

        assertOutputEquals 'custom:good', template, [obj:obj] 
        
        
    }


}
class TestCustomPropertyEditorRegistrar implements PropertyEditorRegistrar {

    public void registerCustomEditors(PropertyEditorRegistry registry) {
        registry.registerCustomEditor CustomProperty, new TestCustomPropertyEditor()
    }
}
class TestCustomPropertyEditor extends PropertyEditorSupport {

    public String getAsText() {
        return "custom:${value?.name}"
    }

    public void setAsText(String s) {
        Object v = getValue()
        if(v == null) v = new CustomProperty()
        value = v
        value.name = s
    }


}
class CustomProperty {
    String name
}
