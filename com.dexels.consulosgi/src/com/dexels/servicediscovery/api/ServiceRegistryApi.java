package com.dexels.servicediscovery.api;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface ServiceRegistryApi {

	public String registerService(String name, String host, int port, String alias, Map<String, Object> attributes,
			Set<String> tags) throws IOException;

	public void deregisterService(String id);
	public void cleanAttributes(String host, int port) throws IOException;

}
