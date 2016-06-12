@XmlJavaTypeAdapters({
	@XmlJavaTypeAdapter(value=DurationAdapter.class,type=Duration.class),
	@XmlJavaTypeAdapter(value=InstantAdapter.class,type=Instant.class),
})  
package upsilon.node.dataStructures; 

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter; 
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import java.time.Duration;
import java.time.Instant; 

import upsilon.node.management.rest.server.util.DurationAdapter;
import upsilon.node.management.rest.server.util.InstantAdapter;
 
      