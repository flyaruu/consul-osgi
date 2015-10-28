package com.dexels.sharedconfigstore.http.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.codehaus.jackson.JsonNode;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.servicediscovery.http.api.HttpJsonApi;
import com.dexels.sharedconfigstore.consul.ConsulResourceEvent;
import com.dexels.sharedconfigstore.consul.ConsulResourceListener;
import com.dexels.sharedconfigstore.consul.LongPollingScheduler;

@Component(name = "dexels.consul.listener", immediate = true,configurationPolicy=ConfigurationPolicy.REQUIRE)
public class LongPollingHttpListenerImpl implements LongPollingScheduler {

	//    private Thread checkThread;
	private String consulServer = null;
	public static final String KVPREFIX = "/v1/kv/";
	public static final String CATALOG = "/v1/catalog/services";
	private String blockIntervalInSeconds = "20";
//	private Integer lastIndex = null;
//	private String lastValue = null;
	private final Map<String,Integer> lastIndexes = new HashMap<>();
	private final Map<String,JsonNode> lastValues = new HashMap<>();
	private final Map<String,LongPollingCallback> currentCallbacks = new HashMap<>();
	
	private final Set<ConsulResourceListener> resourceListeners = new HashSet<>();
	private final static Logger logger = LoggerFactory.getLogger(LongPollingHttpListenerImpl.class);
	private CloseableHttpAsyncClient client;
	private String servicePrefix;
	private String containerInfoPrefix;
	private HttpJsonApi consulClient;
	
	@Activate
    public void activate(Map<String, Object> settings) {
		this.servicePrefix = (String)settings.get("servicePrefix");
		this.containerInfoPrefix = (String)settings.get("containerInfoPrefix");
		int timeout = Integer.parseInt(blockIntervalInSeconds) + 10;
		consulServer = this.consulClient.getHost();
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();
        client = HttpAsyncClients.custom().setDefaultRequestConfig(config).build();
        client.start();
    }
	
	@Reference(unbind="clearConsulClient", policy=ReferencePolicy.DYNAMIC)
	public void setConsulClient(HttpJsonApi httpApi) {
		this.consulClient = httpApi;
	}

	public void clearConsulClient(HttpJsonApi httpApi) {
		this.consulClient = null;
	}

	@Override
	public void monitorURL(final String path) {
		Integer blockIndex = lastIndexes.get(path);
		String baseURL = consulServer+path+"?wait="+blockIntervalInSeconds+"s";
		final HttpGet get = (blockIndex!=null)?new HttpGet(baseURL+"&index="+blockIndex):new HttpGet(baseURL);
        try {
        	LongPollingCallback callback = new LongPollingCallback(get, path, this);
        	currentCallbacks.put(path, callback);
        	client.execute(get,callback);
        } catch (Exception e) {
            logger.error("Got Exception on performing GET: ", e);
        }
	}

	@Override
	public void callFailed(String key, int responseCode) {
        logger.warn("Failed calling: "+key+" with code: "+responseCode+" sleeping to be sure");
        try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
		}
        monitorURL(key);
	}

	@Override
	public void valueChanged(String key, JsonNode value, Integer index) {
		Integer prev = lastIndexes.get(key);
		JsonNode old = lastValues.get(key);
		
		if(prev!=null && prev.equals(index)) {
		} else {
			lastIndexes.put(key, index);
	        lastValues.put(key,value);
	        notifyListeners(key,old,value);
		}
        monitorURL(key);
	}

	private void notifyListeners(String key, JsonNode oldValue, JsonNode newValue) {
		try {
			ConsulResourceEvent cre = new ConsulResourceEvent(key, oldValue, newValue,servicePrefix,containerInfoPrefix);
			for (ConsulResourceListener c : resourceListeners) {
				c.resourceChanged(cre);
			}
		} catch (Exception e) {
			logger.error("Error delivering changes: ", e);
		}
	}

	@Deactivate
	public void deactivate() {
		// cancel all running requests
		// close sync/async connector
	}

	@Override
	public void addConsulResourceListener(ConsulResourceListener listener) {
		resourceListeners.add(listener);
	}
	
	@Override
	public void removeConsulResourceListener(ConsulResourceListener listener) {
		resourceListeners.remove(listener);
		
	}


}
