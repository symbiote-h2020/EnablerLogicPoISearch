package eu.h2020.symbiote.smeur.elpois;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.enablerlogic.ProcessingLogic;
import eu.h2020.symbiote.smeur.elpois.messaging.HttpCommunication;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;

@Component
public class PoiLogic implements ProcessingLogic{
	private static final Logger log = LoggerFactory.getLogger(PoiLogic.class);

	private EnablerLogic enablerLogic;
	
	@Override
	public void initialization(EnablerLogic enablerLogic) {
		this.enablerLogic = enablerLogic;
		//register sync consumer for communication with DSI
		
		//MIGRATE TO CONSUMER OF DSI/RAP MESSAGE
		String overpassURL = "www.overpass-api.de/api/xapi?node";
		
		double lat = 43.51432;
		double lon = 16.45872;
		double r = 5.0;
		
		double northBound = lat + ((1/111.0)*r);
		double southBound = lat - ((1/111.0)*r);
		
		double eastBound = lon + ((1/(111.0*(Math.cos(Math.toRadians(lat)))))*r);
		double westBound = lon - ((1/(111.0*Math.cos(Math.toRadians(lat))))*r);
		
		String poiResponse;
		try {
			poiResponse = HttpCommunication.sendGetHttpRequest("http://"+overpassURL+"[amenity=hospital][bbox="+westBound+","+southBound+","+eastBound+","+northBound +"]");
		
		log.info("Response from overpass-api received: " + poiResponse);
		
		//parse received XML with PoIs
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse( new InputSource( new StringReader( poiResponse ) ) );
		
		NodeList nl = document.getElementsByTagName("node");
		
		for (int i = 0; i < nl.getLength(); i++) {
	        
	        log.info("lat: "+nl.item(i).getAttributes().getNamedItem("lat"));
	        log.info("lat: "+nl.item(i).getAttributes().getNamedItem("lon"));
	        NodeList children = nl.item(i).getChildNodes();

	        for (int j = 0; j < children.getLength(); j++) {
	        	
	        	if(children.item(j).getNodeName().equals("tag") && children.item(j).getAttributes().getNamedItem("k").toString().contains("name")){
	        		log.info("->"+children.item(j).getAttributes().getNamedItem("v"));
	        	}
	        }
		}	
		} catch (Exception e) {
			e.printStackTrace();
		}
		////////////////////////////////////////
	}
	
	@Override
	public void measurementReceived(EnablerLogicDataAppearedMessage arg0) {
		// TODO Auto-generated method stub
		
	}

}
