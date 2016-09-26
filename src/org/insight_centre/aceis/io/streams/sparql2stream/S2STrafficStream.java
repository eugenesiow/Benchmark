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

public class S2STrafficStream extends S2SSensorStream implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	EPServiceProvider epService = null;
	Double distance;
	String SEPARATOR = ",";
	List<String> header = new ArrayList<String>();

	public S2STrafficStream(EPServiceProvider epService, String streamName, String reportId, String metadataPath, String srcFile) {
		this.epService = epService;
		this.streamName = streamName;
		Map<String,Object> streamDefinition = new LinkedHashMap<String, Object>();
		streamDefinition.put("eventId", String.class);
		streamDefinition.put("status", String.class);
		streamDefinition.put("avgMeasuredTime", Double.class);
		streamDefinition.put("averageSpeed", Double.class);
		streamDefinition.put("extID", Double.class);
		streamDefinition.put("medianMeasuredTime", Double.class);
		streamDefinition.put("TIMESTAMP", String.class);
		streamDefinition.put("vehicleCount", Double.class);
		streamDefinition.put("_id", Double.class);
		streamDefinition.put("REPORT_ID", Double.class);
		streamDefinition.put("congestionLevel", Double.class);
		saveMetaData(reportId,metadataPath);
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
	
	private void saveMetaData(String reportId,String metadataPath) {
		try {
			CsvReader metaData = new CsvReader(metadataPath);
			metaData.readHeaders();
			while (metaData.readRecord()) {
				if (reportId.equals(metaData.get("REPORT_ID"))) {
					distance = Double.parseDouble(metaData.get("DISTANCE_IN_METERS")); //read the distance of this particular station from metadata file
					metaData.close();
					break;
				}
			}
		} catch(IOException e) {
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
		data.put("congestionLevel", (Double.parseDouble(parts[header.indexOf("vehicleCount")]) / distance));
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
