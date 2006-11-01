package net.sourceforge.mayfly.evaluation.command;

import net.sourceforge.mayfly.MayflyException;
import net.sourceforge.mayfly.MayflyResultSet;
import net.sourceforge.mayfly.datastore.DataStore;
import net.sourceforge.mayfly.datastore.LongCell;
import net.sourceforge.mayfly.evaluation.Expression;
import net.sourceforge.mayfly.evaluation.ResultRow;
import net.sourceforge.mayfly.evaluation.ResultRows;
import net.sourceforge.mayfly.evaluation.expression.LastIdentityExpression;
import net.sourceforge.mayfly.evaluation.what.Selected;

import java.sql.ResultSet;

public class LastIdentity extends Command {

    private static final String UPDATE_MESSAGE = 
        "SELECT is only available with query, not update";

    public UpdateStore update(DataStore store, String currentSchema) {
        throw new MayflyException(UPDATE_MESSAGE);
    }
    
    public ResultSet select(DataStore store, String currentSchema) {
        Expression expression = new LastIdentityExpression();
        return new MayflyResultSet(
            new Selected(expression), 
            new ResultRows(
                new ResultRow()
                    .with(
                        expression,
                        new LongCell(12345))));
    }

}
