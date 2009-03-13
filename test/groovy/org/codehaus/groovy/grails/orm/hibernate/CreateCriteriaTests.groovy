package org.codehaus.groovy.grails.orm.hibernate
/**
 * Author: Siegfried Puchbauer
 * Created: Dec 19, 2007 1:48:21 PM
 */
class CreateCriteriaTests extends AbstractGrailsHibernateTests {

    void testCreateCriteriaMethod() {
        def books = []
        def dc = ga.getDomainClass("CreateCriteriaMethodBook")
        25.times {
            def book = dc.newInstance()
            book.title = "Book $it"
            books << book
        }
        books*.save(true)

        def results = dc.clazz.createCriteria().list(max: 10, offset: 0) {
            like("title","Book%")
        }

        assertEquals results?.size(), 10
        assertEquals results?.totalCount, 25
    }

    void onSetUp() {
        gcl.parseClass("""
class CreateCriteriaMethodBook {
  Long id
  Long version
  String title

  boolean equals(obj) { title == obj?.title }
  int hashCode() { title ? title.hashCode() : super.hashCode() }
  String toString() { title }

  static constraints = {
      title(nullable:false)
  }
}
""")
    }

}