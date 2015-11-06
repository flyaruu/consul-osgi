package com.dexels.sharedconfigstore.http.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.dexels.servicediscovery.http.api.HttpJsonApi;
import com.dexels.servicediscovery.http.api.HttpRawApi;

@Component(name="consul.http",configurationPolicy=ConfigurationPolicy.REQUIRE,property={"type=consul"})
public class ConsulHttpClient implements HttpJsonApi, HttpRawApi {
	
	private CloseableHttpClient syncClient;
	private ObjectMapper mapper;
	private String blockIntervalInSeconds = "20";
	private String consulServer = null;

	@Activate
    public void activate(Map<String, Object> settings) {
		mapper = new ObjectMapper().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
		int timeout = Integer.parseInt(blockIntervalInSeconds) + 10;

		consulServer = (String) settings.get("consulServer");
		        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();
        syncClient = HttpClients.custom().setDefaultRequestConfig(config).build();

    }
	
	@Override
	public JsonNode getJson(String path) throws IOException {
		final HttpGet request = new HttpGet(consulServer+path);
		return processJsonRequest(request,true);
	}

	@Override
	public JsonNode headJson(String path) throws IOException {
		final HttpHead request = new HttpHead(consulServer+path);
		return processJsonRequest(request,true);
	}
	
	@Override
	public JsonNode deleteJson(String path) throws IOException {
		final HttpDelete request = new HttpDelete(consulServer+path);
		return processJsonRequest(request,false);
	}
	
	@Override
	public JsonNode postJson(String path, JsonNode body,boolean expectReply) throws IOException {
		final HttpPost request = new HttpPost(consulServer+path);
		if(body!=null) {
			request.setEntity(createEntity(body));
		}
		return processJsonRequest(request,expectReply);
	}

	@Override
	public JsonNode putJson(String path, JsonNode body,boolean expectReply) throws IOException {
		debugCall("put",path,body);
		final HttpPut request = new HttpPut(consulServer+path);
		if(body!=null) {
			request.setEntity(createEntity(body));
		}
		return processJsonRequest(request,expectReply);
	}

	@Override
	public byte[] put(String path, byte[] body,boolean expectReply) throws IOException {
		final HttpPut request = new HttpPut(consulServer+path);
		if(body!=null) {
			request.setEntity(new ByteArrayEntity(body));
		}
		return processRawRequest(request,expectReply);
	}
	
	private void debugCall(String method, String path, JsonNode body) throws JsonGenerationException, JsonMappingException, IOException {
//		logger.info("Debugging {} request to path: {}",method,path);
		if(body!=null) {
			mapper.writer().withDefaultPrettyPrinter().writeValue(System.err, body);
		}
	}

	private JsonNode processJsonRequest(final HttpUriRequest request, boolean expectReply)
			throws IOException, ClientProtocolException, JsonProcessingException {
		CloseableHttpResponse response = syncClient.execute(request);
		try {
			if (response.getStatusLine().getStatusCode() >= 300) {
				return null;
			}
			if (expectReply) {
				return mapper.readTree(response.getEntity().getContent());
			}
		} finally {
			response.close();
		}
		return null;
	}

	private byte[] processRawRequest(final HttpUriRequest request, boolean expectReply)
			throws IOException, ClientProtocolException {
		CloseableHttpResponse response = syncClient.execute(request);
		try {
			if (response.getStatusLine().getStatusCode() >= 300) {
				return null;
			}
			if (expectReply) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				response.getEntity().writeTo(baos);
				return baos.toByteArray();
			}
		} finally {
			response.close();
		}
		return null;
	}

	private HttpEntity createEntity(JsonNode body) throws IOException, JsonGenerationException, JsonMappingException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			mapper.writeValue(baos,body);
			HttpEntity entity = new ByteArrayEntity(baos.toByteArray());
			return entity;
	}

	@Override
	public String getHost() {
		return this.consulServer;
	}

	@Override
	public byte[] get(String path) throws IOException {
		final HttpGet request = new HttpGet(consulServer+path);
		return processRawRequest(request,true);
	}

	@Override
	public byte[] head(String path) throws IOException {
		final HttpHead request = new HttpHead(consulServer+path);
		return processRawRequest(request,false);
	}

	@Override
	public byte[] delete(String path) throws IOException {
		final HttpDelete request = new HttpDelete(consulServer+path);
		return processRawRequest(request,false);
	}

	@Override
	public byte[] post(String path, byte[] body, boolean expectReply) throws IOException {
		final HttpPost request = new HttpPost(consulServer+path);
		if(body!=null) {
			request.setEntity(new ByteArrayEntity(body));
		}
		return processRawRequest(request,expectReply);
	}

}
