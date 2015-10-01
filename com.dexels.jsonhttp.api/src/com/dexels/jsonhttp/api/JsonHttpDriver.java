package com.dexels.jsonhttp.api;

import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface JsonHttpDriver {
	public JsonNode callJson(JsonNode input, String path,String method) throws IOException;
}
