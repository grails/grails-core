/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.grails.config

import org.grails.core.exceptions.GrailsConfigurationException
import spock.lang.Specification

class CompositeConfigSpec extends Specification {

    void 'first config takes precedence and last config supplies missing values'() {
        given:
        def config = new CompositeConfig()
        config.addLast(new PrefixedConfig('scope', new PropertySourcesConfig([scope: [shared: 'middle', fallback: 42]])))
        config.addLast(new PrefixedConfig('scope', new PropertySourcesConfig([scope: [shared: 'last', lastOnly: 'last']])))
        config.addFirst(new PrefixedConfig('scope', new PropertySourcesConfig([scope: [shared: 'first', firstOnly: 'first']])))

        expect:
        config.getAt('shared') == 'first'
        config.get('shared') == 'first'
        config.getProperty('shared') == 'first'
        config.getProperty('fallback', Integer) == 42
        config.getAt('lastOnly') == 'last'
        config.getAt('firstOnly') == 'first'
        config.getAt('missing') == null
        config.get('missing') == null
        config.getProperty('missing', Integer) == null
        config.getProperty('missing', Integer, 7) == 7
        config.getProperty('missing', 'default') == 'default'
    }

    void 'navigate searches nested paths across configs'() {
        given:
        def config = new CompositeConfig()
        config.addFirst(new PropertySourcesConfig([service: [host: 'first']]))
        config.addLast(new PropertySourcesConfig([service: [host: 'last', port: 8080]]))

        expect:
        config.navigate('service', 'host') == 'first'
        config.navigate('service', 'port') == 8080
        config.navigate('service', 'missing') == null
    }

    void 'flatten and properties combine configs in insertion order'() {
        given:
        def config = new CompositeConfig()
        config.addFirst(new PropertySourcesConfig([shared: 'first', firstOnly: 'one']))
        config.addLast(new PropertySourcesConfig([shared: 'last', lastOnly: 'two']))

        expect:
        config.flatten() == [shared: 'last', firstOnly: 'one', lastOnly: 'two']
        config.toProperties() == [shared: 'last', firstOnly: 'one', lastOnly: 'two'] as Properties
        config.getProperty('shared') == 'first'
    }

    void 'map views include every config and deduplicate keys'() {
        given:
        def first = new PropertySourcesConfig([shared: 'first', firstOnly: 'one'])
        def last = new PropertySourcesConfig([shared: 'last', lastOnly: 'two'])
        def config = new CompositeConfig()
        config.addFirst(first)
        config.addLast(last)

        expect:
        config.size() == first.size() + last.size()
        !config.isEmpty()
        config.keySet() == ['shared', 'firstOnly', 'lastOnly'] as Set
        config.containsKey('firstOnly')
        config.containsKey('lastOnly')
        !config.containsKey('missing')
        config.containsProperty('shared')
        !config.containsProperty('missing')
        config.containsValue('first')
        config.containsValue('last')
        !config.containsValue('missing')
        config.values().toSet() == ['first', 'last', 'one', 'two'] as Set
        config.entrySet() == (first.entrySet() + last.entrySet()) as Set
        config.iterator().toList().toSet() == config.entrySet()
    }

    void 'empty composites and empty delegates have empty views'() {
        given:
        def config = new CompositeConfig()
        config.addLast(new PropertySourcesConfig())

        expect:
        config.isEmpty()
        config.size() == 0
        config.keySet().isEmpty()
        !config.iterator().hasNext()
        config.flatten().isEmpty()
        config.toProperties().isEmpty()
    }

    void 'allowed values accept configured and default values'() {
        given:
        def config = new CompositeConfig()
        config.addLast(new PropertySourcesConfig([mode: 'enabled']))

        expect:
        config.getProperty('mode', String, 'disabled', ['enabled', 'disabled']) == 'enabled'
        config.getProperty('missing', String, 'disabled', ['enabled', 'disabled']) == 'disabled'
    }

    void 'invalid allowed values throw a configuration exception'() {
        given:
        def config = new CompositeConfig()
        config.addLast(new PropertySourcesConfig([mode: 'invalid']))

        when:
        config.getProperty('mode', String, 'disabled', ['enabled', 'disabled'])

        then:
        thrown(GrailsConfigurationException)
    }

    void 'required properties return values and blank strings use defaults'() {
        given:
        def config = new CompositeConfig()
        config.addFirst(new PropertySourcesConfig([name: 'Grails', count: 2, blank: '  ']))

        expect:
        config.getRequiredProperty('name') == 'Grails'
        config.getRequiredProperty('count', Integer) == 2
        config.getProperty('blank', 'default') == 'default'
    }

    void 'missing required properties are rejected'() {
        given:
        def config = new CompositeConfig()

        when:
        operation(config)

        then:
        thrown(IllegalStateException)

        where:
        operation << [
            { it.getRequiredProperty('missing') },
            { it.getRequiredProperty('missing', Integer) }
        ]
    }

    void 'modification and placeholder resolution are unsupported'() {
        given:
        def config = new CompositeConfig()
        config.addLast(new PropertySourcesConfig([name: 'original']))

        when:
        operation(config)

        then:
        thrown(UnsupportedOperationException)
        config.getProperty('name') == 'original'

        where:
        operation << [
            { it.merge([name: 'changed']) },
            { it.setAt('name', 'changed') },
            { it.put('name', 'changed') },
            { it.remove('name') },
            { it.putAll([name: 'changed']) },
            { it.clear() },
            { it.resolvePlaceholders('${name}') },
            { it.resolveRequiredPlaceholders('${name}') }
        ]
    }
}
