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
