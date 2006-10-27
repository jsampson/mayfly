package net.sourceforge.mayfly.datastore.constraint;

import net.sourceforge.mayfly.MayflyException;
import net.sourceforge.mayfly.datastore.Cell;
import net.sourceforge.mayfly.datastore.Columns;
import net.sourceforge.mayfly.datastore.NullCell;

public class PrimaryKey extends NotNullOrUnique {

    public PrimaryKey(Columns columns) {
        this(columns, null);
    }

    public PrimaryKey(Columns columns, String constraintName) {
        super(columns, constraintName);
    }

    protected void checkForNull(String column, Cell proposedCell) {
        if (proposedCell instanceof NullCell) {
            throw new MayflyException("primary key " + column + " cannot be null");
        }
    }

    protected String description() {
        return "primary key";
    }

}
