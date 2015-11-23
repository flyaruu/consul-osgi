package com.dexels.servicediscovery.api;

import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface PublishedService {

	public Map<String, Object> getServiceAttributes();
	public int getPort();
	public Set<String> getTags();
	public String getId();
	public String getHost();
}
