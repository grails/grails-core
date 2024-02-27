package org.grails.databinding.xml

import grails.databinding.DataBindingSource
import groovy.xml.XmlSlurper
import spock.lang.Specification

class GPathCollectionDataBindingSourceSpec extends Specification {

    void 'Test multiple child elements'() {
        given:
        def xml = new XmlSlurper().parseText('''
<list>
    <person>
        <firstName>Peter</firstName>
        <lastName>Gabriel</lastName>
    </person>
    <person>
        <firstName>Tony</firstName>
        <lastName>Banks</lastName>
    </person>
    <person>
        <firstName>Steve</firstName>
        <lastName>Hackett</lastName>
    </person>
</list>
''')
        when:
        def source = new GPathResultCollectionDataBindingSource(xml)
        def dataBindingSources = source.dataBindingSources

        then:
        dataBindingSources.size() == 3
        dataBindingSources[0] instanceof DataBindingSource
        dataBindingSources[0]['firstName'] == 'Peter'
        dataBindingSources[0]['lastName'] == 'Gabriel'
        dataBindingSources[1] instanceof DataBindingSource
        dataBindingSources[1]['firstName'] == 'Tony'
        dataBindingSources[1]['lastName'] == 'Banks'
        dataBindingSources[2] instanceof DataBindingSource
        dataBindingSources[2]['firstName'] == 'Steve'
        dataBindingSources[2]['lastName'] == 'Hackett'
    }
}
