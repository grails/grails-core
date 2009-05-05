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
 * WITHOUT c;pWARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.plugins.web.taglib.CountryTagLib

class CountryTagLibTests extends AbstractGrailsTagTests {

    void testFullCountryListWithSelection() {
        def template = '<g:countrySelect name="foo" value="gbr" />'

        def result = applyTemplate(template, [:])
        CountryTagLib.ISO3166_3.each {
            def expected = "<option value=\"${it.key}\""
            assertTrue "Output does not contain expected string [$expected]. Output was: ${result}", result.indexOf(expected) > -1
            expected = ">${it.value.encodeAsHTML()}</option>"
            assertTrue "Output does not contain expected string [$expected]. Output was: ${result}", result.indexOf(expected) > -1
        }
    }

    void testReducedCountryListWithSelection() {
        def template = '<g:countrySelect name="foo" value="usa" from="[\'gbr\', \'usa\', \'deu\']"/>'

        assertOutputContains('<option value="usa" selected="selected" >United States</option>', template,[:])

        ['gbr', 'usa', 'deu'].each {
            def value = CountryTagLib.ISO3166_3[it]
            assertOutputContains("<option value=\"${it}\"", template,[:])
            assertOutputContains(">${value.encodeAsHTML()}</option>", template,[:])
        }
    }

    void testCountryNamesWithValueMessagePrefix() {
        def template = '<g:countrySelect name="foo" valueMessagePrefix="country" value="usa" from="[\'gbr\', \'usa\', \'deu\']"/>'

        assertOutputContains('<option value="usa" selected="selected" >country.usa</option>', template,[:])

        ['gbr', 'usa', 'deu'].each {
            def value = CountryTagLib.ISO3166_3[it]
            assertOutputContains("<option value=\"${it}\"", template,[:])
            assertOutputContains(">country.${it.encodeAsHTML()}</option>", template,[:])
        }
    }

    void testDefault() {
        def template = '<g:countrySelect name="foo" default="deu" from="[\'gbr\', \'usa\', \'deu\']"/>'

        assertOutputContains('<option value="deu" selected="selected" >Germany</option>', template,[:])

        ['gbr', 'usa', 'deu'].each {
            def value = CountryTagLib.ISO3166_3[it]
            assertOutputContains("<option value=\"${it}\"", template,[:])
            assertOutputContains(">${value.encodeAsHTML()}</option>", template,[:])
        }
    }

    void testCountryDisplay() {
        def template = '<g:country code="deu"/>'

        assertOutputContains('Germany', template,[:])

    }
}