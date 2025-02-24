package edu.uob;

//----控制层----

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Field;


public class OXOController implements Serializable {
    @Serial 
    private static final long serialVersionUID = 1;
    OXOModel gameModel;

    public OXOController(OXOModel model) {
        gameModel = model;
    }

    public void handleIncomingCommand(String command) throws OXOMoveException {
        char rowLetter = Character.toUpperCase(command.charAt(0)); // 'A'...'Z'
        String colStr = command.substring(1);                      // "1", "2", "3", ...

        int rowIndex = rowLetter - 'A';               // 'A' -> 0, 'B' -> 1, ...
        int colIndex = Integer.parseInt(colStr) - 1;  // "1" -> 0, "2" -> 1, ...

        // 获取当前玩家并将其标记放到对应格子
        OXOPlayer currentPlayer = gameModel.getPlayerByNumber(gameModel.getCurrentPlayerNumber());
        gameModel.setCellOwner(rowIndex, colIndex, currentPlayer);

        // 切换到下一个玩家
        int nextPlayerNumber = (gameModel.getCurrentPlayerNumber() + 1) % gameModel.getNumberOfPlayers();
        gameModel.setCurrentPlayerNumber(nextPlayerNumber);
        
    }
    public void addRow() {
        int oldRows = gameModel.getNumberOfRows();
        int oldCols = gameModel.getNumberOfColumns();
        OXOPlayer[][] newCells = new OXOPlayer[oldRows + 1][oldCols];

        // 1. 拷贝旧数据
        for (int r = 0; r < oldRows; r++) {
            for (int c = 0; c < oldCols; c++) {
                newCells[r][c] = gameModel.getCellOwner(r, c);
            }
        }
        // 2. 新增那一行默认填 null（其实默认就是 null，可不写）
        //    这里仅演示：newCells[oldRows][...] = null

        // 3. 通过反射把 newCells 替换回去
        setNewCellsToModel(newCells);
    }
    public void removeRow() {
        int oldRows = gameModel.getNumberOfRows();
        int oldCols = gameModel.getNumberOfColumns();

        // 如果只剩一行，可根据需求抛异常或直接 return
        if (oldRows <= 1) { return; }

        OXOPlayer[][] newCells = new OXOPlayer[oldRows - 1][oldCols];
        // 拷贝保留的行
        for (int r = 0; r < oldRows - 1; r++) {
            for (int c = 0; c < oldCols; c++) {
                newCells[r][c] = gameModel.getCellOwner(r, c);
            }
        }
        setNewCellsToModel(newCells);
    }
    public void addColumn() {
        int oldRows = gameModel.getNumberOfRows();
        int oldCols = gameModel.getNumberOfColumns();
        OXOPlayer[][] newCells = new OXOPlayer[oldRows][oldCols + 1];

        // 拷贝旧数据
        for (int r = 0; r < oldRows; r++) {
            for (int c = 0; c < oldCols; c++) {
                newCells[r][c] = gameModel.getCellOwner(r, c);
            }
        }
        // 新增的那一列默认 null

        setNewCellsToModel(newCells);
    }
    public void removeColumn() {
        int oldRows = gameModel.getNumberOfRows();
        int oldCols = gameModel.getNumberOfColumns();
        if (oldCols <= 1) { return; }

        OXOPlayer[][] newCells = new OXOPlayer[oldRows][oldCols - 1];
        // 拷贝保留的列
        for (int r = 0; r < oldRows; r++) {
            for (int c = 0; c < oldCols - 1; c++) {
                newCells[r][c] = gameModel.getCellOwner(r, c);
            }
        }
        setNewCellsToModel(newCells);
    }
    public void increaseWinThreshold() {}
    public void decreaseWinThreshold() {}
    public void reset() {}



    private void setNewCellsToModel(OXOPlayer[][] newCells) {
        try {
            Field cellsField = OXOModel.class.getDeclaredField("cells");
            cellsField.setAccessible(true);
            cellsField.set(gameModel, newCells);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
