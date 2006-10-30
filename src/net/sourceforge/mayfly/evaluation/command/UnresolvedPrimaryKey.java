package net.sourceforge.mayfly.evaluation.command;

import net.sourceforge.mayfly.UnimplementedException;
import net.sourceforge.mayfly.datastore.ColumnNames;
import net.sourceforge.mayfly.datastore.Columns;
import net.sourceforge.mayfly.datastore.DataStore;
import net.sourceforge.mayfly.datastore.constraint.Constraint;
import net.sourceforge.mayfly.datastore.constraint.PrimaryKey;

import java.util.Collections;
import java.util.List;

public class UnresolvedPrimaryKey extends UnresolvedConstraint {
    
    private final ColumnNames columns;

    private final String constraintName;

    public UnresolvedPrimaryKey(List columns, String constraintName) {
        this.columns = new ColumnNames(columns);
        this.constraintName = constraintName;
    }
    
    public UnresolvedPrimaryKey(String column) {
        this(Collections.singletonList(column), null);
    }

    public Constraint resolve(DataStore store, String schema, String table) {
        throw new UnimplementedException();
    }
    
    public Constraint resolve(
        DataStore store, String schema, String table, Columns tableColumns) {
        return new PrimaryKey(this.columns.resolve(tableColumns), constraintName);
    }

}