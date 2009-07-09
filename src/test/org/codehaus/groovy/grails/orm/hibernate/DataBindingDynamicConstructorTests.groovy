package org.codehaus.groovy.grails.orm.hibernate
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Sep 19, 2008
 */
class DataBindingDynamicConstructorTests extends AbstractGrailsHibernateTests {

        void onSetUp() {
            gcl.parseClass('''
    class DataBindingDynamicConstructorTest {
      Long id
      Long version
      String name
      Integer number
      static constraints = {
          name(size:5..15)
      }
    }
    ''')

        }

    void testDynamicConstructor() {
        def script = gcl.parseClass("""
t = new DataBindingDynamicConstructorTest(params)				
        """).newInstance()
        script.params = [name: 'blah', number:'10']

        def t = script.run()
        assertEquals "blah", t.name
        assertEquals 10, t.number

    }

    void onTearDown() {

    }
}
