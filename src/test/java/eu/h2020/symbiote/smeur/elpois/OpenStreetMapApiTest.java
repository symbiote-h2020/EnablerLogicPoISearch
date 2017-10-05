package eu.h2020.symbiote.smeur.elpois;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import eu.h2020.symbiote.smeur.elpois.messaging.HttpCommunication;

public class OpenStreetMapApiTest {
	
	String overpassURL1 = "http://overpass-api.de/api/xapi";
	String overpassURL2 = "http://api.openstreetmap.fr/oapi/xapi";
	String overpassURL3 = "http://overpass.osm.rambler.ru/cgi/xapi";
	String dummyRequestParameters = "?node[amenity=hospital][bbox=16.41223,43.49439,16.50218,43.53498]";
	
	@Test
	public void TestReceivingResponseURL1(){
		try {
			String instance1 = HttpCommunication.sendGetHttpRequest(overpassURL1+dummyRequestParameters);
			assertNotNull(instance1);
		} catch (Exception e) {
			System.out.println("Exception thrown on URL1 testing!");
		}
	}
	
	@Test
	public void TestReceivingResponseURL2(){
		try {
			String instance2 = HttpCommunication.sendGetHttpRequest(overpassURL2+dummyRequestParameters);
			assertNotNull(instance2);
		} catch (Exception e) {
			System.out.println("Exception thrown on URL2 testing!");
		}
	}
	
	@Test
	public void TestReceivingResponseURL3(){
		try {
			String instance3 = HttpCommunication.sendGetHttpRequest(overpassURL3+dummyRequestParameters);
			assertNotNull(instance3);
		} catch (Exception e) {
			System.out.println("Exception thrown on URL3 testing!");
		}
	}
	
	@Test
	public void testResultParsingLatitudeLongitude(){
		String response = null;
		
		try {
			response = HttpCommunication.sendGetHttpRequest(overpassURL1+dummyRequestParameters);
		} catch (Exception e) {
			System.out.println("Response not received while testing parsing of result!");
			e.printStackTrace();
		}
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			Document document = builder.parse( new InputSource( new StringReader(response) ) );
			NodeList nl = document.getElementsByTagName("node");
			
			for (int i = 0; i < nl.getLength(); i++) {
				assertNotNull(nl.item(i).getAttributes().getNamedItem("lat"));
				assertNotNull(nl.item(i).getAttributes().getNamedItem("lon"));			
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}
	
//	@Test
//	public void testResultParsingName(){
//		String response = null;
//		
//		try {
//			response = HttpCommunication.sendGetHttpRequest(overpassURL1+dummyRequestParameters);
//		} catch (Exception e) {
//			System.out.println("Response not received while testing parsing of result!");
//			e.printStackTrace();
//		}
//		
//		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//		DocumentBuilder builder;
//		try {
//			builder = factory.newDocumentBuilder();
//			Document document = builder.parse( new InputSource( new StringReader(response) ) );
//			NodeList nl = document.getElementsByTagName("node");
//			NodeList children = nl.item(1).getChildNodes();
//			boolean nameExists = false;
//			for (int j = 0; j < children.getLength(); j++) {
//	        	
//	        	if(children.item(j).getNodeName().equals("tag") && children.item(j).getAttributes().getNamedItem("k").toString().contains("name")){
//	        		nameExists = true;
//	        	}
//	        }
//			assertTrue(nameExists);
//		} catch (ParserConfigurationException | SAXException | IOException e) {
//			e.printStackTrace();
//		}
//	}

}
