package com.dexels.sharedconfigstore.http;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;

public interface HttpApi {
	public JsonNode get(String path) throws IOException;
	public JsonNode head(String path) throws IOException;
	public JsonNode delete(String path) throws IOException;
	public JsonNode post(String path, JsonNode body, boolean expectReply) throws IOException;
	public JsonNode put(String path, JsonNode body, boolean expectReply) throws IOException;
	public String getHost();
}
