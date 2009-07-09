package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Jul 21, 2008
 */
class CascadingSaveAndUniqueConstraintTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
class Face {
    Long id
    Long version
    Nose nose

    static constraints = {
        nose(unique: true)
    }
}
class Nose {
    Long id
    Long version
    static belongsTo = [face:Face]
}
''')
    }


    void testCascadingSaveAndUniqueConstraint() {
        def faceClass = ga.getDomainClass("Face").clazz
        def noseClass = ga.getDomainClass("Nose").clazz

        def face = faceClass.newInstance(nose:noseClass.newInstance()).save()
    }

}