/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.commons;

import grails.util.GrailsUtil;

/**
 * Static singleton holder for the GrailsApplication instance.
 *
 * @author Marc Palmer (marc@anyware.co.uk)
 *
 * @deprecated Use dependency injection or implement GrailsApplicationAware instead
 */
@Deprecated
public abstract class ApplicationHolder {

    private static GrailsApplication application;

    /**
     * @return The GrailsApplication instance
     * @deprecated Use dependency injection instead
     */
    @Deprecated
    public static GrailsApplication getApplication() {
        GrailsUtil.deprecated("Method ApplicationHolder.getApplication() is deprecated and will be removed in a future version of Grails.");
        return application;
    }

    /**
     *
     * @param application The application to set
     * @deprecated Use dependency injection instead
     */
    @Deprecated
    public static void setApplication(GrailsApplication application) {
        GrailsUtil.deprecated("Method ApplicationHolder.setApplication(application) is deprecated and will be removed in a future version of Grails.");
        ApplicationHolder.application = application;
    }
}
