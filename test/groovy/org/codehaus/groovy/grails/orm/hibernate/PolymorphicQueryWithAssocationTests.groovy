package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jun 2, 2008
 */
class PolymorphicQueryWithAssocationTests  extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
class PolymorphicQueryWithAssocationPerson {
    Long id
    Long version
    Set bases
    static hasMany = [ bases:PolymorphicQueryWithAssocationBase]
}
class PolymorphicQueryWithAssocationBase {
    Long id
    Long version

    static belongsTo = PolymorphicQueryWithAssocationPerson
    PolymorphicQueryWithAssocationPerson person
}
class PolymorphicQueryWithAssocationHyperBase extends PolymorphicQueryWithAssocationBase {
}
class PolymorphicQueryWithAssocationSpecialBase extends PolymorphicQueryWithAssocationBase {
}
''')
    }



    void testQueryPolymorphicAssocation() {
        def baseClass = ga.getDomainClass("PolymorphicQueryWithAssocationBase").clazz
        def personClass = ga.getDomainClass("PolymorphicQueryWithAssocationPerson").clazz
        def hyperBaseClass = ga.getDomainClass("PolymorphicQueryWithAssocationHyperBase").clazz
        def specialBaseClass = ga.getDomainClass("PolymorphicQueryWithAssocationSpecialBase").clazz

        def p = personClass.newInstance().save()
        assert hyperBaseClass.newInstance( person: p).save()
        assert specialBaseClass.newInstance(person: p).save()

        assertEquals personClass.findAll().size(), 1
        assertEquals hyperBaseClass.findAll().size(), 1
        assertEquals specialBaseClass.findAll().size(), 1
        assertEquals baseClass.findAll().size(), 2


        assertEquals baseClass.findAllByPerson(p).size(), 2
    }
}