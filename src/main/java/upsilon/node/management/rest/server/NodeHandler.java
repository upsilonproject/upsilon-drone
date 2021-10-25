package upsilon.node.management.rest.server;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import upsilon.node.Configuration;
import upsilon.node.dataStructures.StructureNode;
import upsilon.node.dataStructures.StructurePeer;

@Path("nodes")
public class NodeHandler {
    private final static transient Logger LOG = LoggerFactory.getLogger(NodeHandler.class);

    @GET
    public Response def() {
        return Response.status(Status.OK).type(MediaType.TEXT_PLAIN).entity("NodeHandler").build();
    }

    @GET
    @Path("/list")
    public List<StructurePeer> list() {
        return Configuration.instance.peers.getImmutable();
    }

    @GET
    @Path("/listRemote")
    @XmlElementWrapper
    @XmlElement
    public List<StructureNode> listRemote() {
        return Configuration.instance.remoteNodes.getImmutable();
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Path("/update")
    public Response update(final StructureNode node) {
        NodeHandler.LOG.debug("Got node: " + node.getType() + " id: " + node.getIdentifier());
        node.setDatabaseUpdateRequired(true);

        Configuration.instance.remoteNodes.register(node);

        return Response.status(Status.OK).build();
    }
}
