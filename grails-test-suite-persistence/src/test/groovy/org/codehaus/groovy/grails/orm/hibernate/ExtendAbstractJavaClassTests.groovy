package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author graemerocher
 */
class ExtendAbstractJavaClassTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*


@Entity
class ExtendAbstractJava extends org.codehaus.groovy.grails.orm.hibernate.ExtendAbstractJavaClassTests.JavaClass {

}
''')
    }

    void testPersistClassThatExtendsJavaClass() {
        def ExtendAbstractJava = ga.getDomainClass('ExtendAbstractJava').clazz

        def e = ExtendAbstractJava.newInstance()

        e.save()

        assert e.id
    }

    public static abstract class JavaClass {}
}


