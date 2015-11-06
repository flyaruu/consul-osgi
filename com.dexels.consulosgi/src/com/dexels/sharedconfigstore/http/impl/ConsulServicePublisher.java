package com.dexels.sharedconfigstore.http.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.servicediscovery.api.ServiceRegistryApi;
import com.dexels.servicediscovery.http.api.HttpJsonApi;
import com.dexels.servicediscovery.http.api.HttpRawApi;

@Component(name="dexels.consul.publisher",immediate=true,configurationPolicy=ConfigurationPolicy.REQUIRE)
public class ConsulServicePublisher implements ServiceRegistryApi {

	
	private final static Logger logger = LoggerFactory.getLogger(ConsulServicePublisher.class);
	private static final String SERVICE_REGISTER = "/v1/agent/service/register";

	public static final String KVPREFIX = "/v1/kv/";
	private ObjectMapper mapper;
	private String servicePrefix;
	private HttpJsonApi consulClient;
	private HttpRawApi rawConsulClient;
	private Set<String> tags;

	@Activate
	public void activate(Map<String,Object> settings) {
		this.servicePrefix = (String)settings.get("servicePrefix");
		this.tags = parseTags((String) settings.get("tags"));
		this.mapper = new ObjectMapper().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
	}

	@Deactivate
	public void deactivate() {
		// cancel all running requests
		// close sync/async connector
		this.tags = null;
	}

	@Reference(unbind="clearConsulClient", policy=ReferencePolicy.DYNAMIC)
	public void setConsulClient(HttpJsonApi httpApi) {
		this.consulClient = httpApi;
	}

	public void clearConsulClient(HttpJsonApi httpApi) {
		this.consulClient = null;
	}
	
	@Reference(unbind="clearRawConsulClient", policy=ReferencePolicy.DYNAMIC)
	public void setRawConsulClient(HttpRawApi httpApi) {
		this.rawConsulClient = httpApi;
	}

	public void clearRawConsulClient(HttpRawApi httpApi) {
		this.rawConsulClient = null;
	}
	
	private Set<String> parseTags(String tags) {
		if(tags==null) {
			return Collections.emptySet();
		}
		String[] tg = tags.split(",");
		Set<String> result = new HashSet<>();
		for (String s : tg) {
			result.add(s);
		}
		return result;
	}
	
	@Override
	public String registerService(String name, String host, int port, String alias, Map<String,Object> attributes, Set<String> tags) throws IOException {
		ObjectNode request = mapper.createObjectNode();
		request.put("Name", name);
		ArrayNode tagsNode = mapper.createArrayNode();
		request.put("Tags", tagsNode);
		Set<String> localTags = new HashSet<>(this.tags);
		localTags.addAll(tags);
		for (String tag : localTags) {
			tagsNode.add(tag);
		}
		request.put("Address", host);
		request.put("Port", port);
		consulClient.putJson(SERVICE_REGISTER, request,false);
		writeAttributes(host, port,name, attributes);
		return name;
	}
	
	@Override
	public void cleanAttributes(String host, int port) throws IOException {
		consulClient.deleteJson(KVPREFIX+servicePrefix+"/"+host+"/"+port+"?recurse");
	}
	
	private void writeAttributes(String host, int port, String name, Map<String, Object> attributes) throws IOException {
//		ObjectNode result = mapper.createObjectNode();
		for (Map.Entry<String, Object> e : attributes.entrySet()) {
			switch (e.getKey()) {
			case "objectClass":
				continue;
			case "component.name":
				continue;
			case "component.id":
				continue;
			case "service.id":
				continue;
			default:
				break;
			}
//			result.put(e.getKey(), (String)e.getValue());
			rawConsulClient.put(KVPREFIX+servicePrefix+"/"+host+"/"+port+"/"+name+"/"+e.getKey(),((String)e.getValue()).getBytes(),false);
		}
		
	}

	@Override
	public void deregisterService(String id) {
		try {
			consulClient.deleteJson(KVPREFIX+id+"?recurse");
		} catch (IOException e) {
			logger.error("Error: ", e);
		}
		
	}

	
}
