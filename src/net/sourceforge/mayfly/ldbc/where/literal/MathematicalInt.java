package net.sourceforge.mayfly.ldbc.where.literal;

import net.sourceforge.mayfly.datastore.*;
import net.sourceforge.mayfly.ldbc.*;

public class MathematicalInt extends Literal {

    private final int value;

    public MathematicalInt(int value) {
        this.value = value;
    }

    public static Literal fromDecimalValueTree(Tree tree) {
        return new MathematicalInt(Integer.parseInt(tree.getFirstChild().getText()));
    }

    public boolean matchesCell(Cell cell) {
        return value == cell.asInt();
    }

    public Object valueForCellContentComparison() {
        return new Long(value);
    }

}