package com.dexels.sharedconfigstore.http.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.servicediscovery.http.api.ChangeEvent;
import com.dexels.servicediscovery.http.api.HttpCache;
import com.dexels.servicediscovery.http.api.HttpRawApi;
import com.dexels.servicediscovery.http.api.KeyChange;
import com.fasterxml.jackson.databind.JsonNode;

@Component(name="dexels.consul.cache",configurationPolicy=ConfigurationPolicy.IGNORE,immediate=true,property={"event.topics=indexchange/*"})
public class ConsulHttpCacheImpl implements HttpCache,EventHandler {

	private final Map<String,byte[]> cache = new HashMap<>();
	private final Map<String,Integer> cacheIndex = new HashMap<>();
	private HttpRawApi httpApi;
	private final static Logger logger = LoggerFactory.getLogger(ConsulHttpCacheImpl.class);


	private EventAdmin eventAdmin;

	
	@Reference(unbind="clearHttpApi",policy=ReferencePolicy.DYNAMIC,target="(type=consul)")
	public void setHttpApi(HttpRawApi httpApi) {
		this.httpApi = httpApi;
	}

	public void clearHttpApi(HttpRawApi httpApi) {
		this.httpApi = null;
	}
	
	@Override
	public byte[] getValue(String path) {
		String clean = null;
		if(path.indexOf('?')!=-1) {
			clean = path.split("\\?")[0];
		} else {
			clean = path;
		}

		logger.debug("Getting value with path: {} clean: {}",path,clean);
		byte[] cached = cache.get(clean);
		if(cached!=null) {
			return cached;
		}

		byte[] result = null;
		try {
			result = httpApi.get(path);
			if(result!=null) {
				cache.put(clean, result);
			} else {
				logger.warn("No value found for path: {}",path);
			}
		} catch (IOException e) {
			logger.error("Error getting value for path: "+path,e);
		}
		return result;
	}

	@Reference(unbind="clearEventAdmin",policy=ReferencePolicy.DYNAMIC)
	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}
	
	public void clearEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = null;
	}

	@Override
	public void processChange(ChangeEvent ce) {
		Set<String> missing = new HashSet<>(this.cache.keySet());
		missing.removeAll(ce.getChanges().keySet());
		for (Map.Entry<String, KeyChange> e : ce.getChanges().entrySet()) {
			String key = e.getKey();
			int mod = e.getValue().getModifyIndex();
			byte[] data = e.getValue().getDecodedValue();
			Integer prevIndex = cacheIndex.get(key);
			if(prevIndex==null) {
				storeChange(e.getValue());
				postEvent(Events.CREATE, key,mod,data);
				continue;
			}
			if(mod==prevIndex) {
				logger.debug("No real change (identical index)");
				continue;
			}
			storeChange(e.getValue());
			postEvent(Events.MODIFY, key,mod,data);

		}
		for (String key : missing) {
			logger.debug("Deleted missing key: "+key);
			byte[] previous = cache.get(key);
			cache.remove(key);
			Integer previousIndex = cacheIndex.get(key);
			cacheIndex.remove(key);
			postEvent(Events.DELETE, key,previousIndex,previous);
		}
		
	}

	private void postEvent(Events eventType, String key, Integer mod, byte[] data) {
		Map<String,Object> properties = new HashMap<>();
		properties.put("type", eventType.toString());
		properties.put("key", key);
		if(mod!=null) {
			properties.put("index", mod);
		}
		if(data!=null) {
			properties.put("data", data);
		}
		Event e = new Event("consul/change"+cleanKey(key),properties);
		eventAdmin.sendEvent(e);
	}

	private String cleanKey(String key) {
		
		String replaced = key.replaceAll("-", "_").replaceAll("\\.", "_");
		if(!replaced.startsWith("/")) {
			return "/"+replaced;
		}
		if(replaced.endsWith("/")) {
			replaced = replaced.substring(0, replaced.length()-1);
		}
		return replaced;
	}

	private void storeChange(KeyChange value) {
		String key = value.getKey();
		cacheIndex.put(key,value.getModifyIndex());
		cache.put(key, value.getDecodedValue());
	}

	@Override
	public void handleEvent(Event event) {
		try {
			JsonNode changes = (JsonNode) event.getProperty("changes");
			List<KeyChange> c = new ArrayList<>();
			for (JsonNode jsonNode : changes) {
				KeyChange kc = new KeyChange(jsonNode);
				c.add(kc);
			}
			ChangeEvent ce = new ChangeEvent(c);
			processChange(ce);
		} catch (Throwable e) {
			logger.error("Error: ", e);
		}
		
	}
}
