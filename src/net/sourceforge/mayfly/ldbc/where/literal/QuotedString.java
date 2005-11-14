package net.sourceforge.mayfly.ldbc.where.literal;

import net.sourceforge.mayfly.ldbc.*;
import net.sourceforge.mayfly.datastore.*;

public class QuotedString extends Literal {
    private String stringInQuotes;

    public QuotedString(String stringInQuotes) {
        this.stringInQuotes = stringInQuotes;
    }

    public static net.sourceforge.mayfly.ldbc.where.literal.QuotedString fromQuotedStringTree(Tree tree) {
        return new net.sourceforge.mayfly.ldbc.where.literal.QuotedString(tree.getText());
    }

    private String stringWithoutQuotes() {
        String withoutQuotes = stringInQuotes.substring(1, stringInQuotes.length()-1);
        return withoutQuotes.replaceAll("''", "'");
    }

    public Object valueForCellContentComparison() {
        return stringWithoutQuotes();
    }

    public Tuples process(Tuples originalTuples) {
        throw new RuntimeException();
    }
}
