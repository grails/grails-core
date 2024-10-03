package grails.test.mixin

import grails.persistence.Entity
import grails.testing.gorm.DataTest
import spock.lang.Specification

import java.time.LocalDate
import java.time.ZoneId

class UnitTestEmbeddedPropertyQuery extends Specification implements DataTest {
    
    void setupSpec() {
        mockDomains Author2, Book2
    }
    
    void setup() {
        def author = new Author2(name: 'George')

        def book = new Book2(
            name: 'Game of Thrones',
            publishPeriod: new Period(
                startDate: date(2012, 1, 1),
                endDate: date(2013, 1, 1)
            )
        )

        author.addToBooks(book)
        author.save(flush: true, failOnError: true)
    }

    void testQueryEmbedded() {

        expect:
        Book2.withCriteria {
            gt 'publishPeriod.startDate', date(2011, 1, 1)
        }.size() == 1

        Book2.withCriteria {
            ge 'publishPeriod.startDate', date(2012, 1, 1)
        }.size() == 1

        Book2.withCriteria {
            lt 'publishPeriod.startDate', date(2014, 1, 1)
        }.size() == 1

        Book2.withCriteria {
            le 'publishPeriod.startDate', date(2012, 1, 1)
        }.size() == 1

        Book2.withCriteria {
            eq 'publishPeriod.startDate', date(2012, 1, 1)
        }.size() == 1

        Book2.withCriteria {
            ne 'publishPeriod.startDate', date(2017, 1, 1)
        }.size() == 1

        Book2.withCriteria {
            isNotNull 'publishPeriod.startDate'
        }.size() == 1
    }

    void testAssociated() {

        expect:
        Author2.withCriteria {
            books {
                gt 'publishPeriod.startDate', date(2011, 1, 1)
            }
        }.size() == 1

        Author2.withCriteria {
            books {
                ge 'publishPeriod.startDate', date(2012, 1, 1)
            }
        }.size() == 1

        Author2.withCriteria {
            books {
                lt 'publishPeriod.startDate', date(2014, 1, 1)
            }
        }.size() == 1

        Author2.withCriteria {
            books {
                le 'publishPeriod.startDate', date(2012, 1, 1)
            }
        }.size() == 1

        Author2.withCriteria {
            books {
                eq 'publishPeriod.startDate', date(2012, 1, 1)
            }
        }.size() == 1

        Author2.withCriteria {
            books {
                ne 'publishPeriod.startDate', date(2017, 1, 1)
            }
        }.size() == 1

        Author2.withCriteria {
            books {
                isNotNull 'publishPeriod.startDate'
            }
        }.size() == 1
    }

    void testQueryToOne() {

        expect:
        Book2.withCriteria {
            gt 'publishPeriod.startDate', date(2011, 1, 1)
            author {
                eq 'name', 'George'
            }
        }.size() == 1
    }

    private Date date(int year, int month, int day) {
        Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant())
    }
}

@Entity
class Book2 {
    String name
    Period publishPeriod

    static belongsTo = [author: Author2]
    static embedded = ['publishPeriod']
}

@Entity
class Author2 {
    String name
    static hasMany = [books: Book2]
}

class Period {
    Date startDate
    Date endDate
}
