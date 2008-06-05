/* Copyright 2004-2005 Graeme Rocher
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
package grails.util;

import junit.framework.TestCase;
import org.codehaus.groovy.grails.commons.GrailsApplication;

/**
 * Tests for the GrailsUtils class
 *
 * @author Graeme Rocher
 * @since 0.4
 *        <p/>
 *        Created: Jan 22, 2007
 *        Time: 6:21:53 PM
 */
public class GrailsUtilTests extends TestCase {

    public void testEnvironment() {

        System.setProperty(GrailsApplication.ENVIRONMENT, "");
        assertEquals(GrailsApplication.ENV_DEVELOPMENT, GrailsUtil.getEnvironment());

        System.setProperty(GrailsApplication.ENVIRONMENT, "prod");
        assertEquals(GrailsApplication.ENV_PRODUCTION, GrailsUtil.getEnvironment());

        System.setProperty(GrailsApplication.ENVIRONMENT, "dev");
        assertEquals(GrailsApplication.ENV_DEVELOPMENT, GrailsUtil.getEnvironment());

        System.setProperty(GrailsApplication.ENVIRONMENT, "test");
        assertEquals(GrailsApplication.ENV_TEST, GrailsUtil.getEnvironment());

        System.setProperty(GrailsApplication.ENVIRONMENT, "myenvironment");
        assertEquals("myenvironment", GrailsUtil.getEnvironment());        
    }

    public void testGrailsVersion() {

        assertEquals("1.0.3", GrailsUtil.getGrailsVersion());
    }

    protected void tearDown() throws Exception {
        System.setProperty(GrailsApplication.ENVIRONMENT, "");
    }
}
