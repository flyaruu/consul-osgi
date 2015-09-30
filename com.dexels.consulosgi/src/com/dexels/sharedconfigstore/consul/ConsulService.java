package com.dexels.sharedconfigstore.consul;

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

public class ConsulService {
	private final String name;
	private final String id;
	private final String address;
	private final int port;
	private final Set<String> tags;
	private final Map<String,String> attributes;

	public ConsulService(final ObjectNode serviceDetails, final ArrayNode serviceAttributes, final String servicePrefix) {
		this.name = serviceDetails.get("ServiceName").asText();
		this.id = serviceDetails.get("ServiceID").asText();
		this.address = serviceDetails.get("ServiceAddress").asText();
		this.port = serviceDetails.get("ServicePort").asInt();
		String tagsString = serviceDetails.get("ServiceName").asText();
		if (tagsString==null) {
			tags = Collections.emptySet();
		} else {
			tags = new HashSet<>();
			String[] tagArray = tagsString.split(",");
			for (String elt : tagArray) {
				tags.add(elt);
			}
		}
		if(serviceAttributes==null) {
			attributes = Collections.emptyMap();
		} else {
			attributes = new HashMap<>();
			for (JsonNode jsonNode : serviceAttributes) {
				ObjectNode node = (ObjectNode) jsonNode;
				String key = node.get("Key").asText();
				String cleanKey = key.substring(id.length()+servicePrefix.length() +2);
				byte[] decodedValue = Base64.decodeBase64(node.get("Value").asText());
				attributes.put(cleanKey, new String(decodedValue));
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
	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	public String getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

	public Set<String> getTags() {
		return tags;
	}

	public Map<String, String> getAttributes() {
		return Collections.unmodifiableMap(this.attributes) ;
	}
}
