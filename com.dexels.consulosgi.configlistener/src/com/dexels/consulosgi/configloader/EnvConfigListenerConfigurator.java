package com.dexels.consulosgi.configloader;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.servicediscovery.utils.ConfigurationUtils;

@Component(name="dexels.consul.configlistener.env",configurationPolicy=ConfigurationPolicy.IGNORE)
public class EnvConfigListenerConfigurator {
	
	private Configuration configListenerConfiguration;
	private ConfigurationAdmin configAdmin;

	private final static Logger logger = LoggerFactory.getLogger(EnvConfigListenerConfigurator.class);

	
	@Activate
	public void activate() {
		String clusterName = System.getenv("CLUSTER");
		
		if(clusterName==null) {
			logger.info("No cluster defined, not injecting dexels.consul.configlistener config");
			return;
		}
		try {
			configListenerConfiguration = ConfigurationUtils.createOrReuseFactoryConfiguration(configAdmin,
					"dexels.consul.configlistener", "(id=owned_by_configloader)",true);
			Dictionary<String, Object> props = new Hashtable<>();
			props.put("id", "owned_by_configloader");
			props.put("name", clusterName);
			configListenerConfiguration.update(props);

		} catch (IOException e) {
			logger.error("Error: ", e);
		}
	}
	
	@Deactivate
	public void deactivate() {
		if(configListenerConfiguration!=null) {
			try {
				configListenerConfiguration.delete();
			} catch (IOException e) {
				logger.error("Error: ", e);
			}
		}
	}


	@Reference(name = "ConfigAdmin", unbind = "clearConfigAdmin")
	public void setConfigAdmin(ConfigurationAdmin configAdmin) {
		this.configAdmin = configAdmin;
	}

	public void clearConfigAdmin(ConfigurationAdmin configAdmin) {
		this.configAdmin = null;
	}

}
