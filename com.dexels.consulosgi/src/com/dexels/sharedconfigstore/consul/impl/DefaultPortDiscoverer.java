package com.dexels.sharedconfigstore.consul.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.osgi.service.component.annotations.Component;

import com.dexels.sharedconfigstore.consul.NetworkLocation;
import com.dexels.sharedconfigstore.consul.PortDiscoverer;

@Component(name="port.discoverer.default")
public class DefaultPortDiscoverer implements PortDiscoverer {

	@Override
	public NetworkLocation getHostPort(int port) {
		try {
			return new NetworkLocation(InetAddress.getLocalHost().getHostAddress(),port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return new NetworkLocation("localhost", port);
	}

}
