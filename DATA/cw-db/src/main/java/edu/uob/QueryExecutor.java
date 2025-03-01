package main.java.edu.uob;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class QueryExecutor {
    /**
     * Execute a SELECT statement and return the result as a string.
     */
    public static String executeSelect(SelectStatement stmt) {
        String tableName = stmt.getTableName();
        List<String> selectColumns = stmt.getColumns(); // null means all columns
        String whereCol = stmt.getWhereColumn();
        String whereVal = stmt.getWhereValue();
        // Read table schema and records
        List<ColumnDefinition> schema = StorageManager.readTableSchema(DatabaseManager.getCurrentDatabase(), tableName);
        List<List<String>> records = StorageManager.readTableRecords(DatabaseManager.getCurrentDatabase(), tableName);
        // Filter records if where clause present
        List<List<String>> resultRecords = new ArrayList<>();
        int whereIndex = -1;
        if (whereCol != null) {
            // find index of whereCol in schema
            for (int i = 0; i < schema.size(); i++) {
                if (schema.get(i).getName().equals(whereCol)) {
                    whereIndex = i;
                    break;
                }
            }
        }
        for (List<String> record : records) {
            if (whereIndex != -1) {
                if (record.size() > whereIndex && record.get(whereIndex).equals(whereVal)) {
                    resultRecords.add(record);
                }
            } else {
                // no where clause, include all
                resultRecords.add(record);
            }
        }
        if (resultRecords.isEmpty()) {
            return "Empty set.";
        }
        // Determine which columns to output
        List<Integer> colIndexes;
        if (selectColumns == null || selectColumns.isEmpty()) {
            // all columns
            colIndexes = new ArrayList<>();
            for (int i = 0; i < schema.size(); i++) {
                colIndexes.add(i);
            }
        } else {
            colIndexes = new ArrayList<>();
            for (String colName : selectColumns) {
                for (int i = 0; i < schema.size(); i++) {
                    if (schema.get(i).getName().equals(colName)) {
                        colIndexes.add(i);
                    }
                }
            }
        }
        // Build output string
        StringBuilder output = new StringBuilder();
        // Header row with column names (if selecting multiple columns or all columns)
        if (colIndexes.size() > 1 || (selectColumns == null && colIndexes.size() == 1)) {
            for (int j = 0; j < colIndexes.size(); j++) {
                output.append(schema.get(colIndexes.get(j)).getName());
                if (j < colIndexes.size() - 1) {
                    output.append(" | ");
                }
            }
            output.append("\n");
        }
        // Data rows
        for (int r = 0; r < resultRecords.size(); r++) {
            List<String> record = resultRecords.get(r);
            for (int j = 0; j < colIndexes.size(); j++) {
                int idx = colIndexes.get(j);
                String value = "";
                if (idx < record.size()) {
                    value = record.get(idx);
                }
                output.append(value);
                if (j < colIndexes.size() - 1) {
                    output.append(" | ");
                }
            }
            if (r < resultRecords.size() - 1) {
                output.append("\n");
            }
        }
        return output.toString();
    }

    /**
     * Execute an INSERT statement and return result message.
     */
    public static String executeInsert(InsertStatement stmt) {
        String tableName = stmt.getTableName();
        List<String> values = stmt.getValues();
        // Build a single line for the row data separated by commas
        String row = String.join(", ", values);
        boolean success = StorageManager.insertRow(DatabaseManager.getCurrentDatabase(), tableName, row);
        if (!success) {
            // if insertion failed due to I/O or missing table
            return ErrorHandler.generalError("Failed to insert row.");
        }
        return "1 row inserted.";
    }

    /**
     * Execute an UPDATE statement and return result message.
     */
    public static String executeUpdate(UpdateStatement stmt) {
        String tableName = stmt.getTableName();
        String whereCol = stmt.getWhereColumn();
        String whereVal = stmt.getWhereValue();
        Map<String, String> assignments = stmt.getAssignments();
        // Read current records
        List<ColumnDefinition> schema = StorageManager.readTableSchema(DatabaseManager.getCurrentDatabase(), tableName);
        List<List<String>> records = StorageManager.readTableRecords(DatabaseManager.getCurrentDatabase(), tableName);
        // Determine indices for columns to update and where clause
        Map<String, Integer> colIndexMap = new java.util.HashMap<>();
        for (int i = 0; i < schema.size(); i++) {
            colIndexMap.put(schema.get(i).getName(), i);
        }
        Integer whereIndex = null;
        if (whereCol != null) {
            whereIndex = colIndexMap.get(whereCol);
        }
        // Apply updates
        int updateCount = 0;
        for (List<String> record : records) {
            if (whereIndex == null || (whereIndex < record.size() && record.get(whereIndex).equals(whereVal))) {
                // For each assignment, update the corresponding value in record
                for (Map.Entry<String, String> assign : assignments.entrySet()) {
                    String colName = assign.getKey();
                    String newValue = assign.getValue();
                    int idx = colIndexMap.get(colName);
                    if (idx >= record.size()) {
                        // Extend list if needed (shouldn't normally happen if data is consistent)
                        while (record.size() <= idx) {
                            record.add("");
                        }
                    }
                    record.set(idx, newValue);
                }
                updateCount++;
            }
        }
        // Write back all records to file
        boolean success = StorageManager.writeTableRecords(DatabaseManager.getCurrentDatabase(), tableName, schema, records);
        if (!success) {
            return ErrorHandler.generalError("Failed to update rows.");
        }
        return updateCount + " row(s) updated.";
    }

    /**
     * Execute a DELETE statement and return result message.
     */
    public static String executeDelete(DeleteStatement stmt) {
        String tableName = stmt.getTableName();
        String whereCol = stmt.getWhereColumn();
        String whereVal = stmt.getWhereValue();
        // Read current records
        List<ColumnDefinition> schema = StorageManager.readTableSchema(DatabaseManager.getCurrentDatabase(), tableName);
        List<List<String>> records = StorageManager.readTableRecords(DatabaseManager.getCurrentDatabase(), tableName);
        // Determine index for where column if any
        int whereIndex = -1;
        if (whereCol != null) {
            for (int i = 0; i < schema.size(); i++) {
                if (schema.get(i).getName().equals(whereCol)) {
                    whereIndex = i;
                    break;
                }
            }
        }
        // Filter out records that match the WHERE clause
        Iterator<List<String>> iterator = records.iterator();
        int deleteCount = 0;
        while (iterator.hasNext()) {
            List<String> record = iterator.next();
            if (whereIndex == -1 || (whereIndex < record.size() && record.get(whereIndex).equals(whereVal))) {
                iterator.remove();
                deleteCount++;
            }
        }
        // Write back remaining records
        boolean success = StorageManager.writeTableRecords(DatabaseManager.getCurrentDatabase(), tableName, schema, records);
        if (!success) {
            return ErrorHandler.generalError("Failed to delete rows.");
        }
        return deleteCount + " row(s) deleted.";
    }
}
