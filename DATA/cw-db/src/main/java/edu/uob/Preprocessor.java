package main.java.edu.uob;

public class Preprocessor {
    /**
     * Preprocess an SQL statement by trimming whitespace, converting to upper case, and formatting spaces.
     * This helps to standardize the input for parsing.
     */
    public String preprocess(String sql) {
        if (sql == null) {
            return null;
        }

        sql = sql.trim();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }
        // Replace newline and tab with space
        sql = sql.replace('\n', ' ').replace('\t', ' ');

        sql = sql.replaceAll("\\s+", " ");

        sql = sql.replace("(", " ( ");
        sql = sql.replace(")", " ) ");
        sql = sql.replace(",", " , ");
        sql = sql.replace("=", " = ");
        sql = sql.replace("*", " * ");
        // Collapse multiple spaces again after adding spaces around punctuation
        sql = sql.replaceAll("\\s+", " ");
        // Convert to upper case for case-insensitivity of keywords and identifiers
        sql = sql.toUpperCase();
        return sql.trim();
    }
}
