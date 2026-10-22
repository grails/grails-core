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

import org.grails.config.yaml.YamlPropertySourceLoader
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.io.ByteArrayResource
import spock.lang.Issue
import spock.lang.Specification

import java.lang.reflect.Field

/**
 * @author Iván López
 */
class SystemEnvironmentConfigSpec extends Specification {

    private static final String YAML = '''
property.with.period: from-yml
property_with_underscore: from-yml
property-with-hyphen: from-yml
property-with_mixed.symbols: from-yml
'''

    @Issue('#10670')
    void 'configuration properties defined in SystemEnvironment take precedence'() {
        given: 'a value in the system environment'
        modifiableSystemEnvironment.put(systemProperty, value)

        and: 'default configuration in application-yml'
        def config = configFor(YAML)

        expect:
        config.getProperty(propertyToFind) == value

        cleanup:
        modifiableSystemEnvironment.remove(systemProperty)

        where:
        propertyToFind                | systemProperty                | value
        'property.with.period'        | 'property.with.period'        | 'from-env'
        'property.with.period'        | 'property_with_period'        | 'from-env'
        'property.with.period'        | 'PROPERTY_WITH_PERIOD'        | 'from-env'
        'property_with_underscore'    | 'property_with_underscore'    | 'from-env'
        'property_with_underscore'    | 'PROPERTY_WITH_UNDERSCORE'    | 'from-env'
        'property-with-hyphen'        | 'property-with-hyphen'        | 'from-env'
        'property-with-hyphen'        | 'PROPERTY-WITH-HYPHEN'        | 'from-env'
        'property-with-hyphen'        | 'PROPERTY_WITH_HYPHEN'        | 'from-env'
        'property-with_mixed.symbols' | 'property-with_mixed.symbols' | 'from-env'
        'property-with_mixed.symbols' | 'property_with_mixed_symbols' | 'from-env'
        'property-with_mixed.symbols' | 'PROPERTY-WITH_MIXED.SYMBOLS' | 'from-env'
        'property-with_mixed.symbols' | 'PROPERTY_WITH_MIXED_SYMBOLS' | 'from-env'
    }

    void 'a key that no environment variable can match resolves from configuration'() {
        given: 'configuration with no matching variable in the environment'
        def config = configFor(YAML)

        expect: 'the configured value is returned'
        config.getProperty('property.with.period') == 'from-yml'
        config.getProperty('property-with-hyphen') == 'from-yml'

        and: 'a key that is configured nowhere falls back to the default'
        config.getProperty('property.nobody.set', String, 'fallback') == 'fallback'
    }

    void 'a variable that matches the key in none of the probed spellings does not override it'() {
        given: 'a lower-case variable for a mixed-case key'
        modifiableSystemEnvironment.put('property_with_period', 'from-env')
        def config = configFor('Property.With.Period: from-yml')

        expect: 'only the key as written and its upper-cased form are probed, as Spring does'
        config.getProperty('Property.With.Period') == 'from-yml'

        cleanup:
        modifiableSystemEnvironment.remove('property_with_period')
    }

    void 'when several variables match a key the first probed spelling wins'() {
        given: 'two variables that both match the key'
        modifiableSystemEnvironment.put(winner, 'from-winner')
        modifiableSystemEnvironment.put(loser, 'from-loser')
        def config = configFor(YAML)

        expect: 'the value of the spelling that is probed first is returned'
        config.getProperty('property.with.period') == 'from-winner'

        cleanup:
        modifiableSystemEnvironment.remove(winner)
        modifiableSystemEnvironment.remove(loser)

        where:
        winner                 | loser
        'property.with.period' | 'PROPERTY_WITH_PERIOD'
        'property_with_period' | 'PROPERTY_WITH_PERIOD'
        'property_with_period' | 'PROPERTY.WITH.PERIOD'
    }

    void 'a property read more than once keeps resolving to the system environment value'() {
        given: 'configuration that is overridden by the environment'
        modifiableSystemEnvironment.put('PROPERTY_WITH_PERIOD', 'from-env')
        def config = configFor(YAML)

        expect: 'every read resolves to the environment value, not just the first'
        config.getProperty('property.with.period') == 'from-env'
        config.getProperty('property.with.period') == 'from-env'
        config.getProperty('property.with.period') == 'from-env'

        cleanup:
        modifiableSystemEnvironment.remove('PROPERTY_WITH_PERIOD')
    }

    void 'a property that has already been read still reflects later configuration changes'() {
        given: 'a property that has been read once'
        def config = configFor('some.nested.value: original')
        assert config.getProperty('some.nested.value') == 'original'

        when: 'the configuration is changed'
        config.merge(['some.nested.value': 'updated'])

        then: 'the new value is returned rather than the previously resolved one'
        config.getProperty('some.nested.value') == 'updated'
    }

    void 'each config observes the environment as it stands when the config is created'() {
        given: 'a config created before the variable exists'
        def before = configFor('late.bound.property: from-yml')
        assert before.getProperty('late.bound.property') == 'from-yml'

        when: 'the variable is installed and a new config is created'
        modifiableSystemEnvironment.put('LATE_BOUND_PROPERTY', 'from-env')
        def after = configFor('late.bound.property: from-yml')

        then: 'the new config resolves to the environment value'
        after.getProperty('late.bound.property') == 'from-env'

        and: 'the earlier config keeps the environment it was created with'
        before.getProperty('late.bound.property') == 'from-yml'

        cleanup:
        modifiableSystemEnvironment.remove('LATE_BOUND_PROPERTY')
    }

    private static PropertySourcesConfig configFor(String yaml) {
        def yamlPropertiesSource = new YamlPropertySourceLoader()
                .load('application.yml', new ByteArrayResource(yaml.bytes, 'test.yml'), null)
        def propertySources = new MutablePropertySources()
        propertySources.addFirst(yamlPropertiesSource.first())
        new PropertySourcesConfig(propertySources)
    }

    // From https://github.com/spring-projects/spring-framework/blob/4.3.x/spring-core/src/test/java/org/springframework/core/env/StandardEnvironmentTests.java#L492
    @SuppressWarnings("unchecked")
    static Map<String, String> getModifiableSystemEnvironment() {
        // for os x / linux
        Class<?>[] classes = Collections.class.getDeclaredClasses();
        Map<String, String> env = System.getenv();
        for (Class<?> cl : classes) {
            if ('java.util.Collections$UnmodifiableMap'.equals(cl.getName())) {
                try {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    if (obj != null && obj.getClass().getName().equals('java.lang.ProcessEnvironment$StringEnvironment')) {
                        return (Map<String, String>) obj;
                    }
                }
                catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        // for windows
        Class<?> processEnvironmentClass;
        try {
            processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        try {
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Object obj = theCaseInsensitiveEnvironmentField.get(null);
            return (Map<String, String>) obj;
        }
        catch (NoSuchFieldException ex) {
            // do nothing
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        try {
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Object obj = theEnvironmentField.get(null);
            return (Map<String, String>) obj;
        }
        catch (NoSuchFieldException ex) {
            // do nothing
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        throw new IllegalStateException();
    }
}
