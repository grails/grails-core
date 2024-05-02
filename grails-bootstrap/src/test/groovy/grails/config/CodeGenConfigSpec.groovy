/*
 * Copyright 2024 original authors
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
package grails.config

import org.grails.config.CodeGenConfig
import spock.lang.Specification

class CodeGenConfigSpec extends Specification {

    def "should support converting to boolean simple type"() {
        given:
        CodeGenConfig config = new CodeGenConfig()
        config.loadYml(new ByteArrayInputStream(yml.bytes))

        when:
        boolean val = config.getProperty('foo', boolean)

        then:
        val == expected

        where:
        yml          | expected
        'foo: true'  | true
        'foo: false' | false
        ''           | false
    }
}
