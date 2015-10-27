package com.dexels.sharedconfigstore.http;

import java.io.IOException;

public interface HttpRawApi {
	public byte[] get(String path) throws IOException;
	public byte[] head(String path) throws IOException;
	public byte[] delete(String path) throws IOException;
	public byte[] post(String path, byte[] body, boolean expectReply) throws IOException;
	public byte[] put(String path, byte[] body, boolean expectReply) throws IOException;
	public String getHost();
}
