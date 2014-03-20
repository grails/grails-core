package org.codehaus.groovy.grails.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a {@link AbstractDirectoryWatcher} that uses {@link java.nio.WatchService}.
 * This implementation is used for Java 7 and later.
 * @author Craig Andrews
 * @since 2.4
 * @see WatchServiceDirectoryWatcher
 * @see DirectoryWatcher
 */
class WatchServiceDirectoryWatcher extends AbstractDirectoryWatcher {

	private static final Logger LOG = LoggerFactory.getLogger(WatchServiceDirectoryWatcher.class);
    private Map<WatchKey, List<String>> watchKeyToExtensionsMap = new ConcurrentHashMap<WatchKey, List<String>>();
    private Set<Path> individualWatchedFiles = new HashSet<Path>();

	private final WatchService watchService;

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

	public WatchServiceDirectoryWatcher(){
		try {
			watchService = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void run() {
        while (active) {
			try {
				WatchKey watchKey = watchService.poll(sleepTime, TimeUnit.MILLISECONDS);
				if(watchKey!=null){
					List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
					for(WatchEvent<?> watchEvent : watchEvents){
						WatchEvent.Kind<?> kind = watchEvent.kind();
		                if (kind == StandardWatchEventKinds.OVERFLOW) {
		                	// TODO how is this supposed to be handled? I think the best thing to do is ignore it, but I'm not positive
		                	LOG.warn("WatchService Overflow occurred");
		                    continue;
		                }
						WatchEvent<Path> pathWatchEvent = cast(watchEvent);
						Path name = pathWatchEvent.context();
						Path dir = (Path) watchKey.watchable();
						Path child = dir.resolve(name).toAbsolutePath();
						File childFile = child.toFile();
						if(individualWatchedFiles.contains(child)){
							if(kind == StandardWatchEventKinds.ENTRY_CREATE){
								fireOnNew(childFile);
							}else if(kind == StandardWatchEventKinds.ENTRY_MODIFY){
								fireOnChange(childFile);
							}else if(kind == StandardWatchEventKinds.ENTRY_DELETE){
								// do nothing... there's no way to communicate deletions
							}
						}else{
							List<String> fileExtensions = watchKeyToExtensionsMap.get(watchKey);
							if(fileExtensions==null){
								throw new IllegalStateException("No file extensions list found for path not being watched");
							}
							if(kind==StandardWatchEventKinds.ENTRY_CREATE){
								// new directory created, so watch its contents
								addWatchDirectory(child,fileExtensions);
							}
							if(isValidFileToMonitor(childFile,fileExtensions)){
								if(kind == StandardWatchEventKinds.ENTRY_CREATE){
									fireOnNew(childFile);
								}else if(kind == StandardWatchEventKinds.ENTRY_MODIFY){
									fireOnChange(childFile);
								}else if(kind == StandardWatchEventKinds.ENTRY_DELETE){
									// do nothing... there's no way to communicate deletions
								}
							}
						}
					}
					watchKey.reset();
				}
			} catch (InterruptedException e) {
				// ignore
			}
        }
        try {
			watchService.close();
		} catch (IOException e) {
			LOG.debug("Exception while closing watchService", e);
		}
	}

	@Override
	public void addWatchFile(File fileToWatch) {
		if(!isValidFileToMonitor(fileToWatch, Arrays.asList("*"))) return;
		try {
			Path pathToWatch = fileToWatch.toPath().toAbsolutePath();
			individualWatchedFiles.add(pathToWatch);
			pathToWatch.getParent().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void addWatchDirectory(File dir, final List<String> fileExtensions) {
		Path dirPath = dir.toPath();
		addWatchDirectory(dirPath, fileExtensions);
	}

	private void addWatchDirectory(Path dir, final List<String> fileExtensions) {
		if(!isValidDirectoryToMonitor(dir.toFile())){
			return;
		}
		try {
			//add the subdirectories too
			Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
	            @Override
	            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
	                throws IOException
	            {
	            	if(!isValidDirectoryToMonitor(dir.toFile())){
	            		return FileVisitResult.SKIP_SUBTREE;
	            	}
	    			WatchKey watchKey = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
	    			watchKeyToExtensionsMap.put(watchKey, fileExtensions);
	                return FileVisitResult.CONTINUE;
	            }
	        });
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
