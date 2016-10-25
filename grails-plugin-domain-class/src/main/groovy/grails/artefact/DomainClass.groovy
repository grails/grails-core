/*
 * Copyright 2014 the original author or authors.
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
package grails.artefact

import grails.core.GrailsDomainClass
import grails.util.Holders
import grails.validation.Constrained
import groovy.transform.CompileStatic
import org.grails.core.artefact.DomainClassArtefactHandler

/**
 *
 * A trait implemented by all domain classes
 *
 * @author Jeff Brown
 * @author Graeme Rocher
 *
 * @since 3.0
 *
 */
@CompileStatic
trait DomainClass {


    static Map<String, Constrained> getConstrainedProperties() {
        GrailsDomainClass domainClass = (GrailsDomainClass)Holders?.grailsApplication?.getArtefact(DomainClassArtefactHandler.TYPE, this.name)
        def constrainedProperties = domainClass?.getConstrainedProperties()
        if(constrainedProperties) {
            return constrainedProperties
        }
        else {
            return Collections.<String,Constrained>emptyMap()
        }
    }
}
