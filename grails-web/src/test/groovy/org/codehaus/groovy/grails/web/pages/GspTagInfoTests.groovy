package org.codehaus.groovy.grails.web.pages


class GspTagInfoTests extends GroovyTestCase {

    //TODO: where to put test gsp?
    void _testFileConstructor() {
        GspTagInfo info = new GspTagInfo(new File('./test/unit/grails-app/./taglib/tagpackage/testTag.gsp'))
        assert info.tagName == 'testTag'
        assert info.packageName == 'tagpackage'
        assert info.tagLibName == '_TestTagGspTagLib'
        assert info.tagLibFileName == '_TestTagGspTagLib.groovy'
        assert info.tagLibFQCN == 'tagpackage._TestTagGspTagLib'
        assert info.text == 'dummy'
    }

    void testArgsConstructor() {
        GspTagInfo info = new GspTagInfo('testTag', 'tagpackage', 'dummy')
        assert info.tagName == 'testTag'
        assert info.packageName == 'tagpackage'
        assert info.tagLibName == '_TestTagGspTagLib'
        assert info.tagLibFileName == '_TestTagGspTagLib.groovy'
        assert info.tagLibFQCN == 'tagpackage._TestTagGspTagLib'
        assert info.text == 'dummy'
    }
}
