package net.sourceforge.mayfly.evaluation.select;

import net.sourceforge.mayfly.MayflyException;
import net.sourceforge.mayfly.datastore.Cell;
import net.sourceforge.mayfly.evaluation.NoColumn;
import net.sourceforge.mayfly.evaluation.ResultRow;
import net.sourceforge.mayfly.evaluation.expression.SingleColumn;
import net.sourceforge.mayfly.evaluation.what.What;

public class ColumnOrderItem extends OrderItem {

    private final SingleColumn column;

    public ColumnOrderItem(SingleColumn column, boolean ascending) {
        super(ascending);
        this.column = column;
    }
    
    protected int compareAscending(What what, ResultRow first, ResultRow second) {
        return compare(first, second, column);
    }

    public static int compare(ResultRow first, ResultRow second, SingleColumn column) {
        Cell cell1 = column.evaluate(first);
        Cell cell2 = column.evaluate(second);
        return cell1.compareTo(cell2, column.location);
    }
    
    public void check(ResultRow afterGroupByAndDistinct, ResultRow afterJoins) {
        try {
            column.evaluate(afterGroupByAndDistinct);
        } catch (NoColumn e) {
            column.evaluate(afterJoins);
            throw new MayflyException(
                "ORDER BY expression " + column.displayName() +
                " should be in SELECT DISTINCT list",
                column.location);
        }
    }
    
}
