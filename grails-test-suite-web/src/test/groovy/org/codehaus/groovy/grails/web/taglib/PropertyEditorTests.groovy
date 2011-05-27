package org.codehaus.groovy.grails.web.taglib

import java.beans.PropertyEditorSupport
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.beans.PropertyEditorRegistrar
import org.springframework.beans.PropertyEditorRegistry
import org.springframework.beans.factory.config.ConstructorArgumentValues
import org.springframework.beans.factory.support.RootBeanDefinition

 /**
 * @author Graeme Rocher
 * @since 1.1.1
 */
class PropertyEditorTests extends AbstractGrailsTagTests {

    protected void onSetUp() {

        gcl = new GroovyClassLoader(getClass().classLoader)
        gcl.parseClass '''
import org.codehaus.groovy.grails.web.taglib.*
import grails.persistence.*

@Entity
class PropertyEditorDomain {
    CustomProperty custom
}

@Entity
class CollectionPropertyEditorDomain {
    List tags
    static hasMany = [tags: String]
}

@Entity
class Tag {
    String name
}

@Entity
class OneToManyPropertyEditorDomain {
    List tags
    static hasMany = [tags: Tag]
}
'''
    }


    void testUseCustomPropertyEditor() {
        appCtx.registerBeanDefinition("testCustomEditorRegistrar", new RootBeanDefinition(TestCustomPropertyEditorRegistrar))

        def obj = ga.getDomainClass("PropertyEditorDomain").clazz.newInstance()

        obj.properties = [custom:"good"]

        assertEquals "good",obj.custom.name

        def template = '<g:fieldValue bean="${obj}" field="custom" />'

        assertOutputEquals 'custom:good', template, [obj:obj]
    }

    void testUseCustomPropertyEditorOnCollectionOfSimpleType() {
        appCtx.registerBeanDefinition("testSimpleListEditorRegistrar", new RootBeanDefinition(TestSimpleListEditorRegistrar))

        def obj = ga.getDomainClass("CollectionPropertyEditorDomain").clazz.newInstance()
        obj.properties = [tags: "grails, groovy"]

        assertEquals(["grails", "groovy"], obj.tags)

        def template = '<g:fieldValue bean="${obj}" field="tags" />'
        assertOutputEquals "grails, groovy", template, [obj: obj]
    }

    void testUseCustomPropertyEditorOnCollectionOfDomainType() {
        def bean = new RootBeanDefinition(TestDomainListEditorRegistrar)
        def args = new ConstructorArgumentValues()
        args.addGenericArgumentValue ga
        bean.constructorArgumentValues = args
        appCtx.registerBeanDefinition("testDomainListEditorRegistrar", bean)

        def obj = ga.getDomainClass("OneToManyPropertyEditorDomain").clazz.newInstance()
        obj.properties = [tags: "grails, groovy"]

        assertEquals(["grails", "groovy"], obj.tags.collect {it.name})

        def template = '<g:fieldValue bean="${obj}" field="tags" />'
        assertOutputEquals "grails, groovy", template, [obj: obj]
    }
}

class TestCustomPropertyEditorRegistrar implements PropertyEditorRegistrar {
    void registerCustomEditors(PropertyEditorRegistry registry) {
        registry.registerCustomEditor CustomProperty, new TestCustomPropertyEditor()
    }
}

class TestSimpleListEditorRegistrar implements PropertyEditorRegistrar {
    void registerCustomEditors(PropertyEditorRegistry registry) {
        registry.registerCustomEditor List, new TestSimpleListEditor()
    }
}

class TestDomainListEditorRegistrar implements PropertyEditorRegistrar {
    private final GrailsApplication ga

    TestDomainListEditorRegistrar(GrailsApplication ga) {
        this.ga = ga
    }

    void registerCustomEditors(PropertyEditorRegistry registry) {
        registry.registerCustomEditor List, new TestDomainListEditor(type: ga.getDomainClass("Tag").clazz)
    }
}

class TestCustomPropertyEditor extends PropertyEditorSupport {
    String getAsText() { "custom:${value?.name}" }

    void setAsText(String s) {
        Object v = getValue()
        if (v == null) v = new CustomProperty()
        value = v
        value.name = s
    }
}

class CustomProperty {
    String name
}

class TestSimpleListEditor extends PropertyEditorSupport {
    String getAsText() {
        value?.join(", ")
    }

    void setAsText(String text) {
        value = text.split(/,\s*/) as List
    }
}

class TestDomainListEditor extends PropertyEditorSupport {
    Class type

    String getAsText() {
        value?.name?.join(", ")
    }

    void setAsText(String text) {
        value = text.split(/,\s*/).collect {
            type.newInstance(name: it)
        }
    }
}
