package com.dexels.sharedconfigstore.consul;

public interface ConsulResourceListener {
	public void resourceChanged(ConsulResourceEvent event);
}
