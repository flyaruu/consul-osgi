package com.dexels.servicediscovery.http.api;

public interface HttpCache {

	public byte[] getValue(String path);
	public void processChange(ChangeEvent ce);
	public enum Events {CREATE,DELETE,MODIFY}

}
