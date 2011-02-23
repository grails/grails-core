/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.plugins.web.taglib.CountryTagLib
import org.springframework.web.servlet.support.RequestContextUtils as RCU

class CountryTagLibTests extends AbstractGrailsTagTests {

    void testFullCountryListWithSelection() {
        def template = '<g:countrySelect name="foo" value="gbr" />'

        def result = applyTemplate(template, [:])

        assertResultContains result, '<option value="gbr" selected="selected" >United Kingdom</option>'

        CountryTagLib.ISO3166_3.each {
            assertResultContains result, "<option value=\"${it.key}\""
            assertResultContains result, ">${it.value.encodeAsHTML()}</option>"
        }
    }

    void testReducedCountryListWithSelection() {
        def template = '<g:countrySelect name="foo" value="usa" from="[\'gbr\', \'usa\', \'deu\']"/>'
        def result = applyTemplate(template, [:])

        assertResultContains result, '<option value="usa" selected="selected" >United States</option>'

        ['gbr', 'usa', 'deu'].each {
            def value = CountryTagLib.ISO3166_3[it]
            assertResultContains result, "<option value=\"${it}\""
            assertResultContains result, ">${value.encodeAsHTML()}</option>"
        }
    }

    void testCountryNamesWithValueMessagePrefix() {
        // Prepare the custom message source.
        def msgPrefix = "country"
        def codeMap = [gbr: "Royaume Uni", usa: "Les Etats Unis", deu: "Allemagne"]
        codeMap.each { code, val ->
            messageSource.addMessage(msgPrefix + "." + code, RCU.getLocale(request), val)
        }

        // Execute the template.
        def template = "<g:countrySelect name=\"foo\" valueMessagePrefix=\"${msgPrefix}\" value=\"usa\" from=\"['gbr', 'usa', 'deu']\"/>".toString()
        def result = applyTemplate(template, [:])


        assertResultContains result, "<option value=\"usa\" selected=\"selected\" >${codeMap['usa']}</option>"

        codeMap.each { code, val ->
            assertResultContains result, "<option value=\"${code}\""
            assertResultContains result, ">${val}</option>"
        }
    }

    void testDefault() {
        def template = '<g:countrySelect name="foo" default="deu" from="[\'gbr\', \'usa\', \'deu\']"/>'
        def result = applyTemplate(template, [:])

        assertResultContains result, '<option value="deu" selected="selected" >Germany</option>'

        ['gbr', 'usa', 'deu'].each {
            def value = CountryTagLib.ISO3166_3[it]
            assertResultContains result, "<option value=\"${it}\""
            assertResultContains result, ">${value.encodeAsHTML()}</option>"
        }
    }

    void testCountryDisplay() {
        def template = '<g:country code="deu"/>'

        assertOutputContains('Germany', template,[:])
    }

    void assertResultContains(result, expectedSubstring) {
        assertTrue "Result does not contain expected string [$expectedSubstring]. Result was: ${result}", result.indexOf(expectedSubstring) > -1
    }
}
