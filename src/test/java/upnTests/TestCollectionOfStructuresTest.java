package upnTests;

import org.junit.Assert;
import org.junit.Test;

import upsilon.node.dataStructures.CollectionOfStructures;
import upsilon.node.dataStructures.StructureCommand;

public class TestCollectionOfStructuresTest {

    @Test
    public void testGetType() {
        final CollectionOfStructures<StructureCommand> listCommands = new CollectionOfStructures<>("StructureCommand");

        Assert.assertEquals("StructureCommand", listCommands.getTitle());
    }

}
