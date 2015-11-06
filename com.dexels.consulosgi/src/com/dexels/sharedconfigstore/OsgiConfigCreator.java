package com.dexels.sharedconfigstore;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.servicediscovery.utils.ConfigurationUtils;

@Component(name = "dexels.configstore.configcreator", immediate = true,property={"event.topics=consul/change/sharedstore/*"})
public class OsgiConfigCreator implements EventHandler {
    private final static Logger logger = LoggerFactory.getLogger(OsgiConfigCreator.class);
    private ConfigurationAdmin configAdmin;
    private ObjectMapper mapper = new ObjectMapper();
	private String instanceName;
	private String kvPrefix;
    
    
	@Activate
    public void activate() {
    	this.instanceName = "test-knvb-1";
    	this.kvPrefix = "sharedstore";
    }

    @Reference(name = "ConfigAdmin", unbind = "clearConfigAdmin")
    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public void clearConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = null;
    }

	@SuppressWarnings("unchecked")
	@Override
	public void handleEvent(Event event) {
        String key = (String) event.getProperty("key");
        byte[] bb = (byte[]) event.getProperty("data");
        
        if(!checkPrefix(key)) {
        	return;
        }
        logger.info("OSGIHANDLE: "+new String(bb));
        try {
            ObjectNode on = (ObjectNode) mapper.readTree(bb);
            Map<String, Object> result = mapper.convertValue(on, Map.class);
            Dictionary<String, Object> dict = new Hashtable<>();
            
            for (Map.Entry<String, Object> e : result.entrySet()) {
            	dict.put(e.getKey(), e.getValue());
			}
            String id =  on.get("id").asText();
            String factoryPid = createKey(key);
//            logger.info("Create configuration with id: {} and factoryPid: {}. Res: {}",id,factoryPid,result);
			ConfigurationUtils.emitFactoryIfChanged(configAdmin, factoryPid, createFilter(id, factoryPid), dict);
		} catch (IOException e) {
			logger.error("Error: ", e);
		} catch (Throwable t) {
			logger.error("Catch all in eventHandler: ",t);
		}
	}

	private String createFilter(String id, String factoryPid) {
		return "(&(service.factoryPid="+factoryPid+")(id="+id+"))";
	}
	private boolean checkPrefix(String key) {
		String expected = kvPrefix+"/"+instanceName;
		if(key.startsWith(expected)) {
			return true;
		}
		return false;
	}

	private String createKey(String key) {
		String pid = key.substring(key.lastIndexOf("/")+1, key.length());
		return pid;
	}

}
