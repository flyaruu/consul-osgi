package com.dexels.servicediscovery.consul.impl;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.dexels.servicediscovery.api.DiscoveredService;


@Component(name="osgi.consul.local.service",configurationPolicy=ConfigurationPolicy.REQUIRE,immediate=true)
public class DiscoveredServiceImpl implements DiscoveredService {
	private String name;
	private String id;
	private String address;
	private int port;
	private Set<String> tags;
	private Map<String,String> attributes;
//	private final Map<String,String> container;

	private String hostIP = null;
	private String hostPort = null;
	private String portType = null;
	private String containerID = null;
	private String containerHostname = null;
	private String exposedIP = null;
	
	
	public DiscoveredServiceImpl() {
		
	}
	
	public void activate(Map<String,String> settings) {
		this.name = settings.get("name");
		this.id = settings.get("id");
		this.address = settings.get("address");
		this.port = Integer.parseInt(settings.get("port"));
		String tagsString = settings.get("tags");
		if (tagsString==null) {
			tags = Collections.emptySet();
		} else {
			tags = new HashSet<>();
			String[] tagArray = tagsString.split(",");
			for (String elt : tagArray) {
				tags.add(elt);
			}
		}
		this.attributes = new HashMap<>();
		for (Map.Entry<String, String> entry : settings.entrySet()) {
			if(entry.getKey().startsWith("attribute.")) {
				String key = entry.getKey().substring("attribute.".length());
				attributes.put(key, entry.getValue());
			}
		}
	}
	
	public DiscoveredServiceImpl(final ObjectNode serviceDetails, final ArrayNode serviceAttributes, final ArrayNode containerAttributes, final String servicePrefix, final String containerPrefix) {
		this.name = serviceDetails.get("ServiceName").asText();
		this.id = serviceDetails.get("ServiceID").asText();
		this.address = serviceDetails.get("ServiceAddress").asText();
		this.port = serviceDetails.get("ServicePort").asInt();
		String tagsString = serviceDetails.get("ServiceTags").asText();
		if (tagsString==null) {
			tags = Collections.emptySet();
		} else {
			tags = new HashSet<>();
			String[] tagArray = tagsString.split(",");
			for (String elt : tagArray) {
				tags.add(elt);
			}
		}
		if (containerAttributes==null) {
//			container = Collections.emptyMap();
		} else {
//			container = new HashMap<>();
			for (JsonNode jsonNode : containerAttributes) {
				ObjectNode node = (ObjectNode) jsonNode;
				String key = node.get("Key").asText();
				String stripped = key.substring(containerPrefix.length()+1+this.id.length()+1,key.length());
				String value = new String(Base64.decodeBase64(node.get("Value").asText()));
//				container.put(stripped.replaceAll("/", "."), value);
				System.err.println("Found container attribute: "+key+" : "+value);
				if(stripped.equals("HostIP")) {
					this.hostIP = value;
				}
				if(stripped.equals("ExposedIP")) {
					this.exposedIP = value;
				}
				if(stripped.equals("ContainerHostname")) {
					this.containerHostname = value;
				}
				if(stripped.equals("ContainerID")) {
					this.containerID = value;
				}
				if(stripped.equals("HostPort")) {
					this.hostPort = value;
				}
				if(stripped.equals("PortType")) {
					this.portType = value;
				}
			}
		}
		if(serviceAttributes==null) {
			attributes = Collections.emptyMap();
		} else {
			attributes = new HashMap<>();
			for (JsonNode jsonNode : serviceAttributes) {
				ObjectNode node = (ObjectNode) jsonNode;
				String key = node.get("Key").asText();
				String stripped = key.substring(key.lastIndexOf('/')+1);
				byte[] decodedValue = Base64.decodeBase64(node.get("Value").asText());
				String valueString = new String(decodedValue);
				attributes.put(stripped, valueString);
			}
		}
	}

	
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("******* Service: "+name+"******\n");
		result.append("ServiceID : "+id+"\n");
		result.append("Address : "+address+"\n");
		result.append("Port : "+port+"\n");
		result.append("Tags : "+tags+"\n");
		result.append("Attributes : "+attributes+"\n");
//		result.append("Container : "+container+"\n");
		if(this.hostIP!=null) {
			result.append("HostIP : "+hostIP+"\n");
		}
		if(this.hostPort!=null) {
			result.append("hostPort : "+hostPort+"\n");
		}
		if(this.portType!=null) {
			result.append("portType : "+portType+"\n");
		}
		if(this.containerID!=null) {
			result.append("containerID : "+containerID+"\n");
		}
		if(this.containerHostname!=null) {
			result.append("containerHostname : "+containerHostname+"\n");
		}
		if(this.exposedIP!=null) {
			result.append("exposedIP : "+exposedIP+"\n");
		}
		result.append("******* End of Service: "+name+"\n");
		return result.toString();
	}

	public Dictionary<String, Object> createDictionary() {
		Dictionary<String, Object> result = new Hashtable<String, Object>();
		result.put("id", id);
		result.put("name", name);
		result.put("address", address);
		result.put("port", port);
		if(attributes!=null) {
			for (Entry<String,String> e : attributes.entrySet()) {
				result.put(e.getKey(), e.getValue());
			}
		}
		return result;
	}
	/* (non-Javadoc)
	 * @see com.dexels.sharedconfigstore.consul.impl.ConsulService#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see com.dexels.sharedconfigstore.consul.impl.ConsulService#getId()
	 */
	@Override
	public String getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see com.dexels.sharedconfigstore.consul.impl.ConsulService#getAddress()
	 */
	@Override
	public String getAddress() {
		return address;
	}

	/* (non-Javadoc)
	 * @see com.dexels.sharedconfigstore.consul.impl.ConsulService#getPort()
	 */
	@Override
	public int getPort() {
		return port;
	}

	/* (non-Javadoc)
	 * @see com.dexels.sharedconfigstore.consul.impl.ConsulService#getTags()
	 */
	@Override
	public Set<String> getTags() {
		return tags;
	}

	/* (non-Javadoc)
	 * @see com.dexels.sharedconfigstore.consul.impl.ConsulService#getAttributes()
	 */
	@Override
	public Map<String, String> getAttributes() {
		return Collections.unmodifiableMap(this.attributes) ;
	}

	public String getHostIP() {
		return hostIP;
	}

	public String getHostPort() {
		return hostPort;
	}

	/* (non-Javadoc)
	 * @see com.dexels.sharedconfigstore.consul.impl.ConsulService#getPortType()
	 */
	@Override
	public String getPortType() {
		return portType;
	}

	public String getContainerID() {
		return containerID;
	}

	public String getContainerHostname() {
		return containerHostname;
	}

	public String getExposedIP() {
		return exposedIP;
	}

}
