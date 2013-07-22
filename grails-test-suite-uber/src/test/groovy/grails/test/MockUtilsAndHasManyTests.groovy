package grails.test

import static org.junit.Assert.*
import grails.persistence.Entity
import grails.test.mixin.Mock

import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class MockUtilsAndHasManyTests extends GroovyTestCase {

    @Override
    protected void setUp() {
        ApplicationHolder.application = null
    }

    @Override
    protected void tearDown() {
        super.tearDown()
        ApplicationHolder.application = null
    }

    void testMockDomainWithHasMany() {
        def test = new MagazineTests()
        test.setUp()
        test.testSomething()
        test.tearDown()
    }
}

class MagazineTests extends GrailsUnitTestCase {

    @Override
    protected void tearDown() {
        super.tearDown()
        ApplicationHolder.application = null
    }

    void testSomething() {
        mockDomain(Magazine)
        def magazine1 = new Magazine(articles: [new Article()]) // throws NPE in 1.2-M2, no exception in 1.1.1

        assertEquals 1, magazine1.articles.size()
    }

    // check against GRAILS-7309
    void testAddToOneManyAssociation_GRAILS_7309() {
        // given
        mockDomain(Magazine)
        mockDomain(Article)
        mockDomain(Paragraph)
        def magazine = new Magazine()
        assertNotNull magazine.save()

        // when
        def article = new Article()
        magazine.addToArticles(article)

        // then
        assertNotNull article.magazine
        assertEquals magazine, article.magazine
    }
}

// test mockDomain using mixin
@Mock([Magazine,Article,Paragraph])
class MockDomainMixinTests {

    // check against GRAILS-7309
    @Test
    void testAddToOneManyAssociation_GRAILS_7309() {
        // given
        def magazine = new Magazine()
        assertNotNull magazine.save()

        // when
        def article = new Article()
        magazine.addToArticles(article)

        // then
        assertNotNull article.magazine
        assertEquals magazine, article.magazine
    }
}

@Entity
class Magazine {
    static hasMany = [articles: Article]
}

@Entity
class Article {
    static hasMany = [paragraphs: Paragraph]
    static belongsTo = [magazine: Magazine]
}

@Entity
class Paragraph {
    static belongsTo = [article: Article]
}
