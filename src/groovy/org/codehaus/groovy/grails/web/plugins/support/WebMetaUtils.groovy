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
package org.codehaus.groovy.grails.web.plugins.support

import org.springframework.context.ApplicationContext
import org.codehaus.groovy.grails.commons.GrailsTagLibClass
import org.springframework.beans.BeanWrapperImpl
import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.springframework.web.context.request.RequestContextHolder as RCH

/**
 * Provides utility methods used to support meta-programming. In particular commons methods to
 * register tag library method invokations as new methods an a given MetaClass

 * @author Graeme Rocher
 * @since 1.0
  *
 * Created: Sep 7, 2007
 * Time: 9:06:10 AM
 *
 */
class WebMetaUtils {


    static registerMethodMissingForTags(MetaClass mc, ApplicationContext ctx, GrailsTagLibClass tagLibraryClass, String name) {
        mc."$name" = { Map attrs, Closure body ->
                def webRequest = RCH.currentRequestAttributes()
                def tagLibrary = ctx.getBean(tagLibraryClass.fullName)
                def tagBean = new BeanWrapperImpl(tagLibrary)
                def originalOut = webRequest.out
                def capturedOutput
                try {
                    capturedOutput = GroovyPage.captureTagOutput(tagLibrary, name, attrs,body, webRequest, tagBean)
                } finally {
                    webRequest.out = originalOut
                }
                capturedOutput
        }
        mc."$name" = { Map attrs, String body ->
                def webRequest = RCH.currentRequestAttributes()
                def tagLibrary = ctx.getBean(tagLibraryClass.fullName)
                def tagBean = new BeanWrapperImpl(tagLibrary)
                def originalOut = webRequest.out
                def capturedOutput
                try {
                    Closure bodyClosure = {out << body}
                    bodyClosure.delegate = tagLibrary
                    bodyClosure.resolveStrategy = Closure.DELEGATE_ONLY
                    capturedOutput = GroovyPage.captureTagOutput(tagLibrary, name, attrs,bodyClosure, webRequest, tagBean)
                } finally {
                    webRequest.out = originalOut
                }
                capturedOutput
        }
        mc."$name" = { Map attrs ->
                def webRequest = RCH.currentRequestAttributes()
                def tagLibrary = ctx.getBean(tagLibraryClass.fullName)
                def tagBean = new BeanWrapperImpl(tagLibrary)

                def originalOut = webRequest.out
                def capturedOutput
                try {
                    capturedOutput = GroovyPage.captureTagOutput(tagLibrary, name, attrs,null, webRequest, tagBean)
                } finally {
                    webRequest.out = originalOut
                }
                capturedOutput
        }
        mc."$name" = { ->
                def webRequest = RCH.currentRequestAttributes()
                def tagLibrary = ctx.getBean(tagLibraryClass.fullName)
                def tagBean = new BeanWrapperImpl(tagLibrary)

                def originalOut = webRequest.out
                def capturedOutput
                try {
                    capturedOutput = GroovyPage.captureTagOutput(tagLibrary, name, [:],null, webRequest, tagBean)
                } finally {
                    webRequest.out = originalOut
                }
                capturedOutput
        }


    }


}