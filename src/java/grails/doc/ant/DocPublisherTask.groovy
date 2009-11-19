/* Copyright 2004-2005 the original author or authors.
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

package grails.doc.ant

import org.apache.tools.ant.Task
import grails.doc.DocPublisher
import org.apache.tools.ant.BuildException

/**
 * An ant task for using the DocEngine
 *
 * @author Graeme Rocher
 * @since 1.2
 */

public class DocPublisherTask extends Task {

    DocPublisher publisher = new DocPublisher()

    /**
     * The documentation source location
     */
    void setSrc(File src) {
        publisher.src = src
    }

    /**
     * The documentation publishing destination
     */
    void setDest(File dest) {
        publisher.target = dest
    }


    /**
     * The encoding to use (default is UTF-8)
     */
    void setEncoding(String encoding) {
        publisher.encoding = encoding
    }

    /**
     * The directory of the style templates (optional)
     */
    void setStyleDir(File styleDir) {
        publisher.style = styleDir
    }

    /**
     * The directory of the css templates (optional)
     */
    void setCssDir(File cssDir) {
        publisher.css = cssDir
    }
    /**
     * The directory of the images (optional)
     */
    void setImagesDir(File imagesDir) {
        publisher.images = imagesDir
    }

    /**
     * The temporary directory to use (optional)
     */
    void setWorkDir(File workDir) {
        publisher.workDir = workDir
    }

    /**
     * A properties file containing the title, author etc.
     */
    void setProperties(File propsFile) {
       if(!propsFile?.exists()) throw new BuildException("Documentation properties file [${propsFile?.absolutePath}] doesn't exit")

       def props = new Properties()
       propsFile.withInputStream { props.load(it) }

       setProperties(props)
    }
    /**
     * A properties containing the title, author etc.
     */
    void setProperties(Properties props) {
        publisher.engineProperties = props

        def metaProps = DocPublisher.metaClass.properties
        for (MetaProperty mp in metaProps) {
            if (mp.type == String) {
                def value = props[mp.name]
                if(value) {
                    publisher[mp.name] = value
                }
            }
        }
    }

    public void execute() {
        publisher.ant = new AntBuilder(this)
        publisher.publish()
    }


}