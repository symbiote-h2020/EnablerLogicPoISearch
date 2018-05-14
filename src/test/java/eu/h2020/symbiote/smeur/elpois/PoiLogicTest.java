package eu.h2020.symbiote.smeur.elpois;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.enablerlogic.messaging.properties.EnablerLogicProperties;

import eu.h2020.symbiote.model.cim.ObservationValue;
import eu.h2020.symbiote.model.cim.Property;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.model.cim.UnitOfMeasurement;
import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapPlugin;
import eu.h2020.symbiote.smeur.messages.DomainSpecificInterfaceResponse;
import eu.h2020.symbiote.smeur.messages.PoIInformation;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValues;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValuesResponse;

public class PoiLogicTest {

	@Mock
	EnablerLogicProperties props;

	@Mock
	RapPlugin rapPlugin;

	@Mock
	EnablerLogic elMock;

	@InjectMocks
	@Resource
	private PoiLogic poi;

	@Before
	public void setUp() throws Exception {

		MockitoAnnotations.initMocks(this);
		when(props.getEnablerName()).thenReturn("Foo");
		when(elMock.sendSyncMessageToEnablerLogic(any(String.class), any(QueryPoiInterpolatedValues.class),
				any(Class.class))).thenReturn(new QueryPoiInterpolatedValuesResponse());
	}

	@Test
	public void testRegisterResources() {

		CloudResource cr = poi.createServiceResource("23");
		assertEquals("23", cr.getInternalId());
		assertEquals("Foo", cr.getPluginId());
		assertThat(cr.getResource(), instanceOf(Service.class));
	}

	@Test
	public void testParseXml() {

		assertNotNull(getFile("osmResponse.xml"));

		Map<String, WGS84Location> map = poi.parseOsmXml(getFile("osmResponse.xml"), "Hospital");
		assertEquals(3, map.size());

		for (Entry<String, WGS84Location> entry : map.entrySet()) {
			if (entry.getValue().getName().equals("Ambulanta Blatine")) {
				assertEquals(43.5063291, entry.getValue().getLatitude(), 0);
				assertEquals(16.4603236, entry.getValue().getLongitude(), 0);
			} else if (entry.getValue().getName().equals("Splitske toplice")) {
				assertEquals(43.5090209, entry.getValue().getLatitude(), 0);
				assertEquals(16.4370730, entry.getValue().getLongitude(), 0);
			} else if (entry.getValue().getName().equals("Salus ST")) {
				assertEquals(43.5065924, entry.getValue().getLatitude(), 0);
				assertEquals(16.4738638, entry.getValue().getLongitude(), 0);
			}
		}
	}

	@Test
	public void testFormatResponse() {
		QueryPoiInterpolatedValues qiv = new QueryPoiInterpolatedValues(
				poi.parseOsmXml(getFile("osmResponse.xml"), "Hospital"));
		QueryPoiInterpolatedValuesResponse interpolatorResponse = new QueryPoiInterpolatedValuesResponse();
		interpolatorResponse.theData = new HashMap<String, PoIInformation>();
		for (Entry<String, WGS84Location> entry : qiv.thePoints.entrySet()) {
			interpolatorResponse.theData.put(entry.getKey(), new PoIInformation());
		}
		List<DomainSpecificInterfaceResponse> dsiResponse = poi.formatResponse(qiv, interpolatorResponse);
		for (int i = 0; i < dsiResponse.size(); i++) {
			assertTrue(dsiResponse.get(i).getObservation().isEmpty());
		}

		Property dummyProperty = new Property("temp", "http://purl.oclc.org/NET/ssnx/qu/quantity#temperature",Arrays.asList("temp"));
		UnitOfMeasurement uom = new UnitOfMeasurement("C", "Celsius", "http://purl.oclc.org/NET/ssnx/qu/unit#degreeCelsius", Arrays.asList(""));
		ObservationValue dummyObservation = new ObservationValue("23", dummyProperty, uom);
		PoIInformation poiInfo = new PoIInformation();
		poiInfo.interpolatedValues = new HashMap<String, ObservationValue>();
		poiInfo.interpolatedValues.put("prop", dummyObservation);

		for (Entry<String, WGS84Location> entry : qiv.thePoints.entrySet()) {
			interpolatorResponse.theData.put(entry.getKey(), poiInfo);
		}

		dsiResponse = poi.formatResponse(qiv, interpolatorResponse);
		for (int i = 0; i < dsiResponse.size(); i++) {
			assertFalse(dsiResponse.get(i).getObservation().isEmpty());
			assertEquals(dsiResponse.get(i).toString(),
					"DomainSpecificInterfaceResponse [latitude=" + dsiResponse.get(i).getLatitude() + ", longitude="
							+ dsiResponse.get(i).getLongitude() + ", name=" + dsiResponse.get(i).getName()
							+ ", observation=" + dsiResponse.get(i).getObservation() + "]");
		}
	}

	protected String getFile(String fileName) {
		StringBuilder result = new StringBuilder("");

		// Get file from resources folder
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
	
	@Test
	public void testParseInitXml() {

		assertNotNull(getFile("osmResponse.xml"));

		List<DomainSpecificInterfaceResponse> dsiResponse = poi.parseInitXml(getFile("osmResponse.xml"));
		assertEquals(3, dsiResponse.size());
		
		assertEquals("hospital", dsiResponse.get(0).getAmenity());
		assertEquals("1349390241", dsiResponse.get(0).getId());
		assertEquals("43.5063291", dsiResponse.get(0).getLatitude());
		assertEquals("16.4603236", dsiResponse.get(0).getLongitude());
		assertEquals("Ambulanta Blatine", dsiResponse.get(0).getName());
	}

}
