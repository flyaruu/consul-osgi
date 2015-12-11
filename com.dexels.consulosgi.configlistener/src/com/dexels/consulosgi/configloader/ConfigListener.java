package com.dexels.consulosgi.configloader;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.servicediscovery.http.api.HttpCache;
import com.dexels.servicediscovery.http.api.HttpCache.Events;
import com.dexels.servicediscovery.utils.ConfigurationUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component(name = "dexels.consul.configlistener", configurationPolicy=ConfigurationPolicy.REQUIRE, immediate = true, property = { "event.topics=consul/change/*" })
public class ConfigListener implements EventHandler {

	private HttpCache httpCache;
	private String configPrefix = "config";
	private String clusterName = null;
	private ObjectMapper mapper = new ObjectMapper();
	private ConfigurationAdmin configAdmin;
	private Configuration consulConfiguration;

	private final static Logger logger = LoggerFactory.getLogger(ConfigListener.class);

	@Activate
	public void activate(Map<String, Object> settings) {
		this.clusterName = (String) settings.get("name");
		if (this.clusterName == null) {
			logger.error("Can not initialize configlistener: 'name' setting required to initialize");
			return;
		}
		try {
			consulConfiguration = ConfigurationUtils.createOrReuseFactoryConfiguration(configAdmin,
					"dexels.consul.listener", "(id=owned_by_configloader)",true);
			Dictionary<String, Object> props = new Hashtable<>();
			props.put("path", "/v1/kv/" + configPrefix + "/" + clusterName + "?recurse");
			props.put("id", "owned_by_configloader");
			props.put("wait", "30");
			consulConfiguration.update(props);

		} catch (IOException e) {
			logger.error("Error: ", e);
		}

		try {
			byte[] value = httpCache.getValue("/v1/kv/" + configPrefix + "/" + clusterName + "/" + "?recurse");
			ArrayNode node = (ArrayNode) mapper.readTree(value);
			for (JsonNode jsonNode : node) {
				processRecord((ObjectNode) jsonNode);
			}
			mapper.writerWithDefaultPrettyPrinter().writeValue(System.err, node);
		} catch (JsonProcessingException e) {
			logger.error("Error: ", e);
		} catch (IOException e) {
			logger.error("Error: ", e);
		} catch (Throwable t) {
			logger.error("Error: ", t);
		}
	}

	@Deactivate
	public void deactivate() {
		if (consulConfiguration != null) {
			try {
				consulConfiguration.delete();
			} catch (IOException e) {
				logger.error("Error deleting consul config: ", e);
			}
		}
	}

	private void processRecord(ObjectNode record) throws IOException {
		String key = record.get("Key").asText();
		byte[] decodedValue = Base64.decodeBase64(record.get("Value").asText());
		System.err.println("Value: " + new String(decodedValue));
		processValue(key, decodedValue,Events.CREATE);
	}
//	config/test-knvb/dexels.repository.git.repository

	private void processValue(String key,byte[] bb,Events operation) throws IOException {
		JsonNode on = null;
		if (bb.length == 0) {
			on = mapper.createObjectNode();
		} else {
			on = mapper.readTree(bb);
		}
		if(on instanceof ArrayNode) {
			ArrayNode an = (ArrayNode)on;
			logger.info("Array node detected, didn't expect that: {}",mapper.writer().withDefaultPrettyPrinter().writeValueAsString(an));
			for (JsonNode jsonNode : an) {
				processMap(key, operation, (ObjectNode)jsonNode);
				
			}
		} else {
			processMap(key, operation, (ObjectNode)on);
			
		}
	}

	@SuppressWarnings("unchecked")
	private void processMap(String key, Events operation, ObjectNode on) throws IOException {
		Map<String, String> result = mapper.convertValue(on, Map.class);
		InterpolationHelper.performSubstitution(result);
		Dictionary<String, Object> dict = new Hashtable<>();

		for (Map.Entry<String, String> e : result.entrySet()) {
			dict.put(e.getKey(), e.getValue());
			System.err.println("Key: "+e.getKey()+" value: "+e.getValue());
		}
		String pid = null;
		if (on.get("pid") != null) {
			pid = on.get("pid").asText();
		}
		String factoryPid = createKey(key);
		if (pid == null) {
			pid = factoryPid;
			factoryPid = null;
		}
		switch (operation) {
		case CREATE:
		case MODIFY:
			ConfigurationUtils.updateConfigIfChanged(configAdmin, factoryPid, pid, dict);
			break;
		case DELETE:
			ConfigurationUtils.deleteConfig(configAdmin,factoryPid,pid);
		default:
			break;
		}
	}

	@Reference(name = "ConfigAdmin", unbind = "clearConfigAdmin")
	public void setConfigAdmin(ConfigurationAdmin configAdmin) {
		this.configAdmin = configAdmin;
	}

	public void clearConfigAdmin(ConfigurationAdmin configAdmin) {
		this.configAdmin = null;
	}

	@Reference(unbind = "clearHttpCache", policy = ReferencePolicy.DYNAMIC)
	public void setHttpCache(HttpCache httpCache) {
		this.httpCache = httpCache;
	}

	public void clearHttpCache(HttpCache httpCache) {
		this.httpCache = null;
	}

//	private String trimPrefix(String key) {
//		String prefix = this.configPrefix + "/" + this.clusterName + "/";
//		if (!key.startsWith(prefix)) {
//			throw new IllegalArgumentException("Key: " + key + " does not start with: " + prefix);
//		}
//		return key.substring(prefix.length());
//	}

	private String createKey(String key) {
		String filename = key.substring(key.lastIndexOf("/") + 1, key.length());
		String[] parts = filename.split("-");
		return parts[0];
	}

	@Override
	public void handleEvent(Event event) {
		try {
			Events s = Events.valueOf((String) event.getProperty("type"));
			String key = (String) event.getProperty("key");
			byte[] data = (byte[]) event.getProperty("data");
			processValue(key, data, s);
		} catch (Throwable e) {
			logger.error("Error: ", e);
		}
	}
	
	public static void main(String[] args) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = (ObjectNode) mapper.readTree(ConfigListener.class.getResourceAsStream("example.json"));
		System.err.println("Node: "+node);
	}
	
}
