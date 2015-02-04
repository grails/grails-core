/*
 * Copyright 2015 original authors
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
package org.grails.plugins.web.interceptors

import grails.config.Settings
import grails.core.GrailsClass
import grails.plugins.Plugin
import grails.util.GrailsUtil
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic


/**
 * A plugin for interceptors
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class InterceptorsGrailsPlugin extends Plugin {
    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [controllers:version, urlMappings:version]

    @Override
    @CompileDynamic
    Closure doWithSpring() {
        {->
            GrailsClass[] interceptors = grailsApplication.getArtefacts(InterceptorArtefactHandler.TYPE)
            if(interceptors.length == 0) return

            grailsInterceptorHandlerInterceptorAdapter(GrailsInterceptorHandlerInterceptorAdapter)

            def enableJsessionId = config.getProperty(Settings.GRAILS_VIEWS_ENABLE_JSESSIONID, Boolean, false)
            def gspEnc = config.getProperty(Settings.GSP_VIEW_ENCODING, "")
            for(GrailsClass i in interceptors) {
                "${i.propertyName}"(i.clazz) {
                    if (gspEnc.trim()) {
                        gspEncoding = gspEnc
                    }
                    if (enableJsessionId) {
                        useJessionId = enableJsessionId
                    }
                }
            }
        }
    }
}
