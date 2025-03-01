package main.java.edu.uob;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SemanticAnalyzer {
    /**
     * Validate the SQL statement in the context of current database state.
     * Throws an exception with appropriate error message if a semantic rule is violated.
     */
    public void validate(SQLStatement statement) throws Exception {
        if (statement instanceof CreateDatabaseStatement) {
            CreateDatabaseStatement stmt = (CreateDatabaseStatement) statement;
            String dbName = stmt.getDatabaseName();
            // Check if database already exists
            if (StorageManager.databaseExists(dbName)) {
                throw new Exception(ErrorHandler.databaseAlreadyExists(dbName));
            }
        } else if (statement instanceof DropDatabaseStatement) {
            DropDatabaseStatement stmt = (DropDatabaseStatement) statement;
            String dbName = stmt.getDatabaseName();
            // Check if database exists
            if (!StorageManager.databaseExists(dbName)) {
                throw new Exception(ErrorHandler.databaseNotFound(dbName));
            }
        } else if (statement instanceof UseDatabaseStatement) {
            UseDatabaseStatement stmt = (UseDatabaseStatement) statement;
            String dbName = stmt.getDatabaseName();
            if (!StorageManager.databaseExists(dbName)) {
                throw new Exception(ErrorHandler.databaseNotFound(dbName));
            }
        } else if (statement instanceof CreateTableStatement) {
            CreateTableStatement stmt = (CreateTableStatement) statement;
            String tableName = stmt.getTableName();
            // Ensure a database is selected
            if (DatabaseManager.getCurrentDatabase() == null) {
                throw new Exception(ErrorHandler.noDatabaseSelected());
            }
            // Check if table already exists
            if (StorageManager.tableExists(DatabaseManager.getCurrentDatabase(), tableName)) {
                throw new Exception(ErrorHandler.tableAlreadyExists(tableName));
            }
            // Check for duplicate column names
            List<ColumnDefinition> columns = stmt.getColumns();
            Set<String> colNames = new HashSet<>();
            for (ColumnDefinition col : columns) {
                if (colNames.contains(col.getName())) {
                    throw new Exception(ErrorHandler.duplicateColumnName(col.getName()));
                }
                colNames.add(col.getName());
            }
            // Check primary key constraints - only one primary key allowed (should be enforced by parser too)
            int pkCount = 0;
            for (ColumnDefinition col : columns) {
                if (col.isPrimaryKey()) pkCount++;
            }
            if (pkCount > 1) {
                throw new Exception(ErrorHandler.multiplePrimaryKeys());
            }
        } else if (statement instanceof DropTableStatement) {
            DropTableStatement stmt = (DropTableStatement) statement;
            String tableName = stmt.getTableName();
            if (DatabaseManager.getCurrentDatabase() == null) {
                throw new Exception(ErrorHandler.noDatabaseSelected());
            }
            if (!StorageManager.tableExists(DatabaseManager.getCurrentDatabase(), tableName)) {
                throw new Exception(ErrorHandler.tableNotFound(tableName));
            }
        } else if (statement instanceof InsertStatement) {
            InsertStatement stmt = (InsertStatement) statement;
            String tableName = stmt.getTableName();
            if (DatabaseManager.getCurrentDatabase() == null) {
                throw new Exception(ErrorHandler.noDatabaseSelected());
            }
            if (!StorageManager.tableExists(DatabaseManager.getCurrentDatabase(), tableName)) {
                throw new Exception(ErrorHandler.tableNotFound(tableName));
            }
            // Check column count matches table definition
            List<String> values = stmt.getValues();
            List<ColumnDefinition> columns = StorageManager.readTableSchema(DatabaseManager.getCurrentDatabase(), tableName);
            if (columns == null) {
                throw new Exception(ErrorHandler.tableNotFound(tableName));
            }
            if (values.size() != columns.size()) {
                throw new Exception(ErrorHandler.columnCountMismatch());
            }
            // Check primary key uniqueness
            // If table has a primary key column, ensure the value for that column is not already in the table
            int pkIndex = -1;
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).isPrimaryKey()) {
                    pkIndex = i;
                    break;
                }
            }
            if (pkIndex != -1) {
                String newPkValue = values.get(pkIndex);
                // read all existing records and check the pk column
                List<List<String>> records = StorageManager.readTableRecords(DatabaseManager.getCurrentDatabase(), tableName);
                for (List<String> record : records) {
                    if (record.size() > pkIndex && record.get(pkIndex).equals(newPkValue)) {
                        throw new Exception(ErrorHandler.duplicatePrimaryKeyValue(newPkValue));
                    }
                }
            }
            // Optionally, check type matching (e.g., if INT columns have numeric values)
            for (int i = 0; i < columns.size(); i++) {
                ColumnDefinition col = columns.get(i);
                String value = values.get(i);
                if (col.getType().toUpperCase().startsWith("INT")) {
                    // if defined as INT, check that value is numeric
                    try {
                        Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        throw new Exception(ErrorHandler.typeMismatch(col.getName(), col.getType()));
                    }
                }
            }
        } else if (statement instanceof SelectStatement) {
            SelectStatement stmt = (SelectStatement) statement;
            String tableName = stmt.getTableName();
            if (DatabaseManager.getCurrentDatabase() == null) {
                throw new Exception(ErrorHandler.noDatabaseSelected());
            }
            if (!StorageManager.tableExists(DatabaseManager.getCurrentDatabase(), tableName)) {
                throw new Exception(ErrorHandler.tableNotFound(tableName));
            }
            // Check that requested columns in select exist in table (unless select all)
            List<ColumnDefinition> columns = StorageManager.readTableSchema(DatabaseManager.getCurrentDatabase(), tableName);
            if (columns == null) {
                throw new Exception(ErrorHandler.tableNotFound(tableName));
            }
            List<String> selectColumns = stmt.getColumns();
            if (selectColumns != null) {
                for (String colName : selectColumns) {
                    boolean found = false;
                    for (ColumnDefinition colDef : columns) {
                        if (colDef.getName().equals(colName)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new Exception(ErrorHandler.columnNotFound(colName));
                    }
                }
            }
            // Check where clause column exists
            if (stmt.getWhereColumn() != null) {
                boolean found = false;
                for (ColumnDefinition colDef : columns) {
                    if (colDef.getName().equals(stmt.getWhereColumn())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new Exception(ErrorHandler.columnNotFound(stmt.getWhereColumn()));
                }
            }
        } else if (statement instanceof UpdateStatement) {
            UpdateStatement stmt = (UpdateStatement) statement;
            String tableName = stmt.getTableName();
            if (DatabaseManager.getCurrentDatabase() == null) {
                throw new Exception(ErrorHandler.noDatabaseSelected());
            }
            if (!StorageManager.tableExists(DatabaseManager.getCurrentDatabase(), tableName)) {
                throw new Exception(ErrorHandler.tableNotFound(tableName));
            }
            // Check that all assignment columns exist and where column exists
            List<ColumnDefinition> columns = StorageManager.readTableSchema(DatabaseManager.getCurrentDatabase(), tableName);
            if (columns == null) {
                throw new Exception(ErrorHandler.tableNotFound(tableName));
            }
            // Build set of column names
            Set<String> colNames = new HashSet<>();
            for (ColumnDefinition col : columns) {
                colNames.add(col.getName());
            }
            for (String colName : stmt.getAssignments().keySet()) {
                if (!colNames.contains(colName)) {
                    throw new Exception(ErrorHandler.columnNotFound(colName));
                }
            }
            if (stmt.getWhereColumn() != null && !colNames.contains(stmt.getWhereColumn())) {
                throw new Exception(ErrorHandler.columnNotFound(stmt.getWhereColumn()));
            }
            // If updating a primary key column, ensure not creating duplicates
            int pkIndex = -1;
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).isPrimaryKey()) {
                    pkIndex = i;
                    break;
                }
            }
            if (pkIndex != -1 && stmt.getAssignments().containsKey(columns.get(pkIndex).getName())) {
                // If PK is being updated, ensure new value isn't already present (except possibly in the same row)
                String newPkValue = stmt.getAssignments().get(columns.get(pkIndex).getName());
                List<List<String>> records = StorageManager.readTableRecords(DatabaseManager.getCurrentDatabase(), tableName);
                for (List<String> record : records) {
                    // If where clause is specified, maybe skip rows not matching where?
                    // But easier: semantic check just sees if any other row has the new PK.
                    if (record.size() > pkIndex && record.get(pkIndex).equals(newPkValue)) {
                        // If where specified and this record is the one to be updated, skip it:
                        if (stmt.getWhereColumn() != null) {
                            // find index of whereColumn in columns list
                            int whereIndex = -1;
                            for (int i = 0; i < columns.size(); i++) {
                                if (columns.get(i).getName().equals(stmt.getWhereColumn())) {
                                    whereIndex = i;
                                    break;
                                }
                            }
                            if (whereIndex != -1 && record.get(whereIndex).equals(stmt.getWhereValue())) {
                                // This is the record that will be updated, skip checking itself
                                continue;
                            }
                        }
                        throw new Exception(ErrorHandler.duplicatePrimaryKeyValue(newPkValue));
                    }
                }
            }
            // Check type of assignments similar to insert
            for (String colName : stmt.getAssignments().keySet()) {
                // find col definition
                for (ColumnDefinition col : columns) {
                    if (col.getName().equals(colName)) {
                        if (col.getType().toUpperCase().startsWith("INT")) {
                            String val = stmt.getAssignments().get(colName);
                            try {
                                Integer.parseInt(val);
                            } catch (NumberFormatException e) {
                                throw new Exception(ErrorHandler.typeMismatch(col.getName(), col.getType()));
                            }
                        }
                    }
                }
            }
        } else if (statement instanceof DeleteStatement) {
            DeleteStatement stmt = (DeleteStatement) statement;
            String tableName = stmt.getTableName();
            if (DatabaseManager.getCurrentDatabase() == null) {
                throw new Exception(ErrorHandler.noDatabaseSelected());
            }
            if (!StorageManager.tableExists(DatabaseManager.getCurrentDatabase(), tableName)) {
                throw new Exception(ErrorHandler.tableNotFound(tableName));
            }
            // Check where column exists
            List<ColumnDefinition> columns = StorageManager.readTableSchema(DatabaseManager.getCurrentDatabase(), tableName);
            if (columns == null) {
                throw new Exception(ErrorHandler.tableNotFound(tableName));
            }
            if (stmt.getWhereColumn() != null) {
                boolean found = false;
                for (ColumnDefinition col : columns) {
                    if (col.getName().equals(stmt.getWhereColumn())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new Exception(ErrorHandler.columnNotFound(stmt.getWhereColumn()));
                }
            }
        }
    }
}
