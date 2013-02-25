package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author graemerocher
 */
class ExtendAbstractJavaClassTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.Entity
import org.codehaus.groovy.grails.orm.hibernate.ExtendAbstractJavaClassTests.JavaClass

@Entity
class ExtendAbstractJava extends JavaClass {}
''')
    }

    void testPersistClassThatExtendsJavaClass() {
        def ExtendAbstractJava = ga.getDomainClass('ExtendAbstractJava').clazz

        def e = ExtendAbstractJava.newInstance()

        e.save()

        assert e.id
    }

    static abstract class JavaClass {}
}
