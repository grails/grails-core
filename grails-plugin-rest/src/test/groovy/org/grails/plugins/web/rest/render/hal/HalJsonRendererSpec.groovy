/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.plugins.web.rest.render.hal

import grails.rest.render.Renderer
import grails.rest.render.hal.HalJsonCollectionRenderer
import groovy.transform.NotYetImplemented
import org.springframework.core.convert.converter.Converter
import spock.lang.Ignore
import spock.lang.Specification
import grails.rest.render.hal.HalJsonRenderer
import org.springframework.context.support.StaticMessageSource
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mapping.DefaultLinkGenerator
import grails.web.CamelCaseUrlConverter
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingEvaluator
import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder
import grails.persistence.Entity
import grails.util.GrailsWebUtil
import org.springframework.web.util.WebUtils
import org.grails.plugins.web.rest.render.ServletRenderContext
import spock.lang.Issue

import javax.xml.bind.DatatypeConverter

/**
 */
class HalJsonRendererSpec extends Specification{



    @Issue('GRAILS-10372')
    void "Test that the HAL renderer renders JSON values correctly for domains"() {
        given:"A HAL renderer"
            HalJsonRenderer renderer = getRenderer()
            renderer.prettyPrint = true

        when:"A domain object is rendered"
            def webRequest = GrailsWebUtil.bindMockWebRequest()
            webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/product/Macbook")
            def response = webRequest.response
            def renderContext = new ServletRenderContext(webRequest)
            def product = new Product(name: "MacBook", numberInStock: 10, category: new Category(name: 'Laptops'))
            renderer.render(product, renderContext)

        then:"The resulting HAL is correct"
            response.contentType == GrailsWebUtil.getContentType(HalJsonRenderer.MIME_TYPE.name, GrailsWebUtil.DEFAULT_ENCODING)
        response.contentAsString == '''{
  "_links": {
    "self": {
      "href": "http://localhost/products",
      "hreflang": "en",
      "type": "application/hal+json"
    }
  },
  "name": "MacBook",
  "numberInStock": 10,
  "_embedded": {
    "category": {
      "_links": {
        "self": {
          "href": "http://localhost/category/index",
          "hreflang": "en"
        }
      },
      "name": "Laptops"
    }
  }
}'''

    }

    @Issue('GRAILS-10499')
    void "Test that the HAL rendered renders JSON values correctly for collection" () {
        given: "A HAL Collection renderer"
            HalJsonCollectionRenderer renderer = getCollectionRenderer()
            renderer.prettyPrint = true

        when: "A collection of domian objects is rendered"
            def webRequest = GrailsWebUtil.bindMockWebRequest()
            webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/product/Macbook")
            def response = webRequest.response
            def renderContext = new ServletRenderContext(webRequest)
            def products = [
                new Product(name: "MacBook", numberInStock: 10, category:  new Category(name: 'Laptops')),
                new Product(name: "iMac", numberInStock: 42, category:  new Category(name: 'Desktops'))
            ]
            renderer.render(products, renderContext)

        then:"The resulting HAL is correct"
            response.contentType == GrailsWebUtil.getContentType(HalJsonRenderer.MIME_TYPE.name,
                    GrailsWebUtil.DEFAULT_ENCODING)
            response.contentAsString == '''{
  "_links": {
    "self": {
      "href": "http://localhost/product/Macbook",
      "hreflang": "en",
      "type": "application/hal+json"
    }
  },
  "_embedded": {
    "product": [
      {
        "_links": {
          "self": {
            "href": "http://localhost/products",
            "hreflang": "en",
            "type": "application/hal+json"
          }
        },
        "name": "MacBook",
        "numberInStock": 10,
        "_embedded": {
          "category": {
            "_links": {
              "self": {
                "href": "http://localhost/category/index",
                "hreflang": "en"
              }
            },
            "name": "Laptops"
          }
        }
      },
      {
        "_links": {
          "self": {
            "href": "http://localhost/products",
            "hreflang": "en",
            "type": "application/hal+json"
          }
        },
        "name": "iMac",
        "numberInStock": 42,
        "_embedded": {
          "category": {
            "_links": {
              "self": {
                "href": "http://localhost/category/index",
                "hreflang": "en"
              }
            },
            "name": "Desktops"
          }
        }
      }
    ]
  }
}'''

    }
    
    @Issue('GRAILS-10533')
    void "Test customizing the embedded name for a rendered collection of domain objects" () {
        given: "A HAL Collection renderer with a custom embedded name"
            HalJsonCollectionRenderer renderer = getCollectionRenderer()
            renderer.prettyPrint = true
            renderer.collectionName = 'schtuff'

        when: "A collection of domian objects is rendered"
            def webRequest = GrailsWebUtil.bindMockWebRequest()
            webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/product/Macbook")
            def response = webRequest.response
            def renderContext = new ServletRenderContext(webRequest)
            def products = [
                new Product(name: "MacBook", numberInStock: 10, category:  new Category(name: 'Laptops')),
                new Product(name: "iMac", numberInStock: 42, category:  new Category(name: 'Desktops'))
            ]
            renderer.render(products, renderContext)

        then:"The resulting HAL is correct"
            response.contentType == GrailsWebUtil.getContentType(HalJsonRenderer.MIME_TYPE.name,
                    GrailsWebUtil.DEFAULT_ENCODING)
            response.contentAsString == '''{
  "_links": {
    "self": {
      "href": "http://localhost/product/Macbook",
      "hreflang": "en",
      "type": "application/hal+json"
    }
  },
  "_embedded": {
    "schtuff": [
      {
        "_links": {
          "self": {
            "href": "http://localhost/products",
            "hreflang": "en",
            "type": "application/hal+json"
          }
        },
        "name": "MacBook",
        "numberInStock": 10,
        "_embedded": {
          "category": {
            "_links": {
              "self": {
                "href": "http://localhost/category/index",
                "hreflang": "en"
              }
            },
            "name": "Laptops"
          }
        }
      },
      {
        "_links": {
          "self": {
            "href": "http://localhost/products",
            "hreflang": "en",
            "type": "application/hal+json"
          }
        },
        "name": "iMac",
        "numberInStock": 42,
        "_embedded": {
          "category": {
            "_links": {
              "self": {
                "href": "http://localhost/category/index",
                "hreflang": "en"
              }
            },
            "name": "Desktops"
          }
        }
      }
    ]
  }
}'''

    }

    @Issue('GRAILS-10372')
    void "Test that the HAL renderer renders JSON values correctly for simple POGOs"() {
        given:"A HAL renderer"
            HalJsonRenderer renderer = getRenderer()
            renderer.prettyPrint = true

            when:"A domain object is rendered"
            def webRequest = GrailsWebUtil.bindMockWebRequest()
            webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/product/Macbook")
            def response = webRequest.response
            def renderContext = new ServletRenderContext(webRequest)
            def product = new SimpleProduct(name: "MacBook", numberInStock: 10, category: new SimpleCategory(name: 'Laptops'))
            renderer.render(product, renderContext)

        then:"The resulting HAL is correct"
            response.contentType == GrailsWebUtil.getContentType(HalJsonRenderer.MIME_TYPE.name, GrailsWebUtil.DEFAULT_ENCODING)
            response.contentAsString == '''{
  "_links": {
    "self": {
      "href": "http://localhost/product/Macbook",
      "hreflang": "en",
      "type": "application/hal+json"
    }
  },
  "category": {
    "name": "Laptops"
  },
  "name": "MacBook",
  "numberInStock": 10
}'''

    }
    
    @Issue('GRAILS-10512')
    void "Test that the HAL renderer renders JSON values correctly for a collection of simple POGOs"() {
        given:"A HAL renderer"
            HalJsonRenderer renderer = getRenderer()
            renderer.prettyPrint = true
 
            when:"A collection of POGO is rendered"
            def webRequest = GrailsWebUtil.bindMockWebRequest()
            webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/product/Macbook")
            def response = webRequest.response
            def renderContext = new ServletRenderContext(webRequest)
            def products = [
                new SimpleProduct(name: "MacBook", numberInStock: 10, category: new SimpleCategory(name: 'Laptops')),
                new SimpleProduct(name: "iMac", numberInStock: 8, category: new SimpleCategory(name: 'Desktops'))
            ]
            renderer.render(products, renderContext)
 
        then:"The resulting HAL is correct"
            response.contentType == GrailsWebUtil.getContentType(HalJsonRenderer.MIME_TYPE.name, GrailsWebUtil.DEFAULT_ENCODING)
            response.contentAsString == '''{
  "_links": {
    "self": {
      "href": "http://localhost/product/Macbook",
      "hreflang": "en",
      "type": "application/hal+json"
    }
  },
  "_embedded": [
    {
      "_links": {
        "self": {
          "href": "http://localhost/product/Macbook",
          "hreflang": "en",
          "type": "application/hal+json"
        }
      },
      "category": {
        "name": "Laptops"
      },
      "name": "MacBook",
      "numberInStock": 10
    },
    {
      "_links": {
        "self": {
          "href": "http://localhost/product/Macbook",
          "hreflang": "en",
          "type": "application/hal+json"
        }
      },
      "category": {
        "name": "Desktops"
      },
      "name": "iMac",
      "numberInStock": 8
    }
  ]
}'''
 
    }

    @Issue('GRAILS-10499')
    @NotYetImplemented
    void "Test that the HAL rendered renders JSON values correctly for collections with repeated elements" () {
        given: "A HAL Collection renderer"
        HalJsonCollectionRenderer renderer = getCollectionRenderer()
        renderer.prettyPrint = true
        renderer.elideDuplicates = false

        when: "A collection of domian objects is rendered"
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/product/Macbook")
        def response = webRequest.response
        def renderContext = new ServletRenderContext(webRequest)
        def products = [
            new Product(name: "MacBook", numberInStock: 10, category:  new Category(name: 'Laptops')),
            new Product(name: "iMac", numberInStock: 42, category:  new Category(name: 'Desktops')),
            new Product(name: "MacBook", numberInStock: 10, category:  new Category(name: 'Laptops'))
        ]
        renderer.render(products, renderContext)

        then:"The resulting HAL is correct"
        response.contentType == GrailsWebUtil.getContentType(HalJsonRenderer.MIME_TYPE.name,
            GrailsWebUtil.DEFAULT_ENCODING)
        response.contentAsString == '''{
  "_links": {
    "self": {
      "href": "http://localhost/product/Macbook",
      "hreflang": "en",
      "type": "application/hal+json"
    }
  },
  "_embedded": {
    "product": [
      {
        "_links": {
          "self": {
            "href": "http://localhost/products",
            "hreflang": "en",
            "type": "application/hal+json"
          }
        },
        "name": "MacBook",
        "numberInStock": 10,
        "_embedded": {
          "category": {
            "_links": {
              "self": {
                "href": "http://localhost/category/index",
                "hreflang": "en"
              }
            },
            "name": "Laptops"
          }
        }
      },
      {
        "_links": {
          "self": {
            "href": "http://localhost/products",
            "hreflang": "en",
            "type": "application/hal+json"
          }
        },
        "name": "iMac",
        "numberInStock": 42,
        "_embedded": {
          "category": {
            "_links": {
              "self": {
                "href": "http://localhost/category/index",
                "hreflang": "en"
              }
            },
            "name": "Desktops"
          }
        }
      },
      {
        "_links": {
          "self": {
            "href": "http://localhost/products",
            "hreflang": "en",
            "type": "application/hal+json"
          }
        },
        "name": "MacBook",
        "numberInStock": 10,
        "_embedded": {
          "category": {
            "_links": {
              "self": {
                "href": "http://localhost/category/index",
                "hreflang": "en"
              }
            },
            "name": "Laptops"
          }
        }
      }
    ]
  }
}'''

    }

    @Issue('GRAILS-10499')
    @NotYetImplemented
    void "Test that the HAL rendered renders JSON values correctly for collections with elided elements" () {
        given: "A HAL Collection renderer"
        HalJsonCollectionRenderer renderer = getCollectionRenderer()
        renderer.prettyPrint = true

        when: "A collection of domian objects is rendered"
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/product/Macbook")
        def response = webRequest.response
        def renderContext = new ServletRenderContext(webRequest)
        def products = [
            new Product(name: "MacBook", numberInStock: 10, category:  new Category(name: 'Laptops')),
            new Product(name: "iMac", numberInStock: 42, category:  new Category(name: 'Desktops')),
            new Product(name: "MacBook", numberInStock: 10, category:  new Category(name: 'Laptops'))
        ]
        renderer.render(products, renderContext)

        then:"The resulting HAL is correct"
        renderer.elideDuplicates
        response.contentType == GrailsWebUtil.getContentType(HalJsonRenderer.MIME_TYPE.name,
            GrailsWebUtil.DEFAULT_ENCODING)
        response.contentAsString == '''{
  "_links": {
    "self": {
      "href": "http://localhost/product/Macbook",
      "hreflang": "en",
      "type": "application/hal+json"
    }
  },
  "_embedded": {
    "product": [
      {
        "_links": {
          "self": {
            "href": "http://localhost/products",
            "hreflang": "en",
            "type": "application/hal+json"
          }
        },
        "name": "MacBook",
        "numberInStock": 10,
        "_embedded": {
          "category": {
            "_links": {
              "self": {
                "href": "http://localhost/category/index",
                "hreflang": "en"
              }
            },
            "name": "Laptops"
          }
        }
      },
      {
        "_links": {
          "self": {
            "href": "http://localhost/products",
            "hreflang": "en",
            "type": "application/hal+json"
          }
        },
        "name": "iMac",
        "numberInStock": 42,
        "_embedded": {
          "category": {
            "_links": {
              "self": {
                "href": "http://localhost/category/index",
                "hreflang": "en"
              }
            },
            "name": "Desktops"
          }
        }
      },
      {
        "_links": {
          "self": {
            "href": "http://localhost/products",
            "hreflang": "en",
            "type": "application/hal+json"
          }
        },
        "name": "MacBook",
        "numberInStock": 10
      }
    ]
  }
}'''

    }
	
	@Issue('GRAILS-10781')
	void 'Test that the HAL renderer renders enums successfully for non domain classes'() {
		given: 'A HAL render'
		Renderer render = new HalJsonRenderer(Moment)
		
		when: 'A non domain is rendered'
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/moment/theFuture")
        def response = webRequest.response
        def renderContext = new ServletRenderContext(webRequest)
		def moment = new Moment(type: Moment.Category.FUTURE)
		renderer.render moment, renderContext
		
		then: 'The resulting HAL is correct'
		response.contentType == GrailsWebUtil.getContentType(HalJsonRenderer.MIME_TYPE.name, GrailsWebUtil.DEFAULT_ENCODING)
		response.contentAsString == '''{"_links":{"self":{"href":"http://localhost/moment/theFuture","hreflang":"en","type":"application/hal+json"}},"type":"FUTURE"}'''
		
	}

    @Issue('GRAILS-10372 GRAILS-10781')
    void "Test that the HAL renderer renders mixed fields (dates, enums) successfully for domains"() {
        given:"A HAL renderer"
        HalJsonRenderer renderer = getEventRenderer()
        renderer.prettyPrint = true

        when:"A domain object is rendered"
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/event/Lollapalooza")
        def response = webRequest.response
        def renderContext = new ServletRenderContext(webRequest)
        def event = new Event(name: "Lollapalooza", date: new Date(113, 10, 8, 13, 12, 30), state: Event.State.OPEN)

        renderer.render(event, renderContext)

        then:"The resulting HAL is correct"
        response.contentType == GrailsWebUtil.getContentType(HalJsonRenderer.MIME_TYPE.name, GrailsWebUtil.DEFAULT_ENCODING)
        response.contentAsString == '''{
  "_links": {
    "self": {
      "href": "http://localhost/events",
      "hreflang": "en",
      "type": "application/hal+json"
    }
  },
  "date": "2013-11-08T19:12:30Z",
  "name": "Lollapalooza",
  "state": "OPEN"
}'''

    }

    @Issue('GRAILS-10372')
    @Ignore
    void "Test that the HAL renderer allows for different date converters"() {
        given:"A HAL renderer"
        HalJsonRenderer renderer = getEventRenderer()
        renderer.prettyPrint = true
        renderer.dateToStringConverter =  new Converter<Date, String>() {
            @Override
            String convert(Date source) {
                final GregorianCalendar cal = new GregorianCalendar()
                cal.setTime(source)
                cal.setTimeZone(TimeZone.getTimeZone('America/Los_Angeles'))
                DatatypeConverter.printDateTime(cal)
            }
        }

        when:"A domain object is rendered"
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        webRequest.request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/event/Lollapalooza")
        def response = webRequest.response
        def renderContext = new ServletRenderContext(webRequest)
        def event = new Event(name: "Lollapalooza", date: new Date(113, 10, 8, 13, 12, 30), state: Event.State.OPEN)

        renderer.render(event, renderContext)

        then:"The resulting HAL is correct"
        response.contentType == GrailsWebUtil.getContentType(HalJsonRenderer.MIME_TYPE.name, GrailsWebUtil.DEFAULT_ENCODING)
        response.contentAsString == '''{
  "_links": {
    "self": {
      "href": "http://localhost/events",
      "hreflang": "en",
      "type": "application/hal+json"
    }
  },
  "date": "2013-11-08T13:12:30-08:00",
  "name": "Lollapalooza",
  "state": "OPEN"
}'''

    }

    protected HalJsonCollectionRenderer getCollectionRenderer() {
        def renderer = new HalJsonCollectionRenderer(Product)
        renderer.mappingContext = mappingContext
        renderer.messageSource = new StaticMessageSource()
        renderer.linkGenerator = getLinkGenerator {
            "/products"(resources: "product")
        }
        renderer
    }

    protected HalJsonRenderer getRenderer() {
        def renderer = new HalJsonRenderer(Product)
        renderer.mappingContext = mappingContext
        renderer.messageSource = new StaticMessageSource()
        renderer.linkGenerator = getLinkGenerator {
            "/products"(resources: "product")
        }
        renderer
    }

    protected HalJsonRenderer getEventRenderer() {
        def renderer = new HalJsonRenderer(Event)
        renderer.mappingContext = mappingContext
        renderer.messageSource = new StaticMessageSource()
        renderer.linkGenerator = getLinkGenerator {
            "/events"(resources: "event")
        }
        renderer
    }

    MappingContext getMappingContext() {
        final context = new KeyValueMappingContext("")
        context.addPersistentEntity(Product)
        context.addPersistentEntity(Category)
        context.addPersistentEntity(Event)
        return context
    }
    LinkGenerator getLinkGenerator(Closure mappings) {
        def generator = new DefaultLinkGenerator("http://localhost", null)
        generator.grailsUrlConverter = new CamelCaseUrlConverter()
        generator.urlMappingsHolder = getUrlMappingsHolder mappings
        return generator;
    }
    UrlMappingsHolder getUrlMappingsHolder(Closure mappings) {
        def evaluator = new DefaultUrlMappingEvaluator(new MockServletContext())
        def allMappings = evaluator.evaluateMappings mappings
        return new DefaultUrlMappingsHolder(allMappings)
    }
}

@Entity
class Product {
    String name
    Integer numberInStock
    Category category

    static embedded = ['category']
}

@Entity
class Category {
    String name

    @Override
    String toString() {
        name
    }

    /*
     * We need these defined for when we're checking if objects are actually written (since we're checking our
     * set 'writtenObjects' if something's there already)
     */
    boolean equals(o) {
        if (this.is(o)) {
            return true
        }
        if (getClass() != o.class) {
            return false
        }

        Category category = (Category) o

        if (name != category.name) {
            return false
        }

        return true
    }

    int hashCode() {
        return name.hashCode()
    }
}

@Entity
class Event {
    String name
    Date date
    enum State {
        OPEN, CLOSED
    }
    State state
}

class Moment {
	enum Category {
		PAST, PRESENT, FUTURE
	}
	Category type
}

class SimpleProduct {
    String name
    Integer numberInStock
    SimpleCategory category
}

class SimpleCategory {
    String name
}
