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
package org.codehaus.groovy.grails.compiler.web;

import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.plugins.web.api.ControllersApi;

import java.net.URL;
import java.util.regex.Pattern;

/**
 * Enhances controller classes with the appropriate API at compile time.
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@AstTransformer
public class ControllerTransformer extends AbstractGrailsArtefactTransformer{

    public static Pattern CONTROLLER_PATTERN = Pattern.compile(".+/" +
             GrailsResourceUtils.GRAILS_APP_DIR + "/controllers/(.+)Controller\\.groovy");

    @Override
    public Class<?> getInstanceImplementation() {
        return ControllersApi.class;
    }

    @Override
    public Class<?> getStaticImplementation() {
        return null;  // No static api
    }

    public boolean shouldInject(URL url) {
        return url != null && CONTROLLER_PATTERN.matcher(url.getFile()).find();
    }
}
