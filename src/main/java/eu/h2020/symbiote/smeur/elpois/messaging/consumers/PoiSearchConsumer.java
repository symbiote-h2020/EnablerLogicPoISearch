package eu.h2020.symbiote.smeur.elpois.messaging.consumers;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import eu.h2020.symbiote.smeur.elpois.messaging.RabbitManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Consumer of the Poi Search Message.
 * Created by Petar Krivic on 13/07/2017.
 */
public class PoiSearchConsumer extends DefaultConsumer {
	
	private static String overpassURL = "www.overpass-api.de/api/xapi?node";
	private static String nominatimURL = "nominatim.openstreetmap.org/?format=json&";

    private static Log log = LogFactory.getLog(PoiSearchConsumer.class);
    RabbitManager rabbitManager;
    
    /**
     * Constructs a new instance and records its association to the passed-in channel.
     * Managers beans passed as parameters because of lack of possibility to inject it to consumer.
     * @param channel the channel to which this consumer is attached
     *
     */
    public PoiSearchConsumer(Channel channel, RabbitManager rabbitManager) {
        super(channel);
        this.rabbitManager = rabbitManager;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope,
    		AMQP.BasicProperties properties, byte[] body) throws IOException {
    	
        String msg = new String(body, "UTF-8");
        log.info( "Consume PoiSearch message: " + msg );

        //Consume message
        //from received message symbolicLocation is sent to nominatim-api to get coordinates
        //dummy data for testing
		try {
			String location = sendGetHttpRequest("http://"+nominatimURL+"city=split");
			String jsonLocation = (String) location.subSequence(1, location.length()-1);
			log.info("Response from nominatim-api received: " + jsonLocation );
			
			//from http-response bounding-box coordinates are parsed and sent to overpass-api
			//overpass-api returns requested PoIs
			JSONObject jsonResponse = new JSONObject( jsonLocation );
			final String geodata = (String) jsonResponse.get("boundingbox").toString();
			
			String [] geo = geodata.substring(1,geodata.length()-2).replaceAll("\"", "").split(",");
			
			log.info("Sending request: "+"http://"+overpassURL+"[amenity=hospital][bbox="+geo[2]+","+geo[0]+","+geo[3]+","+geo[1] +"]");
			String poiResponse = sendGetHttpRequest("http://"+overpassURL+"[amenity=hospital][bbox="+geo[2]+","+geo[0]+","+geo[3]+","+geo[1] +"]");
			log.info("Response from overpass-api received: " + poiResponse);
			
			//parse received XML with PoIs
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse( new InputSource( new StringReader( poiResponse ) ) );
			
			NodeList nl = document.getElementsByTagName("node");
			
			for (int i = 0; i < nl.getLength(); i++) {
		        Node currentNode = nl.item(i);
		        
		        log.info("lat: "+currentNode.getAttributes().getNamedItem("lat"));
		        log.info("lat: "+currentNode.getAttributes().getNamedItem("lon"));
		        NodeList children = currentNode.getChildNodes();

		        for (int j = 0; j < children.getLength(); j++) {
		        	Node currentChild = children.item(j);
		        	if(currentChild.getNodeName().equals("tag") && currentChild.getAttributes().getNamedItem("k").toString().contains("name")){
		        		log.info(currentChild.getAttributes().getNamedItem("v"));
		        	}
		        }
			}		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        //In case answer is needed
        /**
        try {
            ObjectMapper mapper = new ObjectMapper();
            //TODO read proper value and handle received data message
            String dataAppearedMessage = mapper.readValue(msg, String.class);

            log.debug( "Sending response to the sender....");

            byte[] responseBytes = mapper.writeValueAsBytes("Response");

            AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(properties.getCorrelationId())
                    .build();
            this.getChannel().basicPublish("", properties.getReplyTo(), replyProps, responseBytes);
            log.debug("-> Message sent back");

            this.getChannel().basicAck(envelope.getDeliveryTag(), false);

        } catch( JsonParseException | JsonMappingException e ) {
            log.error("Error occurred when parsing Resource object JSON: " + msg, e);
        } catch( IOException e ) {
            log.error("I/O Exception occurred when parsing Resource object" , e);
        }
        **/
    }
    
    private static String sendGetHttpRequest(String address) throws Exception{
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
