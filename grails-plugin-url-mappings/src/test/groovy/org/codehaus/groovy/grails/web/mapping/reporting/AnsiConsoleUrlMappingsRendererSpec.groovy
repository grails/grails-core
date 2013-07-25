package org.codehaus.groovy.grails.web.mapping.reporting

import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingEvaluator
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.springframework.mock.web.MockServletContext
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class AnsiConsoleUrlMappingsRendererSpec extends Specification{

    void "Test render URL mappings for 3 level resource"() {
        given:"A URL mappings renderer"
            def sw = new ByteArrayOutputStream()
            def ps = new PrintStream(sw)
            def renderer = new AnsiConsoleUrlMappingsRenderer(ps)
            renderer.isAnsiEnabled = false
            def urlMappingsHolder = getUrlMappingsHolder {
                "/books"(resources:'book') {
                    '/authors'(resources:'author') {
                        '/publisher'(resource:'publisher')
                    }
                }
            }
        when:"The URL mappings are rendered"
            renderer.render(urlMappingsHolder.urlMappings.toList())
            println sw.toString()
        then:"The output is correct"
            sw.toString() == '''Controller: author
 |   GET    | /books/${bookId}/authors/create                            | Action: create           |
 |   POST   | /books/${bookId}/authors                                   | Action: save             |
 |   GET    | /books/${bookId}/authors                                   | Action: index            |
 |   GET    | /books/${bookId}/authors/${id}/edit                        | Action: edit             |
 |  DELETE  | /books/${bookId}/authors/${id}                             | Action: delete           |
 |   PUT    | /books/${bookId}/authors/${id}                             | Action: update           |
 |   GET    | /books/${bookId}/authors/${id}                             | Action: show             |

Controller: book
 |   GET    | /books/create                                              | Action: create           |
 |   POST   | /books                                                     | Action: save             |
 |   GET    | /books                                                     | Action: index            |
 |   GET    | /books/${id}/edit                                          | Action: edit             |
 |  DELETE  | /books/${id}                                               | Action: delete           |
 |   PUT    | /books/${id}                                               | Action: update           |
 |   GET    | /books/${id}                                               | Action: show             |

Controller: publisher
 |   GET    | /books/${bookId}/authors/${authorId}/publisher/edit        | Action: edit             |
 |   GET    | /books/${bookId}/authors/${authorId}/publisher/create      | Action: create           |
 |  DELETE  | /books/${bookId}/authors/${authorId}/publisher             | Action: delete           |
 |   PUT    | /books/${bookId}/authors/${authorId}/publisher             | Action: update           |
 |   GET    | /books/${bookId}/authors/${authorId}/publisher             | Action: show             |
 |   POST   | /books/${bookId}/authors/${authorId}/publisher             | Action: save             |

'''.denormalize()
    }

    void "Test render URL mappings to target stream"() {
        given:"A URL mappings renderer"
            def sw = new ByteArrayOutputStream()
            def ps = new PrintStream(sw)
            def renderer = new AnsiConsoleUrlMappingsRenderer(ps)
            renderer.isAnsiEnabled = false
            def urlMappingsHolder = getUrlMappingsHolder {
                "/$controller/$action?/$id?(.$format)?"()
                "/images/$name**.jpg"(controller:"image")
                "/foo"(resources:"foo")
                "500"(controller:"errors")
                "/"(view:"/index")

            }

        when:"The URL mappings are rendered"
            renderer.render(urlMappingsHolder.urlMappings.toList())
            println sw.toString()
        then:"The output is correct"
            sw.toString() == '''Dynamic Mappings
 |    *     | /                                                 | View:   /index           |
 |    *     | /${controller}/${action}?/${id}?(.${format)?      | Action: (default action) |

Controller: errors
 |    *     | ERROR: 500                                        | Action: (default action) |

Controller: foo
 |   GET    | /foo/create                                       | Action: create           |
 |   POST   | /foo                                              | Action: save             |
 |   GET    | /foo                                              | Action: index            |
 |   GET    | /foo/${id}/edit                                   | Action: edit             |
 |  DELETE  | /foo/${id}                                        | Action: delete           |
 |   PUT    | /foo/${id}                                        | Action: update           |
 |   GET    | /foo/${id}                                        | Action: show             |

Controller: image
 |    *     | /images/${name}**.jpg                             | Action: (default action) |

'''.denormalize()
    }
    UrlMappingsHolder getUrlMappingsHolder(Closure mappings) {
        def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext())
        def allMappings = evaluator.evaluateMappings mappings
        return new DefaultUrlMappingsHolder(allMappings)
    }
}
