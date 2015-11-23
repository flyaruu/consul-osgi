package com.dexels.servicediscovery.http.api;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public class KeyChange {
	private final int createIndex;
	private final int modifyIndex;
	private final int lockIndex;
	private final String key;
	private final String value;
	private final byte[] decodedValue;
	private final long flags;

	public KeyChange(JsonNode node) {
		ObjectNode change = (ObjectNode)node;
		this.createIndex = change.get("CreateIndex").asInt();
		this.modifyIndex = change.get("ModifyIndex").asInt();
		this.lockIndex = change.get("LockIndex").asInt();
		this.key = change.get("Key").asText();
		this.value = change.get("Value").asText();
		this.decodedValue = Base64.decodeBase64(value);
		this.flags = change.get("Flags").asLong();
	}

	public int getCreateIndex() {
		return createIndex;
	}

	public int getModifyIndex() {
		return modifyIndex;
	}

	public int getLockIndex() {
		return lockIndex;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public byte[] getDecodedValue() {
		return decodedValue;
	}

	public long getFlags() {
		return flags;
	}

}
