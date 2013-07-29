/*
 * Copyright 2012 the original author or authors.
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

package grails.util

import spock.lang.Specification

/**
 */
class BuildSettingsEnvironmentSpec extends Specification{

    void "Test that environment blocks work in BuildConfig"() {
        given:"A build settings instance with environment specific config"
            def buildSettings = new MockBuildSettings()

            buildSettings.loadConfig((Script)new GroovyClassLoader().parseClass('''
 grails.project.work.dir="default"
 environments {
    development {
        grails.project.work.dir="dev"
    }
 }
 ''' ).newInstance(),"development")

        when:"A property that was configured on a per enviornment basis is retrieved"
            final projectWorkDir = buildSettings.projectWorkDir

        then:"It is correctly configured"
            projectWorkDir.name == 'dev'
    }
}

class MockBuildSettings extends BuildSettings {

    MockBuildSettings() {
    }

    MockBuildSettings(File grailsHome) {
        super(grailsHome)
    }

    static version

    @Override
    protected void loadBuildPropertiesFromClasspath(Properties buildProps) {
        buildProps['grails.version'] = "2.3.0"
    }
}

