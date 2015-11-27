package grails.config

import org.grails.config.PropertySourcesConfig
import spock.lang.Specification

/**
 * Created by graemerocher on 25/11/15.
 */
class ConfigPropertiesSpec extends Specification {

    void "Test config properties"() {
        when:"a config object"
        def config = new PropertySourcesConfig('foo.bar':'foo', 'foo.two': 2)
        def props = new ConfigProperties(config)
        then:
        props.getProperty('foo.bar') == 'foo'
        props.get('foo.bar') == 'foo'
        props.getProperty('foo.two') == '2'
        props.get('foo.two') == '2'
        props.get('foo') == null
        props.getProperty('foo') == null
        props.propertyNames().hasMoreElements()
        props.propertyNames().toList() == ['foo.bar','foo','foo.two']
    }
}
