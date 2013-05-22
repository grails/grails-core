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
 |   GET    | /books/${bookId}/authors/create                                  | Action: create           |
 |   GET    | /books/${bookId}/authors/${id}/edit                              | Action: edit             |
 |  DELETE  | /books/${bookId}/authors/${id}(.${format})?                      | Action: delete           |
 |   PUT    | /books/${bookId}/authors/${id}(.${format})?                      | Action: update           |
 |   GET    | /books/${bookId}/authors/${id}(.${format})?                      | Action: show             |
 |   POST   | /books/${bookId}/authors(.${format})?                            | Action: save             |
 |   GET    | /books/${bookId}/authors(.${format})?                            | Action: index            |

Controller: book
 |   GET    | /books/create                                                    | Action: create           |
 |   GET    | /books/${id}/edit                                                | Action: edit             |
 |  DELETE  | /books/${id}(.${format})?                                        | Action: delete           |
 |   PUT    | /books/${id}(.${format})?                                        | Action: update           |
 |   GET    | /books/${id}(.${format})?                                        | Action: show             |
 |   POST   | /books(.${format})?                                              | Action: save             |
 |   GET    | /books(.${format})?                                              | Action: index            |

Controller: publisher
 |   GET    | /books/${bookId}/authors/${authorId}/publisher/edit              | Action: edit             |
 |   GET    | /books/${bookId}/authors/${authorId}/publisher/create            | Action: create           |
 |  DELETE  | /books/${bookId}/authors/${authorId}/publisher(.${format})?      | Action: delete           |
 |   PUT    | /books/${bookId}/authors/${authorId}/publisher(.${format})?      | Action: update           |
 |   GET    | /books/${bookId}/authors/${authorId}/publisher(.${format})?      | Action: show             |
 |   POST   | /books/${bookId}/authors/${authorId}/publisher(.${format})?      | Action: save             |

'''
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
 |    *     | /                                                  | View:   /index           |
 |    *     | /${controller}/${action}?/${id}?(.${format})?      | Action: (default action) |

Controller: errors
 |    *     | ERROR: 500                                         | Action: (default action) |

Controller: foo
 |   GET    | /foo/create                                        | Action: create           |
 |   GET    | /foo/${id}/edit                                    | Action: edit             |
 |  DELETE  | /foo/${id}(.${format})?                            | Action: delete           |
 |   PUT    | /foo/${id}(.${format})?                            | Action: update           |
 |   GET    | /foo/${id}(.${format})?                            | Action: show             |
 |   POST   | /foo(.${format})?                                  | Action: save             |
 |   GET    | /foo(.${format})?                                  | Action: index            |

Controller: image
 |    *     | /images/${name}**.jpg                              | Action: (default action) |

'''
    }
    UrlMappingsHolder getUrlMappingsHolder(Closure mappings) {
        def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext())
        def allMappings = evaluator.evaluateMappings mappings
        return new DefaultUrlMappingsHolder(allMappings)
    }
}
