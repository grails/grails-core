/* Copyright (C) 2011 SpringSource
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

package org.codehaus.groovy.grails.orm.hibernate.cfg;

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.springframework.datastore.mapping.model.AbstractMappingContext;
import org.springframework.datastore.mapping.model.MappingConfigurationStrategy;
import org.springframework.datastore.mapping.model.MappingFactory;
import org.springframework.datastore.mapping.model.PersistentEntity;

/**
 * A MappingContext that adapts the Grails domain model to the Mapping API
 *
 * @author Graeme Rocher
 * @since 1.0
 *
 */
public class GrailsDomainClassMappingContext extends AbstractMappingContext {

	private GrailsApplication grailsApplication;

	public GrailsDomainClassMappingContext(GrailsApplication grailsApplication) {
		super();
		this.grailsApplication = grailsApplication;

		final GrailsClass[] artefacts = grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE);
		for (GrailsClass grailsClass : artefacts) {
			addPersistentEntity(grailsClass.getClazz());
		}
	}

	public GrailsApplication getGrailsApplication() {
		return grailsApplication;
	}

	public MappingConfigurationStrategy getMappingSyntaxStrategy() {
		throw new UnsupportedOperationException("MappingConfigurationStrategy not supported by implementation. Defined by Grails itself.");
	}

	public MappingFactory getMappingFactory() {
		throw new UnsupportedOperationException("MappingFactory not supported by implementation. Defined by Grails itself.");
	}

	@Override
	protected PersistentEntity createPersistentEntity(Class javaClass) {

		GrailsDomainClass domainClass = (GrailsDomainClass) grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, javaClass.getName());
		return new GrailsDomainClassPersistentEntity(domainClass, this);
	}

}
