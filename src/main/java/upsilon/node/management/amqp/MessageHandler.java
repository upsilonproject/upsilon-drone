package upsilon.node.management.amqp;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
 
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;

import upsilon.node.Configuration;
import upsilon.node.Database;
import upsilon.node.Main; 
import upsilon.node.configuration.ValidatedConfiguration;
import upsilon.node.configuration.xml.XmlConfigurationLoader;
import upsilon.node.configuration.xml.XmlConfigurationValidator;
import upsilon.node.dataStructures.StructureNode;
import upsilon.node.dataStructures.StructureService;
import upsilon.node.dataStructures.StructureRemoteService;
import upsilon.node.util.UPath;
import upsilon.node.util.ResourceResolver;

public class MessageHandler {
	private class MessageHelper {
		private Map<String, Object> headers;
		
		public MessageHelper(Map<String, Object> headers) {
			this.headers = headers;
		}

		public Object getHeader(Object key) {
			if (headers.containsKey(key)) {
				return headers.get(key); 
			} else { 
				return "";
			}
		}
		
		public int getHeaderInt(String key) {
			String v = getHeaderString(key);
			
			try {
				return Integer.parseInt(v); 
			} catch (Exception e) { 
				return 0; 
			}
		}
		
		public String getHeaderString(String key) {
			return this.getHeader(key).toString();
		} 
	}
	private static final transient Logger LOG = LoggerFactory.getLogger(MessageHandler.class);

	public static BasicProperties getNewMsgProps(final UpsilonMessageType messageType) {
		return MessageHandler.getNewMsgPropsBuilder(messageType).build();
	}
	
	public static Map<String, Object> getNewMessageHeaders(final UpsilonMessageType messageType) {
		final Map<String, Object> headers = new HashMap<>();
		headers.put("upsilon-msg-type", messageType.toString());
		headers.put("version", Main.getVersion());
		headers.put("identifier", Main.instance.node.getIdentifier());
		
		return headers;
	}

	public static Builder getNewMsgPropsBuilder(final UpsilonMessageType messageType) {
		Map<String, Object> headers = getNewMessageHeaders(messageType);

		return getBuilderFromHeaders(headers);
	} 
	
	public static Builder getBuilderFromHeaders(Map<String, Object> headers) {
		final Builder builder = new BasicProperties.Builder();
		builder.headers(headers);

		return builder;
	}

	public void handleMessageType(final UpsilonMessageType type, Map<String,Object> headers, final String body, final long deliveryTag, final String replyTo, final Channel channel) throws Exception {
		MessageHelper helper = new MessageHelper(headers); 
		
		switch (type) {
		case REQ_NODE_SUMMARY:
			String nodeSummary = "";
			nodeSummary += "Version: " + Main.getVersion() + "\n";
			nodeSummary += "Identifier: " + Main.instance.node.getIdentifier() + "\n";
			nodeSummary = nodeSummary.trim();

			MessageHandler.LOG.debug("req version summary, sending back:\n" + nodeSummary);

			Map<String, Object> respHeaders = MessageHandler.getNewMessageHeaders(UpsilonMessageType.RES_NODE_SUMMARY);
			respHeaders.put("node-identifier", Main.instance.node.getIdentifier());
			respHeaders.put("node-version", Main.getVersion());
			respHeaders.put("node-service-count", Configuration.instance.services.size());	
			respHeaders.put("node-type", Main.instance.node.getType());
 
			channel.basicPublish(DaemonConnectionHandler.EXCHANGE_NAME, "upsilon.res", getBuilderFromHeaders(respHeaders).build(), nodeSummary.getBytes());

			channel.basicAck(deliveryTag, false);
			break;
		case REQ_COMMAND_CREATE:
			break;
		case REQ_SERVICE_CREATE:
			final XmlConfigurationValidator validator = new XmlConfigurationValidator(body, true);
			final ValidatedConfiguration vcfg = validator.getValidatedConfiguration();

			final XmlConfigurationLoader loader = new XmlConfigurationLoader();
			loader.load("fragment", vcfg, false);

			System.out.println("service create SS");

			break;
		case UPDATED_NODE_CONFIG:
			channel.basicAck(deliveryTag, false);

			String nodeIdentifier = headers.get("node-identifier").toString();

			if (Main.instance.node.getIdentifier().equals(nodeIdentifier)) {
				String configIdentifier = headers.get("remote-config-source-identifier").toString();
				String configId = headers.get("remote-config-id").toString();

				UPath configPath;

				if (ResourceResolver.getInstance().getConfigDirAsUPath().isWriteable()) {
					configPath = new UPath(ResourceResolver.getInstance().getConfigDir() + File.separator + "remotes.d" + File.separator + configIdentifier + ".xml");
				} else {
					configPath = new UPath(Files.createTempFile("upsilon-remoteConfig-" + configId, ".xml"));
				}
					   
				FileWriter configWriter = new FileWriter(configPath.getAbsolutePath());    
				configWriter.write(body);  
				configWriter.flush();    
				configWriter.close(); 
				
				Main.instance.getConfigurationLoader().load(configId, configPath, false, true);
			} else {
				LOG.debug("Irrelevant node config received for: " + nodeIdentifier);
			}
		
			break;			
		case HEARTBEAT:
		case RES_NODE_SUMMARY:
			LOG.debug("Node heartbeat from: " + helper.getHeaderString("node-identifier"));  
			 
			if (Database.instance != null) {
				StructureNode node = new StructureNode();
				node.setDatabaseUpdateRequired(true);
				node.setIdentifier(helper.getHeaderString("node-identifier"));
				node.setInstanceApplicationVersion(helper.getHeaderString("node-version"));
				node.setServiceCount((helper.getHeaderInt("node-service-count")));
				node.setSource("amqp");  
				node.setType(helper.getHeaderString("node-type"));
				node.setConfigs(helper.getHeaderString("node-configs")); 
				
				Configuration.instance.remoteNodes.register(node);
			} 
			 
			channel.basicAck(deliveryTag, false);
			
			break; 
		case SERVICE_CHECK_RESULT:
			if (Database.instance != null) {
				StructureRemoteService srs = new StructureRemoteService();
				srs.setIdentifier(helper.getHeaderString("identifier")); 
				srs.setCommandIdentifier(helper.getHeaderString("command-identifier"));
				srs.setDatabaseUpdateRequired(true);
				srs.setDescription(helper.getHeaderString("description"));
				srs.setExecutable(helper.getHeaderString("executable"));
				srs.setNodeIdentifier(helper.getHeaderString("node-identifier"));
				srs.setOutput(body); 
				srs.setResultConsequtiveCount(helper.getHeaderInt("consequtive-count"));
				srs.setKarmaString(helper.getHeaderString("karma"));
				srs.setLastUpdated(Instant.now()); // faked
				srs.setLastChanged(Instant.now()); // faked
				srs.setEstimatedNextCheck(Instant.now()); // caked
				
				Configuration.instance.remoteServices.add(srs);
			}
			
			channel.basicAck(deliveryTag, false);
			
			break;
		case EXECUTE_SINGLE:
			String serviceIdentifier = headers.get("service-identifier").toString();
			String nodeIdentifier2 = headers.get("node-identifier").toString();

			if (Main.instance.node.getIdentifier().equals(nodeIdentifier2)) {
				StructureService service = Configuration.instance.services.get(serviceIdentifier);

				if (service == null) {
					MessageHandler.LOG.warn("Could not find service to EXECUTE_SINGLE: " + serviceIdentifier);
				} else {
					MessageHandler.LOG.info("Enqueued service due to EXECUTE_SINGLE: " + serviceIdentifier);
					Main.instance.queueMaintainer.queueUrgent(service);			
				}
			}
			
			channel.basicAck(deliveryTag, false);

			break;
		case UNKNOWN:  
		default:
			MessageHandler.LOG.warn("Unsupported upsilon message type: " + type);
			channel.basicAck(deliveryTag, false);
			break;
		}
	}
}
