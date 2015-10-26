package com.dexels.sharedconfigstore.consul;

public interface PortDiscoverer {
	public NetworkLocation getHostPort(int port);
}
