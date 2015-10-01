package com.dexels.jsonexample;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.dexels.jsonhttp.api.JsonHttpDriver;

@Component(name="elasticsearch.inserter",service={},immediate=true)
public class ElasticSearchInserter implements Runnable{
	private Thread thread;
	private boolean active = false;
	private JsonHttpDriver jsonHttpDriver;
	  private SecureRandom random = new SecureRandom();
	private final ObjectMapper mapper = new ObjectMapper().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
	private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");


	@Activate
	public void activate() {
		this.thread = new Thread(this);
		this.active = true;
		this.thread.start();
	}

	@Deactivate
	public void deactivate() {
		this.active = false;
		if(this.thread!=null) {
			this.thread.interrupt();
		}
	}
	@Reference(policy=ReferencePolicy.DYNAMIC,unbind="clearJsonHttpDriver")
	public void setJsonHttpDriver(JsonHttpDriver driver) {
		this.jsonHttpDriver = driver;
	}
	
	public void clearJsonHttpDriver(JsonHttpDriver driver) {
		this.jsonHttpDriver = null;
	}

	@Override
	public void run() {
		while(active) {
			try {
				String index = nextIndexId();
				JsonNode request = createInput(index);
				JsonNode result = jsonHttpDriver.callJson(request, "/logstash-1/"+index, "POST");
				System.err.println("Request:");
				mapper.writerWithDefaultPrettyPrinter().writeValue(System.err, request);

				System.err.println("Inserting: "+index);
//				mapper.writerWithDefaultPrettyPrinter().writeValue(System.err, result);
			} catch (Throwable e) {
				e.printStackTrace();
			}finally {
				try {
					Thread.sleep(random.nextInt(500));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	  private JsonNode createInput(String index) {
		  ObjectNode result = mapper.createObjectNode();
//		     df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		  System.err.println("DF: "+dateFormat.format(new Date()));
//		  result.put("@timestamp", "2015-10-01T05:10:26.336Z");
		  result.put("@timestamp", dateFormat.format(new Date()));
		  result.put("name", index);
		  return result;
	}

	public String nextIndexId() {
		    return new BigInteger(130, random).toString(32);
		  }
		
}
