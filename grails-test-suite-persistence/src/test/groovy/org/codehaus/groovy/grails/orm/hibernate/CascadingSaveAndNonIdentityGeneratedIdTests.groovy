package org.codehaus.groovy.grails.orm.hibernate

class CascadingSaveAndNonIdentityGeneratedIdTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
package cascadingsave

import grails.persistence.Entity

@Entity
class Request {
    static hasMany = [samples: Sample]
    
    Date dateCreated
    Date lastUpdated
    
    static mapping = {
        cache false
        id generator: 'increment'
    }
}

@Entity
class Sample {
    static belongsTo = [request : Request]
    static hasMany = [attributes: Attribute]

    static mapping = {
        cache false
        id generator: 'increment'
    }
}

@Entity
class Attribute {
    static belongsTo = [sample : Sample]

    static mapping = {
        cache false
        id generator: 'increment'
    }
}
'''
      println "Parsed!"
    }

    void testCascadingSaveToMultipleLevels() {
        def requestClass = ga.getDomainClass("cascadingsave.Request").clazz
        def sampleClass = ga.getDomainClass("cascadingsave.Sample").clazz
        def attributeClass = ga.getDomainClass("cascadingsave.Attribute").clazz
        
        def request = requestClass.newInstance()

        10.times {
            def sample = sampleClass.newInstance()
            request.addToSamples(sample)
            10.times {
                sample.addToAttributes(attributeClass.newInstance())
            }
        }
        
        request.save(flush:true)
        
        requestClass.withNewSession {
            def savedRequest = requestClass.get(request.id)
            assert savedRequest.samples.size() == 10
            savedRequest.samples.each {
                assert it.attributes.size() == 10
            }
        }
        
        
    }
}
