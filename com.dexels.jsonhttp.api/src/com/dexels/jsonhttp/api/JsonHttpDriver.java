package com.dexels.jsonhttp.api;

import java.io.IOException;

import org.osgi.annotation.versioning.ConsumerType;

import com.fasterxml.jackson.databind.JsonNode;

@ConsumerType
public interface JsonHttpDriver {
	public JsonNode callJson(JsonNode input, String path,String method) throws IOException;
}
