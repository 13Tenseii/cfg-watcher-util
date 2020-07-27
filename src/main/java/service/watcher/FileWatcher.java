package service.watcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;

public class FileWatcher implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(FileWatcher.class);

    private final File file;
    private final FileListener fileListener;
    private final Long intervalMillis;

    public FileWatcher(String filePath, Long intervalMillis, FileListener fileListener) throws FileNotFoundException {
        this.file = new File(filePath);
        if (!file.exists() || file.isDirectory())
            throw new FileNotFoundException("Smpp config file "+ filePath + " is not found!");
        this.intervalMillis = intervalMillis;
        this.fileListener = fileListener;
    }

    public void watch() {
        if (file.exists()) {
            var thread = new Thread(this);
            thread.start();
        }
    }

    @Override
    public void run() {
        WatchService watchService = null;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            var path = Paths.get(file.getAbsolutePath());
            var dir = path.getParent();
            dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
            var isPolling = true;
            while (isPolling) {
                isPolling = pollEvents(watchService, path.getFileName());
                if (intervalMillis != null && intervalMillis > 0)
                    Thread.sleep(intervalMillis);
            }
        } catch (IOException | InterruptedException | ClosedWatchServiceException exc) {
            logger.error("Error running watcher", exc);
            Thread.currentThread().interrupt();
        } finally {
            closeWatchService(watchService);
        }
    }

    private boolean pollEvents(WatchService watchService, Path fileName) throws InterruptedException {
        var watchKey = watchService.take();
        var path = (Path) watchKey.watchable();
        watchKey.pollEvents()
                .forEach(it -> {
                    if (((Path)it.context()).endsWith(fileName))
                        notifyListeners(it.kind(), path.resolve((Path) it.context()).toFile());
                });

        return watchKey.reset();
    }

    private void notifyListeners(WatchEvent.Kind kind, File file) {
        var fileEvent = new FileEvent(file);
        if (kind == StandardWatchEventKinds.ENTRY_MODIFY
                || kind == StandardWatchEventKinds.ENTRY_CREATE) {
            fileListener.onChange(fileEvent);
        }
    }

    private void closeWatchService(WatchService watchService) {
        try {
            if (watchService != null)
                watchService.close();
        } catch (IOException exc) {logger.error("Error closing watcher");}
    }
}
