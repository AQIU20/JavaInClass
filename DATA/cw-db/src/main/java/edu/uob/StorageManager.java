package main.java.edu.uob;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StorageManager {
    private static final String BASE_PATH = "."; // use current directory as base for storing databases

    public static boolean databaseExists(String dbName) {
        File dbDir = new File(BASE_PATH, dbName);
        return dbDir.exists() && dbDir.isDirectory();
    }

    public static boolean createDatabase(String dbName) {
        File dbDir = new File(BASE_PATH, dbName);
        if (dbDir.exists()) {
            return false;
        }
        return dbDir.mkdir();
    }

    public static boolean deleteDatabase(String dbName) {
        File dbDir = new File(BASE_PATH, dbName);
        if (!dbDir.exists() || !dbDir.isDirectory()) {
            return false;
        }
        // Delete all files in the directory
        File[] files = dbDir.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
        // Delete the directory itself
        return dbDir.delete();
    }

    public static boolean tableExists(String dbName, String tableName) {
        if (dbName == null) return false;
        File tableFile = new File(new File(BASE_PATH, dbName), tableName + ".txt");
        return tableFile.exists() && tableFile.isFile();
    }

    public static boolean createTable(String dbName, String tableName, String schemaLine) {
        if (dbName == null) return false;
        File tableFile = new File(new File(BASE_PATH, dbName), tableName + ".txt");
        if (tableFile.exists()) {
            return false;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile))) {
            writer.write(schemaLine);
            writer.newLine();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean deleteTable(String dbName, String tableName) {
        if (dbName == null) return false;
        File tableFile = new File(new File(BASE_PATH, dbName), tableName + ".txt");
        if (!tableFile.exists()) {
            return false;
        }
        return tableFile.delete();
    }

    /**
     * Read the schema (column definitions) of a table from its file.
     */
    public static List<ColumnDefinition> readTableSchema(String dbName, String tableName) {
        File tableFile = new File(new File(BASE_PATH, dbName), tableName + ".txt");
        if (!tableFile.exists()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(tableFile))) {
            String schemaLine = reader.readLine();
            if (schemaLine == null) {
                return null;
            }
            // Parse schema line: "COL1 TYPE [PRIMARY KEY], COL2 TYPE, ... "
            List<ColumnDefinition> columns = new ArrayList<>();
            String[] colDefs = schemaLine.split(",");
            for (String colDef : colDefs) {
                colDef = colDef.trim();
                if (colDef.isEmpty()) continue;
                // Check if this column has "PRIMARY KEY"
                boolean pk = colDef.toUpperCase().contains("PRIMARY KEY");
                // Remove "PRIMARY KEY" part for parsing name and type
                String cleanDef = colDef.replaceAll("(?i)PRIMARY KEY", "").trim();
                // Now cleanDef like "COL1 INT", etc.
                String[] parts = cleanDef.split("\\s+");
                if (parts.length < 2) {
                    continue;
                }
                String name = parts[0];
                String type = parts[1];
                // If type had spaces (like "VARCHAR 20"), join them back
                if (parts.length > 2) {
                    StringBuilder typeBuilder = new StringBuilder(parts[1]);
                    for (int i = 2; i < parts.length; i++) {
                        typeBuilder.append(" ").append(parts[i]);
                    }
                    type = typeBuilder.toString();
                }
                columns.add(new ColumnDefinition(name, type, pk));
            }
            return columns;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Read all records (data rows) of a table (excluding the schema line).
     */
    public static List<List<String>> readTableRecords(String dbName, String tableName) {
        File tableFile = new File(new File(BASE_PATH, dbName), tableName + ".txt");
        List<List<String>> records = new ArrayList<>();
        if (!tableFile.exists()) {
            return records;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(tableFile))) {
            // skip schema line
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                // Each line is a record with values separated by commas
                String[] parts = line.split(",");
                List<String> record = new ArrayList<>();
                for (String part : parts) {
                    record.add(part.trim());
                }
                records.add(record);
            }
        } catch (IOException e) {
            // Ignore read errors for now
        }
        return records;
    }

    /**
     * Append a new data row to a table file.
     */
    public static boolean insertRow(String dbName, String tableName, String rowData) {
        File tableFile = new File(new File(BASE_PATH, dbName), tableName + ".txt");
        if (!tableFile.exists()) {
            return false;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile, true))) {
            writer.write(rowData);
            writer.newLine();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Overwrite the table file with new records (keeping existing schema line).
     */
    public static boolean writeTableRecords(String dbName, String tableName, List<ColumnDefinition> schema, List<List<String>> records) {
        File tableFile = new File(new File(BASE_PATH, dbName), tableName + ".txt");
        if (!tableFile.exists()) {
            return false;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tableFile))) {
            // Reconstruct schema line
            StringBuilder schemaLine = new StringBuilder();
            for (int i = 0; i < schema.size(); i++) {
                ColumnDefinition col = schema.get(i);
                schemaLine.append(col.getName()).append(" ").append(col.getType());
                if (col.isPrimaryKey()) {
                    schemaLine.append(" PRIMARY KEY");
                }
                if (i < schema.size() - 1) {
                    schemaLine.append(", ");
                }
            }
            writer.write(schemaLine.toString());
            writer.newLine();
            // Write each record line
            for (List<String> record : records) {
                writer.write(String.join(", ", record));
                writer.newLine();
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
