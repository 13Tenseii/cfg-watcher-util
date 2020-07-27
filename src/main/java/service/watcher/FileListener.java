package service.watcher;

import java.util.EventListener;

public interface FileListener extends EventListener {
    void onChange(FileEvent fileEvent);
}
