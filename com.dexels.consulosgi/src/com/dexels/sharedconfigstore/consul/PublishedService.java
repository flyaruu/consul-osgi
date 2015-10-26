package com.dexels.sharedconfigstore.consul;

import java.util.Map;
import java.util.Set;

public interface PublishedService {

	public Map<String, Object> getServiceAttributes();
	public int getPort();
	public Set<String> getTags();
	public String getId();
	public String getHost();
}
