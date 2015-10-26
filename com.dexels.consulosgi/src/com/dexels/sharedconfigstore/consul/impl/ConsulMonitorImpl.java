package com.dexels.sharedconfigstore.consul.impl;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.sharedconfigstore.consul.ConsulResourceEvent;
import com.dexels.sharedconfigstore.consul.ConsulResourceListener;
import com.dexels.sharedconfigstore.consul.DiscoveredService;
import com.dexels.sharedconfigstore.consul.LongPollingScheduler;
import com.dexels.sharedconfigstore.consul.PublishedService;
import com.dexels.sharedconfigstore.http.HttpApi;

@Component(name="dexels.consul.monitor",immediate=true)
public class ConsulMonitorImpl implements ConsulResourceListener {
	
	private LongPollingScheduler consulListener = null;
	private HttpApi httpApi = null;

	private final Map<String,DiscoveredServiceImpl> detectedServices = new HashMap<>();
	private ConfigurationAdmin configAdmin;
	private final Map<String,Configuration> resourcePids = new HashMap<>();
	private final Map<String,Configuration> localResourcePids = new HashMap<>();
	
	private final Map<Integer,Map<String,PublishedService>> sharedServices = new HashMap<>();
	
	private final static Logger logger = LoggerFactory.getLogger(ConsulMonitorImpl.class);
	
	@Activate
	public void activate() {
		consulListener.addConsulResourceListener(this);
		// is this the right place?
		consulListener.monitorURL("/v1/catalog/services");
	}
	
	public void deactivate() {
		consulListener.removeConsulResourceListener(this);
		// remove resourcePids (& localResourcePids)
	}
	
	@Reference(unbind="clearConsulListener",policy=ReferencePolicy.DYNAMIC)
	public void setConsulListener(LongPollingScheduler scheduler) {
		consulListener = scheduler;
	}

	public void clearConsulListener(LongPollingScheduler scheduler) {
		consulListener = null;
	}
	
	
	@Reference(unbind="clearHttpApi",policy=ReferencePolicy.DYNAMIC)
	public void setHttpApi(HttpApi httpApi) {
		this.httpApi = httpApi;
	}

	public void clearHttpApi(HttpApi httpApi) {
		this.httpApi = null;
	}

	@Override
	public void resourceChanged(ConsulResourceEvent event) {
		try {
			ObjectNode services = (ObjectNode) event.getNewValue();
			Iterator<String> names = services.getFieldNames();
			
			// copy, so orphans can be detected
			Map<String,DiscoveredServiceImpl> remaining = new HashMap<>(detectedServices);
			Map<String,DiscoveredServiceImpl> newServices = new HashMap<>();
			ObjectMapper mapper = new ObjectMapper().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);;
			while (names.hasNext()) {
				try {
					String serviceName = names.next();
					ArrayNode serviceDetails = (ArrayNode) httpApi.get("/v1/catalog/service/"+serviceName);
					for (JsonNode serv : serviceDetails) {
						String serviceId = ((ObjectNode)serv).get("ServiceID").asText();
						String url = "/v1/kv/"+event.getServicePrefix()+"/"+serviceId+"?recurse";
						JsonNode serviceAttributes = httpApi.get(url);

						String containerInfo = "/v1/kv/"+event.getContainerInfoPrefix()+"/"+serviceId+"?recurse";
						JsonNode containerInfoAttributes = httpApi.get(containerInfo);
						System.err.println("Querying container info from: " + containerInfo);
						if (containerInfoAttributes != null) {
							mapper.writerWithDefaultPrettyPrinter().writeValue(System.err, containerInfoAttributes);
						}

						DiscoveredServiceImpl cs = new DiscoveredServiceImpl((ObjectNode)serv, (ArrayNode)serviceAttributes,(ArrayNode)containerInfoAttributes,event.getServicePrefix(),event.getContainerInfoPrefix());
						if(remaining.containsKey(serviceId)) {
							// tick off
							logger.info("ServiceId: "+serviceId+" is still present");
							remaining.remove(serviceId);
						} else {
							logger.info("ServiceId: "+serviceId+"  seems to be new");
							newServices.put(serviceId, cs);
						}
					}
				} catch (Throwable e) {
					logger.error("Error: ", e);
				}
			}
			for (Entry<String,DiscoveredServiceImpl> e : remaining.entrySet()) {
				logger.info("removing: "+e.getKey());
				removeService(e.getKey(),e.getValue());
				detectedServices.remove(e.getKey());
			}
			for (Entry<String,DiscoveredServiceImpl> e : newServices.entrySet()) {
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
    
	private void removeService(String key, DiscoveredService value) {
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

	private void addService(String key, DiscoveredServiceImpl value) throws IOException {
		String containerHostname = System.getenv("HOSTNAME");
		
		boolean isMe = containerHostname!=null && containerHostname.equals(value.getContainerHostname());
		logger.info("My ContainerHostname: "+containerHostname+" discovered: "+value.getContainerHostname());
		String type = value.getAttributes().get("type");
		if(type!=null) {
			String factoryPid = type;
			String filter = "(id="+value.getId()+")";
			Configuration cc = emitFactoryIfChanged(factoryPid, filter, value.createDictionary());
			resourcePids.put(value.getId(), cc);
			if(isMe) {
				addLocalService(value, type);
			}
		}
	}

	@Reference(unbind="removeSharedService",cardinality=ReferenceCardinality.MULTIPLE,policy=ReferencePolicy.DYNAMIC)
	public void addSharedService(PublishedService service) {
		int port = service.getPort();
		String id = service.getId();
		Map<String,PublishedService> shared = this.sharedServices.get(port);
		if(shared==null) {
			shared = new HashMap<>();
			this.sharedServices.put(port, shared);
		}
		shared.put(id, service);
	}
	
	public void removeSharedService(PublishedService service) {
		int port = service.getPort();
//		String id = service.getId();
		Map<String,PublishedService> shared = this.sharedServices.get(port);
		shared.remove(service);
	}
	
	private void addLocalService(DiscoveredServiceImpl value, String type) throws IOException {
		String localFactoryPid = "local."+type;
		String localFilter = "(serviceId="+value.getId()+")";
		Configuration localConfig = emitFactoryIfChanged(localFactoryPid, localFilter, value.createDictionary());
		localConfig.update();
		logger.info("Emitting config with factoryPid: {} and local settings: {} and filter: {}",localFactoryPid,value.getAttributes(),null);
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
			} else {
				logger.info("No config found");
			}
		} catch (InvalidSyntaxException e) {
			logger.error("Error in filter: {}", filter, e);
		}
		if (cc == null) {
			logger.info("creating.");
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
			logger.info("updating");
		}
		return c;
	}
}
