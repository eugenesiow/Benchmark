package org.insight_centre.aceis.io.streams.sparql2stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.espertech.esper.client.EPServiceProvider;

public class S2SSensorStream implements Runnable {
	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected String streamName;
	protected BufferedReader br = null; 
	protected double rate = 1.0;
	// private int sleep = 1000;
	protected int sleep = 1000;
	protected boolean stop = false;
	protected List<String> requestedProperties = new ArrayList<String>();

	public List<String> getRequestedProperties() {
		return requestedProperties;
	}

	public void setRequestedProperties(List<String> requestedProperties) {
		this.requestedProperties = requestedProperties;
	}

	public void setRate(Double rate) {
		this.rate = rate;
		if (this.rate != 1.0)
			logger.info("Streaming acceleration rate set to: " + rate);
	}

	public double getRate() {
		return rate;
	}

	public void setFreq(Double freq) {
		sleep = (int) (sleep / freq);
		if (this.rate == 1.0)
			logger.info("Streaming interval set to: " + sleep + " ms");
	}

	@Override
	public void run() {

	}
	
	public void stop() {
		if (!stop) {
			stop = true;
		}
		try {
			logger.info("Stopping stream: " + this.streamName);
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
