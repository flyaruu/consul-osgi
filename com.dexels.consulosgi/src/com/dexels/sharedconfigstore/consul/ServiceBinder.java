package com.dexels.sharedconfigstore.consul;

public interface ServiceBinder {
	public void bind(PublishedService local, DiscoveredService network);
	public void unbind(PublishedService local, DiscoveredService network);
}
