package com.dexels.servicediscovery.network.api;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface PortDiscoverer {
	public NetworkLocation getHostPort(int port);
}
