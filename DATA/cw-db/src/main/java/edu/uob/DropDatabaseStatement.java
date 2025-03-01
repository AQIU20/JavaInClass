package main.java.edu.uob;

public class DropDatabaseStatement implements SQLStatement {
    private final String databaseName;

    public DropDatabaseStatement(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseName() {
        return databaseName;
    }
}
