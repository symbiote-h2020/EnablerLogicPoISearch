package eu.h2020.symbiote.smeur.elpois.messaging;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

public class HttpCommunication {
	
	/**
	 * Sends a HTTP-GET request to the given URI and returns response body as a String.
	 * @param address
	 * @return
	 * @throws Exception
	 */
	public static String sendGetHttpRequest(String address) throws Exception{
    	SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        
        URI uri = new URI(address);
        HttpMethod method = HttpMethod.GET;
        ClientHttpRequest request = factory.createRequest(uri, method);
        ClientHttpResponse response = request.execute();
        
        BufferedReader rdr = new BufferedReader(new InputStreamReader(response.getBody()));
        
        StringBuilder builder = new StringBuilder();
        String responseString = "";

        while ((responseString = rdr.readLine()) != null) {
            builder.append(responseString);
        }

        String result = builder.toString();
        return result;
    }
}
