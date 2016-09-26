package org.insight_centre.aceis.io.streams.sparql2stream;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.insight_centre.citybench.main.CityBench;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class S2SResultListener implements UpdateListener {
	private String uri;
	private static final Logger logger = LoggerFactory.getLogger(S2SResultListener.class);
	
	public S2SResultListener(String uri) {
		setUri(uri);
	}
	
	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	@Override
	public void update(EventBean[] newEvents, EventBean[] oldEvents) {
		try {
			String result="";
			Map<String, Long> latencies = new HashMap<String, Long>();
			for(int i=0;i<newEvents.length;i++) {
				for(Entry<?, ?> entry:((Map<?, ?>)newEvents[i].getUnderlying()).entrySet()) {
					if(entry.getKey().equals("eventId")) {
						long initTime = CityBench.timeMap.get(entry.getValue().toString());
						latencies.put(entry.getValue().toString(), (System.currentTimeMillis() - initTime));
					}
				}
			}
			
			CityBench.pm.addResults(getUri(), latencies, 1);
//
//			// logger.info("CQELS result arrived: " + result);
//			if (!capturedResults.contains(result)) {
//				capturedResults.add(result);
//				// uncomment for testing the completeness, i.e., showing how many observations are captured
//				// logger.info("CQELS result arrived " + capturedResults.size() + ", obs size: " + capturedObIds.size()
//				// + ", result: " + result);
//				
//			} else {
//				logger.debug("CQELS result discarded: " + result);
//			}

		} catch (Exception e) {
			logger.error("CQELS decoding error: " + e.getMessage());
			e.printStackTrace();
		}
		// TODO Auto-generated method stub
		
	}
}
