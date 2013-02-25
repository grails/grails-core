package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

@Entity
class Book {
    String title
    Popularity popularity

    static embedded = ["popularity"]

    static namedQueries = {
        popularBooks {
            popularity {
                gt "liked", 0
            }
        }
    }
}

class Popularity {
    int liked
}

@Entity
class OneBookAuthor {
    Book book
}

@Entity
class OneAuthorPublisher {
    String name
    OneBookAuthor author

    static embedded = ['author']

    static namedQueries = {
        withPopularBooks {
            author {
                book {
                    popularity {
                        gt 'liked', 0
                    }
                }
            }
        }
    }
}