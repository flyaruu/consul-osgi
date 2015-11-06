package com.dexels.servicediscovery.consul.impl;

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

@Component(name="dexels.consul.configurator", configurationPolicy=ConfigurationPolicy.IGNORE,immediate=true)
public class ConsulEnvConfigurator {

	private ConfigurationAdmin configAdmin;

	private Configuration configuration = null;
	
	private final static Logger logger = LoggerFactory.getLogger(ConsulEnvConfigurator.class);

	
	@Activate
	public void activate() {
		String consulURL = System.getenv("CONSUL_URL");
		if(consulURL!=null) {
			try {
				Dictionary<String,Object> settings = new Hashtable<>();
				settings.put("consulServer", consulURL);
				this.configuration = configAdmin.getConfiguration("consul.http");
				ConfigurationUtils.updateIfChanged(this.configuration, settings);
			} catch (IOException e) {
				logger.error("Error: ", e);
			}
		}
	}

	@Deactivate
	public void deactivate() {
		if(this.configuration!=null) {
			try {
				this.configuration.delete();
			} catch (IOException e) {
				logger.error("Error: ", e);
			}
		}
	}
	
	@Reference
	public void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
		this.configAdmin = configAdmin;
	}
}
