/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.compiler.gorm;

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.grails.datastore.gorm.GormInstanceApi;
import org.grails.datastore.gorm.GormStaticApi;

import java.net.URL;

/**
 * Transforms GORM entities making the GORM API available to Java
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@AstTransformer
public class GormTransformer extends AbstractGrailsArtefactTransformer {

    @Override
    public String getArtefactType() {
        return DomainClassArtefactHandler.TYPE;
    }

    @Override
    public Class getInstanceImplementation() {
        return GormInstanceApi.class;
    }

    @Override
    public Class getStaticImplementation() {
        return GormStaticApi.class;
    }

    @Override
    protected boolean requiresStaticLookupMethod() {
        return true;
    }

    public boolean shouldInject(URL url) {
        return GrailsResourceUtils.isDomainClass(url);
    }
}
