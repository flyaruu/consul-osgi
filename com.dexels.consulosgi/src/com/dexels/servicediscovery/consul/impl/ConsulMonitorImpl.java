package com.dexels.servicediscovery.consul.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.servicediscovery.api.DiscoveredService;
import com.dexels.servicediscovery.api.PublishedService;
import com.dexels.servicediscovery.http.api.HttpJsonApi;
import com.dexels.servicediscovery.utils.ConfigurationUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component(name="consul.http.monitor",enabled=true, immediate=true,configurationPolicy=ConfigurationPolicy.REQUIRE,property={"event.topics=consul/v1/catalog/services"})
public class ConsulMonitorImpl implements EventHandler {
	
//private static final String SERVICES = "/v1/catalog/services";

	//	private LongPollingScheduler consulListener = null;
	private HttpJsonApi httpApi = null;

	private final Map<String,DiscoveredServiceImpl> detectedServices = new HashMap<>();
	private ConfigurationAdmin configAdmin;
	private final Map<String,Configuration> resourcePids = new HashMap<>();
	
	private String servicePrefix;
	private String containerInfoPrefix;

	private Configuration monitorConfiguration;
	
	private final Map<Integer,Map<String,PublishedService>> sharedServices = new HashMap<>();
	
	private final static Logger logger = LoggerFactory.getLogger(ConsulMonitorImpl.class);
	
	private final ObjectMapper mapper = new ObjectMapper();
	
	@Activate
	public void activate(Map<String,Object> settings, BundleContext bundleContext) {
		this.servicePrefix = (String)settings.get("servicePrefix");
		this.containerInfoPrefix = (String)settings.get("containerInfoPrefix");
//		try {
//			monitorConfiguration = ConfigurationUtils.createOrReuseFactoryConfiguration(configAdmin, "dexels.consul.listener", "(id=owned_by_monitor)");
//			Dictionary<String,Object> props = new Hashtable<>();
//			props.put("path", SERVICES);
//			props.put("id", "owned_by_monitor");
//			monitorConfiguration.update(props);
//		
//		} catch (IOException e) {
//			logger.error("Error: ", e);
//		}		
	}

	@Deactivate
	public void deactivate() {
		// remove resourcePids (& localResourcePids)
		if(monitorConfiguration!=null) {
			try {
				monitorConfiguration.delete();
			} catch (IOException e) {
				logger.error("Error: ", e);
			}
		}
	}
	
	@Reference(unbind="clearHttpApi",policy=ReferencePolicy.DYNAMIC,target="(type=consul)")
	public void setHttpApi(HttpJsonApi httpApi) {
		this.httpApi = httpApi;
	}

	public void clearHttpApi(HttpJsonApi httpApi) {
		this.httpApi = null;
	}

	@Override
	public void handleEvent(Event event) {
		try {
			logger.info("Event for monitor");
			byte[] input = (byte[]) event.getProperty("new");
			if(input==null) {
				logger.warn("No input data. Ignoring");
				return;
			}
			logger.info("Received data: {}",new String(input));
			ObjectNode services = (ObjectNode) mapper.readTree(input);
//			ObjectNode services = (ObjectNode) event.getNewValue();
			Iterator<String> names = services.fieldNames();
			
			
			// copy, so orphans can be detected
			Map<String,DiscoveredServiceImpl> remaining = new HashMap<>(detectedServices);
			Map<String,DiscoveredServiceImpl> newServices = new HashMap<>();
			ObjectMapper mapper = new ObjectMapper().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);;
			while (names.hasNext()) {
				try {
					String serviceName = names.next();
					ArrayNode serviceDetails = (ArrayNode) httpApi.getJson("/v1/catalog/service/"+serviceName);
					for (JsonNode serv : serviceDetails) {
						String serviceId = ((ObjectNode)serv).get("ServiceID").asText();
						String serviceAddress = ((ObjectNode)serv).get("ServiceAddress").asText();
						int servicePort = ((ObjectNode)serv).get("ServicePort").asInt();
						String url = "/v1/kv/"+this.servicePrefix+"/"+serviceAddress+"/"+servicePort+"/"+serviceId+"?recurse";
						JsonNode serviceAttributes = httpApi.getJson(url);

						String containerInfo = "/v1/kv/"+this.containerInfoPrefix+"/"+serviceId+"?recurse";
						JsonNode containerInfoAttributes = httpApi.getJson(containerInfo);
						if (containerInfoAttributes != null) {
							mapper.writerWithDefaultPrettyPrinter().writeValue(System.err, containerInfoAttributes);
						}

						DiscoveredServiceImpl cs = new DiscoveredServiceImpl((ObjectNode)serv, (ArrayNode)serviceAttributes,(ArrayNode)containerInfoAttributes,this.servicePrefix,this.containerInfoPrefix);
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
		} catch (Throwable e) {
			logger.error("Catch all in eventhandler: ", e);
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
//		System.err.println("Attributes: "+value.getAttributes().keySet());
		if(type!=null) {
			String factoryPid = type;
			String filter = "(id="+value.getId()+")";
			Configuration cc = ConfigurationUtils.emitFactoryIfChanged(configAdmin, factoryPid, filter, value.createDictionary());
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
		Configuration localConfig = ConfigurationUtils.emitFactoryIfChanged(configAdmin,localFactoryPid, localFilter, value.createDictionary());
		localConfig.update();
		logger.info("Emitting config with factoryPid: {} and local settings: {} and filter: {}",localFactoryPid,value.getAttributes(),null);
	}


}
