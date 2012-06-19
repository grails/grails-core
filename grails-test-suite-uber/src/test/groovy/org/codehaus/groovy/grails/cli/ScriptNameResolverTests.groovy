package org.codehaus.groovy.grails.cli

class ScriptNameResolverTests extends GroovyTestCase {
    void testFoo() {
        assert ScriptNameResolver.resolvesTo('F', 'Foo')
        assert ScriptNameResolver.resolvesTo('FB', 'FooBar')
        assert ScriptNameResolver.resolvesTo('FoB', 'FooBar')
        assert ScriptNameResolver.resolvesTo('FBa', 'FooBar')
        assert ScriptNameResolver.resolvesTo('FoBa', 'FooBar')
        assert ScriptNameResolver.resolvesTo('FooBar', 'FooBar')
        assert !ScriptNameResolver.resolvesTo('FB', 'FooBarZoo')
        assert !ScriptNameResolver.resolvesTo('FBaz', 'FooBar')
        assert !ScriptNameResolver.resolvesTo('FBr', 'FooBar')
        assert !ScriptNameResolver.resolvesTo('F', 'FooBar')
        assert !ScriptNameResolver.resolvesTo('Fo', 'FooBar')
        assert !ScriptNameResolver.resolvesTo('Foo', 'FooBar')
    }
}
