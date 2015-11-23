package com.dexels.servicediscovery.http.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public class ChangeEvent {
	
	private final Map<String,KeyChange> changeMap = new HashMap<>();

	public ChangeEvent(List<KeyChange> changes) {
		for (KeyChange keyChange : changes) {
			changeMap.put(keyChange.getKey(), keyChange);
		}
	}
	
	public Map<String,KeyChange> getChanges() {
		return Collections.unmodifiableMap(changeMap);
	}
}
