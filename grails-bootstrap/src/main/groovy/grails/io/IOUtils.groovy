/*
 * Copyright 2002-2013 the original author or authors.
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
package grails.io

import grails.util.BuildSettings
import groovy.transform.CompileStatic
import org.grails.io.support.Resource
import org.grails.io.support.SpringIOUtils
import org.grails.io.support.UrlResource

/**
 * Utility methods for performing I/O operations.
 *
 * @author Graeme Rocher
 * @since 2.4
 */
@CompileStatic
class IOUtils extends SpringIOUtils {

    private static String applicationDirectory

    /**
     * Gracefully opens a stream for a file, throwing exceptions where appropriate. Based off the commons-io method
     *
     * @param file The file
     * @return The stream
     */
    static BufferedInputStream openStream(File file)  {
        if(!file.exists()) {
            throw new FileNotFoundException("File $file does not exist")
        }
        else {
            if ( file.directory ) {
                throw new IOException("File $file exists but is a directory")
            }
            else if ( !file.canRead() ) {
                throw new IOException("File $file cannot be read")
            }
            else {
                file.newInputStream()
            }
        }
    }

    /**
     * Convert a reader to a String, reading the data from the reader
     * @param reader The reader
     * @return The string
     */
    static String toString(Reader reader) {
        def writer = new StringWriter()
        copy reader, writer
        writer.toString()
    }

    /**
     * Convert a stream to a String, reading the data from the stream
     * @param stream The stream
     * @return The string
     */
    static String toString(InputStream stream, String encoding = null) {
        def writer = new StringWriter()
        copy stream, writer, encoding
        writer.toString()
    }

    /**
     * Copy an InputStream to the given writer with the given encoding
     * @param input The input
     * @param output The writer
     * @param encoding The encoding
     */
    static void copy(InputStream input, Writer output, String encoding = null) {
        def reader = encoding ? new InputStreamReader(input, encoding) : new InputStreamReader(input)
        copy(reader, output)
    }

    /**
     * Finds a JAR file for the given class
     * @param targetClass The target class
     * @return The JAR file
     */
    static File findJarFile(Class targetClass) {
        def resource = findClassResource(targetClass)
        findJarFile(resource)
    }

    static File findJarFile(URL resource) {
        def absolutePath = resource?.getPath()
        if (absolutePath) {
            final jarPath = absolutePath.substring("file:".length(), absolutePath.lastIndexOf("!"))
            new File(jarPath)
        }
    }

    /**
     * Returns the URL resource for the location on disk of the given class or null if it cannot be found
     *
     * @param targetClass The target class
     * @return The URL to class file or null
     */
    static URL findClassResource(Class targetClass) {
        targetClass.getResource('/' + targetClass.name.replace(".", "/") + ".class")
    }

    /**
     * Finds a URL within a JAR relative (from the root) to the given class
     * @param targetClass
     * @param path
     * @return
     */
    static URL findResourceRelativeToClass(Class targetClass, String path) {
        def pathToClassFile = '/' + targetClass.name.replace(".", "/") + ".class"
        def classRes = targetClass.getResource(pathToClassFile)
        if(classRes) {
            def rootPath = classRes.toString() - pathToClassFile
            if(rootPath.endsWith(BuildSettings.BUILD_CLASSES_PATH)) {
                rootPath = rootPath.replace('/build/classes/', '/build/resources/')
            }
            return new URL("$rootPath$path")
        }
        return null
    }

    public static File findApplicationDirectoryFile() {
        def directory = findApplicationDirectory()
        if(directory) {
            def f = new File(directory)
            if(f.exists()) {

                return f
            }
        }
        return null
    }

    public static String findApplicationDirectory() {
        if(applicationDirectory) {
            return applicationDirectory
        }

        String location = null
        try {
            String mainClassName = System.getProperty(BuildSettings.MAIN_CLASS_NAME)
            if(!mainClassName) {
                def stackTraceElements = Thread.currentThread().getStackTrace()
                if(stackTraceElements) {
                    def lastElement = stackTraceElements[-1]
                    def className = lastElement.className
                    def methodName = lastElement.methodName
                    if(className.endsWith(".Application") && methodName == '<clinit>') {
                        mainClassName = className
                    }
                }
            }
            if(mainClassName) {

                final Class<?> mainClass = Thread.currentThread().contextClassLoader.loadClass(mainClassName)
                final URL classResource = mainClass ? findClassResource(mainClass) : null
                if(classResource) {
                    def file = new UrlResource(classResource).getFile()
                    def path = file.canonicalPath
                    if(path.contains(BuildSettings.BUILD_CLASSES_PATH)) {
                        location = path.substring(0, path.indexOf(BuildSettings.BUILD_CLASSES_PATH) - 1)
                    }
                }
            }

        } catch (ClassNotFoundException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }
        applicationDirectory = location
        return location;
    }
}
