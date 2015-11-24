
package com.dexels.sharedconfigstore.http.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.concurrent.FutureCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

public class LongPollingCallback implements FutureCallback<HttpResponse> {

	
	private final static Logger logger = LoggerFactory.getLogger(LongPollingCallback.class);
	private final String key;
	private final HttpRequestBase request;
	private final LongPollingHttpListenerImpl scheduler;
	private final boolean recurse;
//	private final ObjectMapper mapper = new ObjectMapper();
//	private HttpCache cache;
	
	public LongPollingCallback(HttpRequestBase request, String key, LongPollingHttpListenerImpl scheduler) {
		this.key = key;
		this.request = request;
		this.scheduler = scheduler;
		// not pretty:
		this.recurse = request.getURI().getQuery().contains("recurse");  //request.getURI().getQuery() // ("recurse").length > 0;
	}
	
	public void completed(final HttpResponse response) {
        try {
			int responseCode = response.getStatusLine().getStatusCode();
			if (responseCode >= 300) {
			    // No data yet or something is broken. Lets sleep for a little while to prevent flooding the server
//				logger.warn("Request: "+this.request);
//				logger.warn("Request uri: "+this.request.getURI());
//			    logger.warn("Got responsecode {}: {} for url: {}", responseCode, response.getStatusLine().getReasonPhrase(),key);
			    scheduler.callFailed(key, responseCode);
			}
			try {
					ByteArrayOutputStream reply = new ByteArrayOutputStream();
					response.getEntity().writeTo(reply);
				    Integer index = getConsulIndex(response);
				    scheduler.valueChanged(key, reply.toByteArray(),index,recurse);

			} catch (JsonProcessingException e) {
				logger.error("Error: ", e);
			} catch (IOException e) {
				logger.error("Error: ", e);
			}
		} catch (Throwable e) {
			logger.error("Failure in HTTP callback: ",e);
		}
    }
	
	private Integer getConsulIndex(HttpResponse response) {
        Header consulIndex = response.getFirstHeader("X-Consul-Index");
        if(consulIndex!=null) {
        	return Integer.parseInt(consulIndex.getValue());
        }
        return null;
	}

    public void failed(final Exception ex) {
    	logger.error("Error calling path: {} full url: ",key,request.getURI(), ex);
    	scheduler.callFailed(key, -1);
    }

//    @Reference(unbind="clearCache",policy=ReferencePolicy.DYNAMIC)
//    public void setCache(HttpCache cache) {
//    	this.cache = cache;
//    }
//    
//    public void clearCache(HttpCache cache) {
//    	this.cache = null;
//    }
    
    public void cancelled() {
    	logger.warn("Call cancelled: "+request.getURI());
    	scheduler.callFailed(key, -1);
    }

    public void cancel() {
    	this.request.abort();
    }
}
