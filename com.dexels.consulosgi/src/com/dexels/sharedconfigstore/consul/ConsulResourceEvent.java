package com.dexels.sharedconfigstore.consul;

import org.codehaus.jackson.JsonNode;

public final class ConsulResourceEvent {
	
	private final JsonNode oldValue;
	private final JsonNode newValue;
	private final String path;
	private final String servicePrefix;
	
	public ConsulResourceEvent(String path, JsonNode oldValue, JsonNode newValue,String servicePrefix) {
		this.path = path;
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.servicePrefix = servicePrefix;
		
	}

	public String getPath() {
		return path;
	}

	public JsonNode getOldValue() {
		return oldValue;
	}

	public JsonNode getNewValue() {
		return newValue;
	}

	public String getServicePrefix() {
		return servicePrefix;
	}
}
