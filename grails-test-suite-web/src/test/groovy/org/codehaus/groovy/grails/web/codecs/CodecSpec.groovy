package org.codehaus.groovy.grails.web.codecs

import grails.test.mixin.TestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin

import org.springframework.web.util.WebUtils

import spock.lang.Specification

/**
 * Tests the behavior of the include tag
 */
@TestMixin(GroovyPageUnitTestMixin)
class CodecSpec extends Specification {
    // TODO: separate tag codec from scriplet codec to it's own setting
    // TODO: applyCodec should have an option to make everything safe at the end
    
    
    void "output should be safe at the end"() {
        
    }
    
        
    void "detailed test document showing a GSP + Taglib that uses a default HTML codec but also writes out JS data inline in the GSP, and writes out JS data inline using a call to a TagLib, and a call to a tag that renders pre-escaped HTML content, and so on"() {
        
    }
    
    // opionated setting
    void "tag output must not be automatically encoded."() {
        // TODO: problem with out << body() ? 
    }
    
    /*
     * static defaultEncodeAs = 'raw' // default encodeAs applied to all tags in this taglib class
     * static encodeAsForTags = [someTag: 'html'] // default encodeAs for a single tag in this taglib class
     * the codec setting accepts a string or a map. (explained in https://github.com/grails/grails-core/blob/scb-encoding-support/grails-web/src/main/groovy/org/codehaus/groovy/grails/web/util/WithCodecHelper.groovy#L54) 
     */
    void "tag call as function call should use defaultEncodeAs / encodeAsForTags settings"() {
        
    }

    void "scriptlets should apply outCodec"() {
        // Behaviour is inconsistent. <% ... %> and <%= ... %> do not apply default codec. ${g.xxx([:])} does apply codec. <g:xxx/> does not apply codec.
        // Change <% ... %> and <%= ... %> to apply current default codec, as currently this is a little known security hole. 
    }
    
    void "double encoding should be prevented"() {
        // There is a risk of double-encoding of data when the developer is not aware of encodings already applied.
        
    }
    
    void "Plugins cannot have their pages break because the app developer changes default codec setting."() {
        
    }
    
    void "Ideally the user should never need to explicitly think about codecs or calling them except in rare situations."() {
        
    }
    
    void "Add a function/tag to switch the current default codec - effectively pushing and popping a default codec stack. This could take the form of a withCodec(name, Closure) method in tags."() {
        
    }
    
    void "Use this function/tag in core tags like <g:javascript> and <r:script> to automatically set an appropriate codec"() {
        
    }
    
    void "<g:render> and similar tags would need to set default codec to HTML again when including another GSP, pushing whatever was default onto a stack"() {
        
    }
    
    void "Add support for an optional encodeAs attribute to all tags automatically, such that the result will be encoded with that codec if specified i.e. var s = \${g.createLink(...., encodeAs:'JavaScript')}"() {
        
    }
    
    void "All GSPs in app or plugins default to HTML codec unless developer does something to change that using directive/tag"() {
        
    }
    
    void "All outputs of expressions/inline code apply the current default codec"() {
        
    }
    
    void "Tags are responsible for the correct encoding of their output, unless specified in encodeAs= attribute"() {
        
    }
    
    void "It's possible to use raw codec to mark some output as something that shouldn't be escaped"() {
        
    }
    
    void "support map argument to encodeAs attribute so that templateCodec, pageCodec & defaultCode can be changed separately"() {
        
    }
}
