package com.dexels.sharedconfigstore.consul.impl;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.sharedconfigstore.consul.ConsulResourceEvent;
import com.dexels.sharedconfigstore.consul.ConsulResourceListener;
import com.dexels.sharedconfigstore.consul.ConsulService;
import com.dexels.sharedconfigstore.consul.LongPollingScheduler;

@Component(name="dexels.consul.monitor",immediate=true)
public class ConsulMonitorImpl implements ConsulResourceListener {
	
	private LongPollingScheduler consulListener = null;
//	private final ObjectMapper mapper = new ObjectMapper().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);;

	private final Map<String,ConsulService> detectedServices = new HashMap<>();
	private ConfigurationAdmin configAdmin;

	private final Map<String,Configuration> resourcePids = new HashMap<>();
	
	
	private final static Logger logger = LoggerFactory.getLogger(ConsulMonitorImpl.class);

	
	@Activate
	public void activate() {
		consulListener.addConsulResourceListener(this);
		// is this the right place?
		consulListener.monitorURL("/v1/catalog/services");
	}
	
	public void deactivate() {
		consulListener.removeConsulResourceListener(this);
	}
	
	@Reference(unbind="clearConsulListener",policy=ReferencePolicy.DYNAMIC)
	public void setConsulListener(LongPollingScheduler scheduler) {
		consulListener = scheduler;
	}

	public void clearConsulListener(LongPollingScheduler scheduler) {
		consulListener = null;
	}

	@Override
	public void resourceChanged(ConsulResourceEvent event) {
		try {
			ObjectNode services = (ObjectNode) event.getNewValue();
			Iterator<String> names = services.getFieldNames();
			
			// copy, so orphans can be detected
			Map<String,ConsulService> remaining = new HashMap<>(detectedServices);
			Map<String,ConsulService> newServices = new HashMap<>();
			while (names.hasNext()) {
				try {
					String serviceName = names.next();
//				System.err.println(">> name: "+serviceName);
					ArrayNode serviceDetails = (ArrayNode) consulListener.queryPath("/v1/catalog/service/"+serviceName);
					for (JsonNode serv : serviceDetails) {
						String serviceId = ((ObjectNode)serv).get("ServiceID").asText();
//					mapper.writerWithDefaultPrettyPrinter().writeValue(System.err, serviceDetails);
						String url = "/v1/kv/"+event.getServicePrefix()+"/"+serviceId+"?recurse";
						JsonNode serviceAttributes = consulListener.queryPath(url);
//					if(serviceAttributes!=null) {
//						mapper.writerWithDefaultPrettyPrinter().writeValue(System.err, serviceAttributes);
//					}
						ConsulService cs = new ConsulService((ObjectNode)serv, (ArrayNode)serviceAttributes,event.getServicePrefix());
//						System.err.println("Service: \n"+cs);
						if(remaining.containsKey(serviceId)) {
							// tick off
							System.err.println("ServiceId: "+serviceId+" is still present");
							remaining.remove(serviceId);
						} else {
							System.err.println("ServiceId: "+serviceId+"  seems to be new");
							newServices.put(serviceId, cs);
						}
					}
					
//				detectedServices.

				} catch (Throwable e) {
					e.printStackTrace();
				}
				
			}

//		for (String orphan : remaining.keySet()) {
				for (Entry<String,ConsulService> e : remaining.entrySet()) {
				System.err.println("removing: "+e.getKey());
				removeService(e.getKey(),e.getValue());
				detectedServices.remove(e.getKey());
			}
			for (Entry<String,ConsulService> e : newServices.entrySet()) {
				addService(e.getKey(),e.getValue());
				detectedServices.put(e.getKey(),e.getValue());
			}
		} catch (IOException e) {
			logger.error("Error: ", e);
		}
		
		
	}

    @Reference(name = "ConfigAdmin", unbind = "clearConfigAdmin")
    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public void clearConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = null;
    }
    
	private void removeService(String key, ConsulService value) {
		//configAdmin.getConfiguration(pid, location)
		Configuration cc = resourcePids.get(value.getId());
		if(cc!=null) {
			try {
				cc.delete();
			} catch (IOException e) {
				logger.error("Error: ", e);
			}
		}
	}

	private void addService(String key, ConsulService value) throws IOException {
		String type = value.getAttributes().get("type");
		if(type!=null) {
			String factoryPid = type;
			String filter = "(id="+value.getId()+")";
			Configuration cc = emitFactoryIfChanged(factoryPid, filter, value.createDictionary());
			resourcePids.put(value.getId(), cc);
		}
	}

	private Configuration emitFactoryIfChanged(String factoryPid, String filter, Dictionary<String, Object> settings)
			throws IOException {
		return updateIfChanged(createOrReuseFactoryConfiguration(factoryPid, filter), settings);
	}

	protected Configuration createOrReuseFactoryConfiguration(String factoryPid, final String filter)
			throws IOException {
		Configuration cc = null;
		try {
			Configuration[] c = configAdmin.listConfigurations(filter);
			if (c != null && c.length > 1) {
				logger.warn("Multiple configurations found for filter: {}", filter);
			}
			if (c != null && c.length > 0) {
				cc = c[0];
			}
		} catch (InvalidSyntaxException e) {
			logger.error("Error in filter: {}", filter, e);
		}
		if (cc == null) {
			cc = configAdmin.createFactoryConfiguration(factoryPid, null);
		}
		return cc;
	}

	private Configuration updateIfChanged(Configuration c, Dictionary<String, Object> settings) throws IOException {
		Dictionary<String, Object> old = c.getProperties();
		if (old != null) {
			if (!old.equals(settings)) {
				c.update(settings);
			} else {
				logger.info("Ignoring equal");
			}
		} else {
			// this will make this component 'own' this configuration, unsure if
			// this is desirable.
			c.update(settings);
		}
		return c;
	}
}
