package net.sourceforge.mayfly.evaluation.select;

import net.sourceforge.mayfly.datastore.DataStore;
import net.sourceforge.mayfly.datastore.Schema;

/**
 * @internal
 * This class has the information needed to evaluate a subselect.
 */
public class Evaluator {
    
    public static final Evaluator NO_SUBSELECT_NEEDED = 
        new Evaluator(null, null);

    public final DataStore store;
    public final String currentSchema;

    public Evaluator(DataStore store, String currentSchema) {
        this.store = store;
        this.currentSchema = currentSchema;
    }

    public Evaluator(Schema schema) {
        this(new DataStore(schema), DataStore.ANONYMOUS_SCHEMA_NAME);
    }

}
