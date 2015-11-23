package com.dexels.servicediscovery.http.api;

import java.io.IOException;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface HttpRawApi {
	public byte[] get(String path) throws IOException;
	public byte[] head(String path) throws IOException;
	public byte[] delete(String path) throws IOException;
	public byte[] post(String path, byte[] body, boolean expectReply) throws IOException;
	public byte[] put(String path, byte[] body, boolean expectReply) throws IOException;
	public String getHost();
}
