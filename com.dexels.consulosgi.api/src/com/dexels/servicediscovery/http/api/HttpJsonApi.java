package com.dexels.servicediscovery.http.api;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface HttpJsonApi {
	public JsonNode getJson(String path) throws IOException;
	public JsonNode headJson(String path) throws IOException;
	public JsonNode deleteJson(String path) throws IOException;
	public JsonNode postJson(String path, JsonNode body, boolean expectReply) throws IOException;
	public JsonNode putJson(String path, JsonNode body, boolean expectReply) throws IOException;
	public String getHost();
}
