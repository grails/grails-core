package grails.test

import grails.persistence.Entity

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class MockUtilsAndHasManyTests extends GroovyTestCase{

      void testMockDomainWithHasMany() {
          def test = new MagazineTests()
          test.setUp()
          test.testSomething()
      }

}
class MagazineTests extends GrailsUnitTestCase {
    void testSomething() {
        mockDomain(Magazine)
        def magazine1 = new Magazine(articles: [new Article()]) // throws NPE in 1.2-M2, no exception in 1.1.1

        assertEquals 1, magazine1.articles.size()
    }
}
@Entity
class Magazine { static hasMany = [articles: Article] }
@Entity
class Article { static belongsTo = [magazine: Magazine] }