package eu.h2020.symbiote.smeur.elpois;

import static org.junit.Assert.*;

import org.junit.Test;

public class HttpCommunicationTest {

	@Test
	public void httpCommunicationTest() throws Exception {

		String result1 = PoiLogic.sendGetHttpRequest("https://github.com/symbiote-h2020");
		System.out.println(result1);
		assertNotNull(result1);
	}
}
