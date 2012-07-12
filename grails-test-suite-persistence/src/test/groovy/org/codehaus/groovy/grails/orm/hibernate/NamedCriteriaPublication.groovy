package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

@Entity
class NamedCriteriaPublication {
    Long id
    Long version
    String title
    Date datePublished
    Boolean paperback = true

    static namedQueries = {
        aPaperback {
            eq 'paperback', true
        }

        paperbacksOrderedByDatePublishedDescending {
            eq 'paperback', true
            order 'datePublished', 'desc'
        }

        paperbacksOrderedByDatePublished {
            eq 'paperback', true
            order 'datePublished'
        }

        lastPublishedBefore { date ->
            uniqueResult = true
            le 'datePublished', date
            order 'datePublished', 'desc'
        }
        recentPublications {
            def now = new Date()
            gt 'datePublished', now - 365
        }

        publicationsWithBookInTitle {
            like 'title', '%Book%'
        }

        recentPublicationsByTitle { title ->
            recentPublications()
            eq 'title', title
        }

        latestBooks {
            maxResults(10)
            order("datePublished", "desc")
        }

        publishedBetween { start, end ->
            between 'datePublished', start, end
        }

        publishedAfter { date ->
            if (date != null) {
                gt 'datePublished', date
            }
        }

        paperbackOrRecent {
            or {
                def now = new Date()
                gt 'datePublished', now - 365
                paperbacks()
            }
        }

        paperbacks {
            eq 'paperback', true
        }

        paperbackAndRecent {
            paperbacks()
            recentPublications()
        }

        thisWeeksPaperbacks() {
            paperbacks()
            def today = new Date()
            publishedBetween(today - 7, today)
        }

        queryThatNestsMultipleLevels {
            // this nested query will call other nested queries
            thisWeeksPaperbacks()
        }
    }
}
