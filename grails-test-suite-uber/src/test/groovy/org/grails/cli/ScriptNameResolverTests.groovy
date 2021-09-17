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
