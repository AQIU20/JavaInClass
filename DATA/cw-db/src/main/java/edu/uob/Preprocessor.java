public class Preprocessor {

    public static String normalize(String rawCommand) {

        String[] symbols = {"(", ")", ",", ";", "=", ">=", "<=", ">", "<", "==", "!="};
        for (String sym : symbols) {
            
            // 用正则在符号前后插入空格
            rawCommand = rawCommand.replace(sym, " " + sym + " ");
        }

        // 将多个空格压缩成一个空格
        rawCommand = rawCommand.replaceAll("\\s+", " ");

        // 去掉首尾空白
        rawCommand = rawCommand.trim();

        return rawCommand;
    }

    
}
