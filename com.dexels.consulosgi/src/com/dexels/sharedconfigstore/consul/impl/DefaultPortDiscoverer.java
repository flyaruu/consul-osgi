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
			return new NetworkLocation("127.0.0.1",port);
//		return new NetworkLocation("localhost", port);
	}

}
