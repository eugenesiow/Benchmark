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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.csvreader.CsvReader;
import com.espertech.esper.client.EPServiceProvider;

public class S2SPollutionStream extends S2SSensorStream implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	EPServiceProvider epService = null;
	Double distance;
	String SEPARATOR = ",";
	List<String> header = new ArrayList<String>();

	public S2SPollutionStream(EPServiceProvider epService, String streamName, String srcFile) {
		this.epService = epService;
		this.streamName = streamName;
		Map<String,Object> streamDefinition = new LinkedHashMap<String, Object>();
		streamDefinition.put("eventId", String.class);
		streamDefinition.put("ozone", Double.class);
		streamDefinition.put("particullate_matter", Double.class);
		streamDefinition.put("carbon_monoxide", Double.class);
		streamDefinition.put("sulfure_dioxide", Double.class);
		streamDefinition.put("nitrogen_dioxide", Double.class);
		streamDefinition.put("longitude", Double.class);
		streamDefinition.put("latitude", Double.class);
		streamDefinition.put("timestamp", String.class);
		streamDefinition.put("api", Double.class);
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
	
	private Map<String, Object> fillData(String line, String eventId) {
		Map<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("eventId", eventId);
		String[] parts = line.split(SEPARATOR);
		for(String headerEl:header) {
			data.put(headerEl, parts[header.indexOf(headerEl)]);
		}
		Double ozone = Double.parseDouble(parts[header.indexOf("ozone")]);
		Double particullate_matter = Double.parseDouble(parts[header.indexOf("particullate_matter")]);
		Double sulfure_dioxide = Double.parseDouble(parts[header.indexOf("sulfure_dioxide")]);
		Double nitrogen_dioxide = Double.parseDouble(parts[header.indexOf("nitrogen_dioxide")]);
		Double carbon_monoxide = Double.parseDouble(parts[header.indexOf("carbon_monoxide")]);
		Double api = ozone;
		if (particullate_matter > api)
			api = particullate_matter;
		if (carbon_monoxide > api)
			api = carbon_monoxide;
		if (sulfure_dioxide > api)
			api = sulfure_dioxide;
		if (nitrogen_dioxide > api)
			api = nitrogen_dioxide;
		data.put("api", api);
		return data;
	}
	
	@Override
	public void run() {
		logger.info("Starting sensor stream: " + this.streamName);
		try {
			String line = "";
			while ((line = br.readLine())!=null && !stop) {
				// logger.info("Reading: " + streamData.toString());
//				Date obTime = sdf.parse(streamData.get("TIMESTAMP").toString());
//				if (this.startDate != null && this.endDate != null) {
//					if (obTime.before(this.startDate) || obTime.after(this.endDate)) {
//						logger.debug(this.getURI() + ": Disgarded observation @" + obTime);
//						continue;
//					}
//				}
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
