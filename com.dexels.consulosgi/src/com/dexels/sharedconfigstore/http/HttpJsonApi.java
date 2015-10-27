package com.dexels.sharedconfigstore.http;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;

public interface HttpJsonApi {
	public JsonNode getJson(String path) throws IOException;
	public JsonNode headJson(String path) throws IOException;
	public JsonNode deleteJson(String path) throws IOException;
	public JsonNode postJson(String path, JsonNode body, boolean expectReply) throws IOException;
	public JsonNode putJson(String path, JsonNode body, boolean expectReply) throws IOException;
	public String getHost();
}
