package net.sourceforge.mayfly.evaluation;

import net.sourceforge.mayfly.MayflyException;
import net.sourceforge.mayfly.evaluation.expression.SingleColumn;
import net.sourceforge.mayfly.util.ValueObject;

public class GroupItem extends ValueObject {

    /**
     * Not yet immutable, because of {@link #resolve(ResultRow)}.
     */
    private Expression expression;

    public GroupItem(Expression expression) {
        this.expression = expression;
    }

    public SingleColumn column() {
        if (expression instanceof SingleColumn) {
            return (SingleColumn) expression;
        }
        else {
            throw new MayflyException(
                "GROUP BY expression (as opposed to column) is not implemented");
        }
    }
    
    public Expression expression() {
        return expression;
    }

    public void resolve(ResultRow row) {
        expression = expression.resolve(row);
    }

}
