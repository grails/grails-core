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
package org.grails.cli

import org.grails.build.parsing.ScriptNameResolver
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertTrue

class ScriptNameResolverTests {

    @Test
    void testFoo() {
        assertTrue ScriptNameResolver.resolvesTo('F', 'Foo')
        assertTrue ScriptNameResolver.resolvesTo('FB', 'FooBar')
        assertTrue ScriptNameResolver.resolvesTo('FoB', 'FooBar')
        assertTrue ScriptNameResolver.resolvesTo('FBa', 'FooBar')
        assertTrue ScriptNameResolver.resolvesTo('FoBa', 'FooBar')
        assertTrue ScriptNameResolver.resolvesTo('FooBar', 'FooBar')
        assertTrue !ScriptNameResolver.resolvesTo('FB', 'FooBarZoo')
        assertTrue !ScriptNameResolver.resolvesTo('FBaz', 'FooBar')
        assertTrue !ScriptNameResolver.resolvesTo('FBr', 'FooBar')
        assertTrue !ScriptNameResolver.resolvesTo('F', 'FooBar')
        assertTrue !ScriptNameResolver.resolvesTo('Fo', 'FooBar')
        assertTrue !ScriptNameResolver.resolvesTo('Foo', 'FooBar')
    }
}
