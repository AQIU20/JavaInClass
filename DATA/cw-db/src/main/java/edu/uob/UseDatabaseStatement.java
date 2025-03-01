package main.java.edu.uob;

public class UseDatabaseStatement implements SQLStatement {
    private final String databaseName;

    public UseDatabaseStatement(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseName() {
        return databaseName;
    }
}
