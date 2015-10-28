package com.dexels.consulosgi.servicediscovery.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.dexels.servicediscovery.api.PublishedService;

@Component(name="dexels.sharedservice", configurationPolicy=ConfigurationPolicy.REQUIRE,immediate=true)
public class PublishedServiceImpl implements PublishedService {

	private String id;
	private final Map<String,Object> attributes = new HashMap<>();
	private final Set<String> tags = new HashSet<>();
	private int port;
	private String host;

	@Activate
	public void activate(Map<String,Object> settings) {
		for (Map.Entry<String, Object> e : settings.entrySet()) {
			if(e.getKey().equals("id")) {
				this.id = (String) e.getValue();
				continue;
			} else if(e.getKey().equals("port")) {
				parsePort(e.getValue());
				continue;
			} else if(e.getKey().equals("tags")) {
				parseTags((String)e.getValue());
				continue;
			} else if(e.getKey().equals("host")) {
				this.host = (String)e.getValue();
			} else {
				attributes.put(e.getKey(), e.getValue());
			}
		}
	}
	
	private void parseTags(String value) {
		String[] parsed = value.split(",");
		for (String e : parsed) {
			tags.add(e);
		}
		
	}

	private void parsePort(Object value) {
		if(value instanceof String) {
			this.port = Integer.parseInt((String) value);
		}
		if(value instanceof Integer) {
			this.port = (Integer)value;
		}
	}

	@Override
	public Map<String,Object> getServiceAttributes() {
		return Collections.unmodifiableMap(attributes);
		
	}
	
	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public Set<String> getTags() {
		return Collections.unmodifiableSet(tags);
	}

	@Override
	public String getHost() {
		return this.host;
	}
}
