package org.grails.spring.context

import org.grails.spring.context.support.ReloadableResourceBundleMessageSource
import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.core.io.Resource
import spock.lang.Specification

class ResourceBundleMessageSourceSpec extends Specification {
    File resourceFolder
    
    void setup(){
        resourceFolder = File.createTempDir()
        def messages = new File(resourceFolder,'messages.properties')
        messages.text = '''\
            foo=bar
        '''.stripIndent()
        def other = new File(resourceFolder,'other.properties')
        other.text = '''\
            bar=foo
        '''.stripIndent()
    }
    
    void cleanup(){
        resourceFolder.deleteDir()
    }
    
    
    void 'Check method to retrieve bundle codes per messagebundle'(){
        given:
            def messageSource = new ReloadableResourceBundleMessageSource(
                resourceLoader: new TempResourceLoader(resourceFolder:resourceFolder)
            )
            messageSource.setBasenames('messages','other')
            def locale = Locale.default
        expect:
            messageSource.getBundleCodes(locale,'messages') == ['foo']
            messageSource.getBundleCodes(locale,'other') == ['bar']
            messageSource.getBundleCodes(locale,'messages','other') == ['foo','bar']
    }
    
}

class TempResourceLoader extends FileSystemResourceLoader{
    File resourceFolder
    
    @Override
    protected Resource getResourceByPath(String path) {
        path = new File(resourceFolder,path).absolutePath
        return super.getResourceByPath(path)
    }
}

