package eu.h2020.symbiote.smeur.elpois;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;

import eu.h2020.symbiote.cloud.model.data.observation.Location;
import eu.h2020.symbiote.cloud.model.data.observation.ObservationValue;
import eu.h2020.symbiote.cloud.model.data.observation.Property;
import eu.h2020.symbiote.cloud.model.data.observation.UnitOfMeasurement;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.smeur.elpois.model.DomainSpecificInterfaceResponse;
import eu.h2020.symbiote.smeur.messages.PoIInformation;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValues;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValuesResponse;

public class PoiLogicTest {
	

	PoiLogic poi;
	EnablerLogic elMock;
	
	@Before
	public void setUp() throws Exception {
		poi=new PoiLogic();
		elMock=mock(EnablerLogic.class);
	}
	
	@Test
	public void testParseXml(){
		assertNotNull(getFile("osmResponse.xml"));
		Map<String,Location> map = poi.parseOsmXml(getFile("osmResponse.xml"), "Hospital");
		assertEquals(3, map.size());

		for (Map.Entry<String, Location> entry : map.entrySet()) {
			if(entry.getValue().getId().equals("Ambulanta Blatine")){
				assertEquals(43.5063291, entry.getValue().getLatitude(), 0);
				assertEquals(16.4603236, entry.getValue().getLongitude(), 0);
			}
			else if(entry.getValue().getId().equals("Splitske toplice")){
				assertEquals(43.5090209, entry.getValue().getLatitude(), 0);
				assertEquals(16.4370730, entry.getValue().getLongitude(), 0);
			}
			else if(entry.getValue().getId().equals("Salus ST")){
				assertEquals(43.5065924, entry.getValue().getLatitude(), 0);
				assertEquals(16.4738638, entry.getValue().getLongitude(), 0);
			}
		}
	}
	
	@Test
	public void testFormatResponse(){
		QueryPoiInterpolatedValues qiv = new QueryPoiInterpolatedValues(poi.parseOsmXml(getFile("osmResponse.xml"), "Hospital"));
		QueryPoiInterpolatedValuesResponse interpolatorResponse = new QueryPoiInterpolatedValuesResponse();
		interpolatorResponse.theData = new HashMap<String,PoIInformation>();
		for (Map.Entry<String, Location> entry : qiv.thePoints.entrySet()) {
			interpolatorResponse.theData.put(entry.getKey(), new PoIInformation());
		}
		List<DomainSpecificInterfaceResponse> dsiResponse = poi.formatResponse(qiv, interpolatorResponse);
		for (int i = 0; i < dsiResponse.size(); i++) {
			assertTrue(dsiResponse.get(i).getObservation().isEmpty());
		}
		
		Property dummyProperty = new Property("temp","temp");
		UnitOfMeasurement uom = new UnitOfMeasurement("C", "Celsius", "");
		ObservationValue dummyObservation = new ObservationValue("23", dummyProperty, uom);
		PoIInformation poiInfo = new PoIInformation();
		poiInfo.interpolatedValues = new HashMap<String,ObservationValue>();
		poiInfo.interpolatedValues.put("prop", dummyObservation);
		
		for (Map.Entry<String, Location> entry : qiv.thePoints.entrySet()) {
			interpolatorResponse.theData.put(entry.getKey(), poiInfo);
		}
		
		dsiResponse = poi.formatResponse(qiv, interpolatorResponse);
		for (int i = 0; i < dsiResponse.size(); i++) {
			assertFalse(dsiResponse.get(i).getObservation().isEmpty());
		}
	}
	
	private String getFile(String fileName) {
		StringBuilder result = new StringBuilder("");

		//Get file from resources folder
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(fileName).getFile());

		try (Scanner scanner = new Scanner(file)) {

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				result.append(line).append("\n");
			}

			scanner.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return result.toString();
	  }

}
