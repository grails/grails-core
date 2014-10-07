package org.grails.io.watch

import groovy.transform.CompileStatic

/**
 * A {@link org.grails.io.watch.DirectoryWatcher.FileChangeListener} that only fires for specific extension or list of extensions
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
abstract class FileExtensionFileChangeListener implements DirectoryWatcher.FileChangeListener {

    final List<String> extensions

    FileExtensionFileChangeListener(List<String> extensions) {
        this.extensions = extensions
    }

    @Override
    final void onChange(File file) {
        if(file && extensions.any() { String ext -> file.name.endsWith( ext ) } ) {
            onChange(file, extensions)
        }
    }

    abstract void onChange(File file, List<String> extensions)

    @Override
    final void onNew(File file) {
        if(file && extensions.any() { String ext -> file.name.endsWith( ext ) }  ) {
            onNew(file, extensions)
        }
    }

    abstract void onNew(File file, List<String> extensions)
}
