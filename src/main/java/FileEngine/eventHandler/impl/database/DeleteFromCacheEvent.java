package FileEngine.eventHandler.impl.database;

public class DeleteFromCacheEvent extends DatabaseEvent {

    public DeleteFromCacheEvent(String path) {
        super(path);
    }
}