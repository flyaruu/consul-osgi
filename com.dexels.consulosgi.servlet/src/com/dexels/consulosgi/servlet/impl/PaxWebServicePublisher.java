package com.dexels.consulosgi.servlet.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.servicediscovery.api.ServiceRegistryApi;
import com.dexels.servicediscovery.network.api.NetworkLocation;
import com.dexels.servicediscovery.network.api.PortDiscoverer;

@Component(name="servlet.registrator",configurationPolicy=ConfigurationPolicy.REQUIRE)
public class PaxWebServicePublisher {
	
	private int port;

	private final Map<String,Servlet> servlets = new HashMap<>();
	private final Map<String,String> bindingIds = new HashMap<>();
	private final Map<String,Map<String,Object>> servletSettings = new HashMap<>();

	private ServiceRegistryApi consulApi;
	private PortDiscoverer portDiscoverer;
	private boolean isActive;
	private final static Logger logger = LoggerFactory.getLogger(PaxWebServicePublisher.class);

	
	@Reference
	public void setHttpService(HttpService httpService, Map<String,Object> settings) {
		System.err.println("Settings: "+settings);
		this.port = (Integer) Integer.parseInt((String) settings.get("org.osgi.service.http.port"));
		System.err.println("port: "+port);
	}

	@Reference(unbind="removeServlet", policy=ReferencePolicy.DYNAMIC,cardinality=ReferenceCardinality.MULTIPLE)
	public void addServlet(Servlet servlet,Map<String,Object> settings) {
		NetworkLocation location = portDiscoverer.getHostPort(port);

		try {
			String alias = (String) settings.get("alias");
			if(alias!=null) {
				servlets.put(alias, servlet);
				servletSettings.put(alias, settings);
				bindServlet(alias, settings,location);
			} else {
				logger.info("No alias for servlet {}, ignoring",servlet);
			}
		} catch (Throwable e) {
			logger.error("Error: ", e);
		}
	}

	public void removeServlet(Servlet servlet,Map<String,Object> settings) {
		String alias = (String) settings.get("alias");
		if(alias!=null) {
			servlets.remove(alias);
			if(isActive) {
				unbindServlet(alias);
				servlets.remove(alias);
				servletSettings.remove(alias);
			}
		} else {
			logger.info("No alias for servlet {}, ignoring",servlet);
		}
	}



	@Reference(unbind="clearConsulApi",policy=ReferencePolicy.DYNAMIC)
	public void setConsulApi(ServiceRegistryApi consulApi) {
		this.consulApi = consulApi;
	}
	
	public void clearConsulApi(ServiceRegistryApi consulApi) {
		this.consulApi = null;
	}
	
	@Activate
	public void activate(Map<String,Object> settings) {
//		this.host = (String) settings.get("host");
		isActive = true;
		try {
			NetworkLocation location = portDiscoverer.getHostPort(port);
//			consulApi.cleanAttributes(location.getHost(),location.getPort());
			
			for (Map.Entry<String,Servlet> e : servlets.entrySet()) {
				Map<String, Object> servletSetting = servletSettings.get(e.getKey());
				bindServlet(e.getKey(),servletSetting,location);
			}
		} catch (Throwable e) {
			logger.error("Error: ", e);
		}
	}
	
	private void bindServlet(String alias, Map<String, Object> servletSetting,NetworkLocation location) {
		Set<String> localTags = new HashSet<>();
		appendTags((String) servletSetting.get("tags"),localTags);
		
		try {
			if(!bindingIds.containsKey(alias)) {
				String id = consulApi.registerService(createServiceName(servletSetting), location.getHost(), location.getPort(), alias,servletSetting, localTags);
				bindingIds.put(alias, id);
			}
		} catch (IOException e) {
			logger.error("Error: ", e);
		}
	}


	private void appendTags(String localTags, Set<String> combinedTags) {
		if(localTags!=null) {
			String[] tagParts = localTags.split(",");
			for (String tag : tagParts) {
				combinedTags.add(tag);
			}
		}
	}
	
	private String createServiceName(Map<String, Object> attributes) {
		String name = (String) attributes.get("name");
		if(name!=null) {
			return name;
		}
		String alias = (String) attributes.get("alias");
		if(alias!=null) {
			return alias;
		}
		return null;
	}

	private void unbindServlet(String alias) {
			String id = bindingIds.get(alias);
			consulApi.deregisterService(id);
			bindingIds.remove(alias);
	}

	@Deactivate
	public void deactivate() {
		isActive = false;
		try {
			NetworkLocation location = portDiscoverer.getHostPort(port);
			consulApi.cleanAttributes(location.getHost(),location.getPort());
		} catch (Throwable e1) {
			logger.error("Error: ", e1);
		}

		for (Map.Entry<String,String> e : bindingIds.entrySet()) {
			unbindServlet(e.getValue());
		}
	}
	
	@Reference(unbind="clearPortDisoverer",policy=ReferencePolicy.DYNAMIC)
	public void setPortDiscoverer(PortDiscoverer discoverer) {
		this.portDiscoverer = discoverer;
	}

	public void clearPortDisoverer(PortDiscoverer discoverer) {
		this.portDiscoverer = null;
	}
}
