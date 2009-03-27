package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.web.binding.GrailsDataBinderTests.Author

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
class Person {
    String name

}
@Entity
class Author extends Person {
    Address address
}
@Entity
class Address {
    String houseNumber
}
@Entity
class Book {
    String title
    Person author
}


''')
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

      def Author = ga.getDomainClass("Author").clazz
      def Address = ga.getDomainClass("Address").clazz
      def Book = ga.getDomainClass("Book").clazz


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