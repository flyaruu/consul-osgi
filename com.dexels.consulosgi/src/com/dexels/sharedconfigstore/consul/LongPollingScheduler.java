package com.dexels.sharedconfigstore.consul;

import org.codehaus.jackson.JsonNode;

public interface LongPollingScheduler {
	public void callFailed(String key, int responseCode);

	public void valueChanged(String key, JsonNode value, Integer index);

	public void addConsulResourceListener(ConsulResourceListener listener);

	public void removeConsulResourceListener(ConsulResourceListener listener);


	public void monitorURL(String string);



}
