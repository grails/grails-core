package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.web.binding.GrailsDataBinderTests.Author
import org.hibernate.proxy.HibernateProxy
import org.apache.commons.beanutils.PropertyUtils

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Mar 27, 2009
 */

public class LazyProxiedAssociationsWithInheritanceTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*

@Entity
class ContentRevision implements Serializable {

    Date dateCreated

    static belongsTo = [
        content: Content
    ]
}
@Entity
class Content implements Serializable {

    Date dateCreated
    Date lastUpdated

    List revisions

	static hasMany = [
	    revisions: ContentRevision,
	]
}
@Entity
class ArticleRevision extends ContentRevision {
    String body
}
@Entity
class Article extends Content {
    String author
}

@Entity
class LazyProxiedAssociationsWithInheritancePerson {
    String name

}
@Entity
class LazyProxiedAssociationsWithInheritanceAuthor extends LazyProxiedAssociationsWithInheritancePerson {
    LazyProxiedAssociationsWithInheritanceAddress address

    def houseNumber() {
        address.houseNumber
    }

    def sum(a, b) { a + b }
}
@Entity
class LazyProxiedAssociationsWithInheritanceAddress {
    String houseNumber
}
@Entity
class LazyProxiedAssociationsWithInheritanceBook {
    String title
    LazyProxiedAssociationsWithInheritancePerson author
}


''')
    }

    protected void setUp() {
        super.setUp();
    }



    void testMethodCallsOnProxiedObjects() {

        def Author = ga.getDomainClass("LazyProxiedAssociationsWithInheritanceAuthor").clazz
        def Address = ga.getDomainClass("LazyProxiedAssociationsWithInheritanceAddress").clazz
        def Book = ga.getDomainClass("LazyProxiedAssociationsWithInheritanceBook").clazz


        def addr = Address.newInstance(houseNumber:'52')
        def auth = Author.newInstance(name:'Marc Palmer')
        assert addr.save()
        auth.address = addr
        assert auth.save()

        def book = Book.newInstance(title:"The Grails book of bugs")
        book.author = auth
        assertNotNull "book should have saved", book.save()

        session.flush()
        session.clear()

        book = Book.get(1)

         def proxy = PropertyUtils.getProperty(book, "author")
  //       assertTrue "should be a hibernate proxy", (proxy instanceof HibernateProxy)
    //     assertFalse "proxy should not be initialized", org.hibernate.Hibernate.isInitialized(proxy)

//         assertEquals "Marc Palmer", proxy.name

//        proxy.class.interfaces.each { println it.name }
         
         assertEquals "52", proxy.address.houseNumber
         assertEquals "52", proxy.houseNumber()
         assertEquals 10, proxy.sum(5,5)
         assertFalse "proxies should be instances of the actual class", Author.isInstance(proxy) 
     }


    void testLazyProxiesWithInheritance() {
         def Article = ga.getDomainClass("Article").clazz
        def ArticleRevision = ga.getDomainClass("ArticleRevision").clazz

         def article

         article = Article.newInstance(author:'author1')
         article.addToRevisions(ArticleRevision.newInstance(title:'title1', body:'body1'))
         assertNotNull "article should have saved", article.save()

         article = Article.newInstance(author:'author2')
         article.addToRevisions(ArticleRevision.newInstance(title:'title2', body:'body2'))
         article.addToRevisions(ArticleRevision.newInstance(title:'title3', body:'body3'))
         assertNotNull "article should have saved", article.save()


         session.flush()
         session.clear()
        
		def revisionList = ArticleRevision.findAll()

        def rev = revisionList[0]

        assertEquals "author1", rev.content.author
    }


    void testLazyProxiesWithInheritance2() {

      def Author = ga.getDomainClass("LazyProxiedAssociationsWithInheritanceAuthor").clazz
      def Address = ga.getDomainClass("LazyProxiedAssociationsWithInheritanceAddress").clazz
      def Book = ga.getDomainClass("LazyProxiedAssociationsWithInheritanceBook").clazz


      def addr = Address.newInstance(houseNumber:'52')
      def auth = Author.newInstance(name:'Marc Palmer')
      assert addr.save()
      auth.address = addr
      assert auth.save()

      def book = Book.newInstance(title:"The Grails book of bugs")
      book.author = auth
      assertNotNull "book should have saved", book.save()

      session.flush()
      session.clear()

      book = Book.get(1)
      assertEquals "Marc Palmer", book.author.name
      assertEquals "52", book.author.address.houseNumber

    }
}