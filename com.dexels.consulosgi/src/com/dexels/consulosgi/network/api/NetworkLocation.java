package com.dexels.consulosgi.network.api;

public class NetworkLocation {
	private final String host;
	private final int port;
	
	public NetworkLocation(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public String getHost() {
		return this.host;
	}
	public int getPort() {
		return this.port;
	}
}
