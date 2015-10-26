package com.dexels.sharedconfigstore.consul.test;

import java.util.HashMap;
import java.util.Map;

import com.dexels.sharedconfigstore.consul.impl.ConsulMonitorImpl;
import com.dexels.sharedconfigstore.http.impl.LongPollingHttpListenerImpl;

public class ConsulTest {

	public static void main(String[] args) throws InterruptedException {
		final LongPollingHttpListenerImpl lphl = new LongPollingHttpListenerImpl();
//		final ObjectMapper mapper = new ObjectMapper().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);;
		final Map<String,Object> settings = new HashMap<>();
		settings.put("consulServer", "http://192.168.99.102:8500");
		settings.put("servicePrefix", "serviceAttributes");
		lphl.activate(settings);
		
		ConsulMonitorImpl cmi = new ConsulMonitorImpl();
		cmi.setConsulListener(lphl);
		cmi.activate();

		Thread.sleep(100000);

	}

}
