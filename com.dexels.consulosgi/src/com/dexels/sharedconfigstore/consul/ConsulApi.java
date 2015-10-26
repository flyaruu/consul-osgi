package com.dexels.sharedconfigstore.consul;

import java.io.IOException;
import java.util.Map;

public interface ConsulApi {

	public String registerService(String name, String host, int port, String alias, Map<String, Object> attributes,
			String tags) throws IOException;

	public void deregisterService(String id);
	public void cleanAttributes(String host, int port) throws IOException;

}
