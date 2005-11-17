package net.sourceforge.mayfly.ldbc.where.literal;

import net.sourceforge.mayfly.*;
import net.sourceforge.mayfly.datastore.*;
import net.sourceforge.mayfly.ldbc.*;
import net.sourceforge.mayfly.ldbc.what.*;
import net.sourceforge.mayfly.util.*;

public abstract class Literal extends WhatElement implements Transformer {

    public static Literal fromValue(Object value) {
        if (value instanceof Number) {
            Number number = (Number) value;
            return new MathematicalInt(number.intValue());
        }

        throw new RuntimeException("Don't know how to deal with value of class " + value.getClass());
    }

    public boolean matchesCell(Cell cell) {
        return cell.equals(Cell.fromContents(valueForCellContentComparison()));
    }

    public Object transform(Object from) {
        return Cell.fromContents(valueForCellContentComparison());
    }
    
    public Columns columns() {
        return new Columns(new ImmutableList());
    }

    abstract public Object valueForCellContentComparison();

    public Tuple process(Tuple originalTuple, M aliasToTableName) {
        throw new UnimplementedException();
    }

}
