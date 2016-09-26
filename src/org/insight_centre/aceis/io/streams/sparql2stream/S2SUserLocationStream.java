package org.insight_centre.aceis.io.streams.sparql2stream;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.insight_centre.citybench.main.CityBench;

import com.espertech.esper.client.EPServiceProvider;

public class S2SUserLocationStream extends S2SSensorStream implements Runnable {
	EPServiceProvider epService = null;
	String SEPARATOR = ",";
	List<String> header = new ArrayList<String>();

	public S2SUserLocationStream(EPServiceProvider epService, String streamName, String srcFile) {
		this.epService = epService;
		this.streamName = streamName;
		Map<String,Object> streamDefinition = new LinkedHashMap<String, Object>();
		streamDefinition.put("eventId", String.class);
		streamDefinition.put("user", String.class);
		streamDefinition.put("lat", Double.class);
		streamDefinition.put("lon", Double.class);
		epService.getEPAdministrator().getConfiguration().addEventType(streamName, streamDefinition);
		
		try {
			br = new BufferedReader(new FileReader(srcFile));
			for(String headerEl:br.readLine().split(SEPARATOR)) {
				header.add(headerEl);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendEvent(String line) {
		String eventId = UUID.randomUUID().toString();
		CityBench.timeMap.put(eventId, System.currentTimeMillis());
		epService.getEPRuntime().sendEvent(fillData(line,eventId), streamName);
	}
	
	private Map<String, Object> fillData(String line,String eventId) {
		Map<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("eventId", eventId);
		String[] parts = line.split(SEPARATOR);
		for(String headerEl:header) {
			data.put(headerEl, parts[header.indexOf(headerEl)]);
		}
		return data;
	}
	
	@Override
	public void run() {
		logger.info("Starting sensor stream: " + this.streamName);
		try {
			String line = "";
			while ((line = br.readLine())!=null && !stop) {
				sendEvent(line);
				try {
					if (this.getRate() == 1.0)
						Thread.sleep(sleep);
				} catch (Exception e) {

					e.printStackTrace();
					this.stop();
				}

			}
		} catch (Exception e) {
			logger.error("Unexpected thread termination");
			e.printStackTrace();
		} finally {
			logger.info("Stream Terminated: " + this.streamName);
			this.stop();
		}
	}
}
