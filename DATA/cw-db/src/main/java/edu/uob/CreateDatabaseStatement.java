package main.java.edu.uob;

public class CreateDatabaseStatement implements SQLStatement {
    private final String databaseName;

    public CreateDatabaseStatement(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseName() {
        return databaseName;
    }
}
