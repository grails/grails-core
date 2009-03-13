package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Feb 26, 2009
 */

public class DataBindingHibernateTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class Bookmark {
    URL url
}
''')
    }

    void testDataBindingErrors() {
        def Bookmark = ga.getDomainClass("Bookmark").clazz

        def b = Bookmark.newInstance()

        b.properties = [url:"bad_url"]

        assertTrue "should have a validation errors",b.hasErrors()
        assertNotNull "should have a invalid URL error", b.errors.getFieldError("url")
    }


}