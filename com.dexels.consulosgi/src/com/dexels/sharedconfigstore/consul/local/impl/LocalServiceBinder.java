package com.dexels.sharedconfigstore.consul.local.impl;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.dexels.sharedconfigstore.consul.PublishedService;
import com.dexels.sharedconfigstore.consul.DiscoveredService;



@Component(name="dexels.servicebinder")
public class LocalServiceBinder {
	
	private final Map<Integer,PublishedService> localSharedService = new HashMap<>();
	private final Map<Integer,DiscoveredService> networkServices = new HashMap<>();
	
	@Reference(unbind="removeLocalService",policy=ReferencePolicy.DYNAMIC)
	public void addLocalService(PublishedService sharedService) {
		localSharedService.put(sharedService.getPort(), sharedService);
		attemptBind(sharedService.getPort());
	}

	public void removeLocalService(PublishedService sharedService) {
		localSharedService.remove(sharedService.getPort());
		attemptUnbind(sharedService.getPort());
	}

	@Reference(unbind="removeNetworkService",policy=ReferencePolicy.DYNAMIC)
	public void addNetworkService(DiscoveredService networkService) {
		networkServices.put(networkService.getPort(), networkService);
		attemptBind(networkService.getPort());

	}

	public void removeNetworkService(DiscoveredService networkService) {
		networkServices.remove(networkService.getPort());
		attemptUnbind(networkService.getPort());
	}
	

	private void attemptBind(int port) {
		DiscoveredService ns = networkServices.get(port);
		PublishedService ls = localSharedService.get(port);
		if(ns!=null && ls!=null) {
			
		}
	}

	private void attemptUnbind(int port) {
		// TODO Auto-generated method stub
		
	}

}


