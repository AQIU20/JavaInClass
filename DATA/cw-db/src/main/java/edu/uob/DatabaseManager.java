package main.java.edu.uob;

import java.io.File;

public class DatabaseManager {
    private static String currentDatabase = null;

    public static String getCurrentDatabase() {
        return currentDatabase;
    }

    /**
     * Create a new database (directory).
     */
    public static String createDatabase(String name) {
        boolean created = StorageManager.createDatabase(name);
        if (!created) {
            return ErrorHandler.databaseAlreadyExists(name);
        }
        return "Database " + name + " created.";
    }

    /**
     * Drop (delete) an existing database.
     */
    public static String dropDatabase(String name) {
        boolean deleted = StorageManager.deleteDatabase(name);
        if (!deleted) {
            return ErrorHandler.databaseNotFound(name);
        }
        // If the current database was this one, clear current selection
        if (name.equalsIgnoreCase(currentDatabase)) {
            currentDatabase = null;
        }
        return "Database " + name + " deleted.";
    }

    /**
     * Use (switch to) a database as current.
     */
    public static String useDatabase(String name) {
        if (!StorageManager.databaseExists(name)) {
            return ErrorHandler.databaseNotFound(name);
        }
        currentDatabase = name;
        return "Using database " + name + ".";
    }
}
