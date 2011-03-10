/*
 * Copyright 2011 SpringSource
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

package org.codehaus.groovy.grails.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to watch directories for changes
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class DirectoryWatcher extends Thread {

    private File[] directories;
    private String[] extensions;
    private List<FileChangeListener> listeners = new ArrayList<FileChangeListener>();

    private Map<File, Long> lastModifiedMap = new ConcurrentHashMap<File, Long>();
    private Map<File, Long> directoryWatch = new ConcurrentHashMap<File, Long>();
    private boolean active = true;
    private long sleepTime = 3000;

    public DirectoryWatcher(File[] directories, String[] extensions) {
        this.directories = directories;
        this.extensions = extensions;
        setDaemon(true);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setSleepTime(long sleepTime) {
        this.sleepTime = sleepTime;
    }

    private void initializeLastModifiedCache(File[] directories, final String[] extensions) {

        for (File directory : directories) {
            cacheFilesForDirectory(directory, extensions, false);
        }

    }

    private void cacheFilesForDirectory(File directory, String[] extensions, boolean fireEvent) {
        directoryWatch.put(directory, directory.lastModified());
        File[] files = directory.listFiles();
        for (File file : files) {
            if(isValidFileToMonitor(file.getName(), extensions)) {
                if(!lastModifiedMap.containsKey(file) && fireEvent) {
                    fireOnNew(file);
                }
                lastModifiedMap.put(file, file.lastModified());
            }
            else if(file.isDirectory()) {
               cacheFilesForDirectory(file, extensions, fireEvent);
            }
        }
    }

    private void fireOnNew(File file) {
        for (FileChangeListener listener : listeners) {
            listener.onNew(file);
        }
    }

    private boolean isValidFileToMonitor(String name, String[] extensions) {
        for (String extension : extensions) {
            if (name.endsWith(extension)) return true;
        }
        return false;
    }

    @Override
    public void run() {
        initializeLastModifiedCache(directories, extensions);
        int count = 0;
        while(active) {
            Set<File> files = lastModifiedMap.keySet();
            for (File file : files) {
                long currentLastModified = file.lastModified();
                Long cachedTime = lastModifiedMap.get(file);
                if(currentLastModified > cachedTime) {
                    lastModifiedMap.put(file, currentLastModified);
                    fireOnChange(file);
                }
            }
            try {
                count++;
                if(count > 2) {
                    count = 0;
                    checkForNewFiles();
                }
                sleep(sleepTime);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private void fireOnChange(File file) {
        for (FileChangeListener listener : listeners) {
            listener.onChange(file);
        }
    }

    private void checkForNewFiles() {
        for (File directory : directoryWatch.keySet()) {
            final Long currentTimestamp = directoryWatch.get(directory);

            if(currentTimestamp < directory.lastModified()) {
                cacheFilesForDirectory(directory, extensions, true);
            }

        }
    }


    public void addListener(FileChangeListener listener ) {
        this.listeners.add(listener);
    }

    public static interface FileChangeListener {
        public abstract void onChange(File file);
        public abstract void onNew(File file);
    }
}
