package eu.h2020.symbiote.smeur.elpois;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;

import eu.h2020.symbiote.cloud.model.data.observation.Location;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;

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
		Map<String,Location> map = poi.parseOsmXml(getFile("osmResponse.xml"), "amenity");
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
