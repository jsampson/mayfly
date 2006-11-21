package net.sourceforge.mayfly.evaluation.command;

import net.sourceforge.mayfly.MayflyException;
import net.sourceforge.mayfly.datastore.Cell;
import net.sourceforge.mayfly.datastore.DataStore;
import net.sourceforge.mayfly.parser.Parser;

import java.sql.ResultSet;
import java.util.List;

public abstract class Command {

    public static Command fromSql(String sql) {
        return new Parser(sql).parse();
    }

    public static Command fromTokens(List tokens) {
        return new Parser(tokens).parse();
    }

    abstract public UpdateStore update(DataStore store, String currentSchema);

    public ResultSet select(DataStore store, String currentSchema, 
        Cell lastIdentity) {
        throw new MayflyException(
            "This command is only available with update, not query");
    }

}
