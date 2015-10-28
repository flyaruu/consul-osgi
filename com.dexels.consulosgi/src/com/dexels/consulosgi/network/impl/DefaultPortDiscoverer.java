package com.dexels.consulosgi.network.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.osgi.service.component.annotations.Component;

import com.dexels.consulosgi.network.api.NetworkLocation;
import com.dexels.consulosgi.network.api.PortDiscoverer;

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
