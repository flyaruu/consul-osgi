package com.dexels.jsonhttp;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.dexels.jsonhttp.api.JsonHttpDriver;

@Component(name="http.json",configurationPolicy=ConfigurationPolicy.REQUIRE,immediate=true)
public class JsonHttpImpl implements JsonHttpDriver {

	CloseableHttpClient httpClient = null;
	private String address;
	private int port;
	private ObjectMapper mapper;
	
	@Activate
	public void activate(Map<String,Object> settings) {
		try {
			httpClient = HttpClients.createDefault();
			address = (String) settings.get("address");
			port = (int) settings.get("port");
			mapper = new ObjectMapper().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);;
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	@Deactivate
	public void deactivate() {
		if(httpClient!=null) {
			try {
				httpClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		httpClient = null;
	}

	@Override
	public JsonNode callJson(JsonNode input, String path,String method) throws ClientProtocolException, IOException {
		HttpUriRequest request = null;
		String uri = "http://"+address+":"+port+path;
		System.err.println("Method: "+method+" uri: "+uri);
		switch (method) {
		case "POST":
			HttpPost post = new HttpPost(uri);
			post.setEntity(new StringEntity(mapper.writeValueAsString(input)));
			request = post;
			break;
		case "GET":
			request = new HttpGet(uri);
			break;
		case "PUT":
			HttpPut put = new HttpPut(uri);
			put.setEntity(new StringEntity(mapper.writeValueAsString(input)));
			request = put;
			break;
		case "DELETE":
			request = new HttpDelete(uri);
			break;

		default:
			break;
		}
		if(request!=null) {
			CloseableHttpResponse response = httpClient.execute(request);
			if(response.getStatusLine().getStatusCode()>=300) {
				System.err.println("Http error: "+response.getStatusLine().getStatusCode());
				return null;
			}
			JsonNode reply = mapper.readTree(response.getEntity().getContent());
			response.close();
			return reply;
			
		}
		return null;
	}

}
