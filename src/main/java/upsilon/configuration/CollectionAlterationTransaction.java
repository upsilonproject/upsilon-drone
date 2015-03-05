package upsilon.configuration;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import upsilon.dataStructures.CollectionOfStructures;
import upsilon.dataStructures.ConfigStructure;

public class CollectionAlterationTransaction<T extends ConfigStructure> {
    private static int catIndex = 0;

    private final int id;
    private final static transient Logger LOG = LoggerFactory.getLogger(CollectionAlterationTransaction.class);
    private final CollectionOfStructures<T> list;
    private final HashMap<String, XmlNodeHelper> newList = new HashMap<>();
    private Vector<String> oldList = new Vector<String>();
    private final HashMap<String, XmlNodeHelper> updList = new HashMap<>();

    public CollectionAlterationTransaction(final CollectionOfStructures<T> list, String source) {
        this.list = list;  
        this.oldList = list.getIdsWithSource(source);
        this.id = ++CollectionAlterationTransaction.catIndex;
    }

    public void considerFromConfig(final XmlNodeHelper el) {
        final String idFromConfig = el.getAttributeValueUnchecked("id");

        if (this.list.containsId(idFromConfig)) {
            this.updList.put(idFromConfig, el);
            this.oldList.remove(idFromConfig);
        } else {
            this.newList.put(idFromConfig, el);
        }
    }

    public Map<String, XmlNodeHelper> getNew() {
        return this.newList;
    }

    public Set<String> getNewIds() {
        return this.newList.keySet();
    }

    public Vector<String> getOld() {
        return this.oldList;
    }

    public List<String> getOldIds() {
        return this.oldList;
    }

    public Map<String, XmlNodeHelper> getUpdated() {
        return this.updList;
    }

    public Set<String> getUpdatedIds() {
        return this.updList.keySet();
    }

    public void print() {
        this.printList("new", this.getNewIds());
        this.printList("old", this.getOldIds());
        this.printList("upd", this.getUpdatedIds());
    }

    public void printList(final String name, final Collection<String> col) {
        if (col.isEmpty()) {
            return;
        }

        CollectionAlterationTransaction.LOG.trace(name);
        CollectionAlterationTransaction.LOG.trace("---------");

        for (final String s : col) {
            CollectionAlterationTransaction.LOG.trace("* " + s);
        }
  
        CollectionAlterationTransaction.LOG.trace("");
    }

    @Override
    public String toString() {
        return "CAT<" + this.list.getTitle() + "> #" + this.id;
    }

	public boolean isEmpty() { 
		return newList.isEmpty() && oldList.isEmpty() && updList.isEmpty(); 
	}
}
