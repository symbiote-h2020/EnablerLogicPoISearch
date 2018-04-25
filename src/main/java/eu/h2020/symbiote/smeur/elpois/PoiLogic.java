package eu.h2020.symbiote.smeur.elpois;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.enablerlogic.ProcessingLogic;
import eu.h2020.symbiote.enablerlogic.messaging.RegistrationHandlerClientService;
import eu.h2020.symbiote.enablerlogic.messaging.properties.EnablerLogicProperties;
import eu.h2020.symbiote.model.cim.ObservationValue;
import eu.h2020.symbiote.model.cim.Parameter;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.rapplugin.messaging.rap.InvokingServiceListener;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapPlugin;
import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.smeur.messages.DomainSpecificInterfaceResponse;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValues;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValuesResponse;
import eu.h2020.symbiote.cloud.model.data.Result;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.NotEnoughResourcesAvailable;
import eu.h2020.symbiote.enabler.messaging.model.ResourcesUpdated;

@Component
public class PoiLogic implements ProcessingLogic {
	private static final Logger log = LoggerFactory.getLogger(PoiLogic.class);

	private EnablerLogic enablerLogic;

	@Autowired
	private EnablerLogicProperties props;

	@Autowired
	private RapPlugin rapPlugin;

	@Autowired
	private RegistrationHandlerClientService rhClientService;

	@Value("${symbIoTe.interworking.interface.url}")
	private String interworkingInterfaceUrl;

	@Override
	public void initialization(EnablerLogic enablerLogic) {
		this.enablerLogic = enablerLogic;

		registerResources();
		registerRapConsumers();
	}

	@Override
	public void measurementReceived(EnablerLogicDataAppearedMessage arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void resourcesUpdated(ResourcesUpdated arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void notEnoughResources(NotEnoughResourcesAvailable arg0) {
		// TODO Auto-generated method stub
	}

	private void registerResources() {
		List<CloudResource> cloudResources = new LinkedList<>();
		cloudResources.add(createServiceResource("23"));

		log.info("starting the registration of resources...");

		try {
			log.info("REGISTER RESOURCES:" + rhClientService.registerResources(cloudResources).toString());
		} catch (Throwable t) {
			log.error("ERROR in registering resources.", t);
		}
		
		log.info("Point of Interest service ready!");
	}

	// set IPs
	protected CloudResource createServiceResource(String internalId) {
		CloudResource cloudResource = new CloudResource();
		cloudResource.setInternalId(internalId);
		cloudResource.setPluginId(props.getEnablerName());
		// cloudResource.setCloudMonitoringHost("cloudMonitoringHostIP");

		Service service = new Service();
		cloudResource.setResource(service);
		service.setInterworkingServiceURL(interworkingInterfaceUrl + "/");

		service.setDescription(new LinkedList<>());
		service.setName("PointOfInterestSearch");

		Parameter parameter1 = new Parameter();
		parameter1.setMandatory(true);
		parameter1.setName("latitude");

		Parameter parameter2 = new Parameter();
		parameter2.setMandatory(true);
		parameter2.setName("longitude");

		Parameter parameter3 = new Parameter();
		parameter3.setMandatory(true);
		parameter3.setName("radius");

		Parameter parameter4 = new Parameter();
		parameter4.setMandatory(true);
		parameter4.setName("amenity");
		
		service.setDescription(Arrays.asList("poi service"));
		log.info("added service description and parameters..");
		try {
			cloudResource.setAccessPolicy(
					new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, new HashMap<>()));
		} catch (InvalidArgumentsException e) {
			log.error("Security Token Access Policy Error", e);
		}

		service.setParameters(Arrays.asList(parameter1, parameter2, parameter3, parameter4));
		log.info("Service parameters set.");
		return cloudResource;
	}

	/**
	 * Registration of poiConsumer to EnablerLogic, Logic of PoISearch.
	 */
	protected void registerRapConsumers() {
		rapPlugin.registerInvokingServiceListener(new InvokingServiceListener() {

			String overpassURL = "www.overpass-api.de/api/xapi?node";
			double lat, lon, r;
			String amenity;
			ObjectMapper om = new ObjectMapper();

			@Override
			public Object invokeService(String resourceId, Map<String, eu.h2020.symbiote.rapplugin.domain.Parameter> parameters) {
				log.info("RAP consumer received message with resourceId {} and parameters in body:{}", resourceId, parameters);
				
				for(eu.h2020.symbiote.rapplugin.domain.Parameter ip : parameters.values()){
					if (ip.getName().equals("latitude"))
						lat = (double) ip.getValue();
					else if (ip.getName().equals("longitude"))
						lon = (double) ip.getValue();
					else if (ip.getName().equals("radius"))
						r = (double) ip.getValue();
					else if (ip.getName().equals("amenity"))
						amenity = (String) ip.getValue();
				}

				log.info("Received parameters are: latitude=" + String.valueOf(lat) + ", longitude="
						+ String.valueOf(lon) + ", radius=" + String.valueOf(r) + ", amenity=" + amenity);
				// calculate bounds of bounding box
				double northBound = lat + ((1 / 111.0) * r);
				double southBound = lat - ((1 / 111.0) * r);
				double eastBound = lon + ((1 / (111.0 * (Math.cos(Math.toRadians(lat))))) * r);
				double westBound = lon - ((1 / (111.0 * Math.cos(Math.toRadians(lat)))) * r);
				log.info("Calculated boundingbox: N=" + String.valueOf(northBound) + ";S=" + String.valueOf(southBound) + ";E="
						+ String.valueOf(eastBound) + ";W=" + String.valueOf(westBound));

				try {
					// contact OSM-api to fetch queried PoIs
					log.info("Sending HTTP request to OSM...");
					String osmResponse = sendGetHttpRequest("http://" + overpassURL + "[amenity=" + amenity + "][bbox="
							+ westBound + "," + southBound + "," + eastBound + "," + northBound + "]");
					log.info("Response from overpass-api received: " + osmResponse);

					QueryPoiInterpolatedValues qiv = new QueryPoiInterpolatedValues(parseOsmXml(osmResponse, amenity));

					// contact interpolator to fetch interpolated data
					QueryPoiInterpolatedValuesResponse response = enablerLogic.sendSyncMessageToEnablerLogic(
							"EnablerLogicInterpolator", qiv, QueryPoiInterpolatedValuesResponse.class);
					log.info("RPC communication with Interpolator successful! Received response: "
							+ response.toString());
					//log.info("Sending response message : " + om.writeValueAsString(formatResponse(qiv, response)));
					return new Result<>(false, null, om.writeValueAsString(formatResponse(qiv, response)));

				} catch (Exception e) {
					log.info("HTTP communication with OSM overpass-api failed!");
					e.printStackTrace();
					return null;
				}
			}
		});
	}

	/**
	 * Parsing of XML received from osm-API
	 * 
	 * @param inputXml
	 * @param amenity
	 * @return map of locations where key is locations unique Id.
	 */
	public Map<String, WGS84Location> parseOsmXml(String inputXml, String amenity) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			Map<String, WGS84Location> interpolatorQueryMap = new HashMap<String, WGS84Location>();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new InputSource(new StringReader(inputXml)));

			NodeList nl = document.getElementsByTagName("node");
			for (int i = 0; i < nl.getLength(); i++) {

				WGS84Location l = new WGS84Location(
						Double.parseDouble(nl.item(i).getAttributes().getNamedItem("lon").getNodeValue()),
						Double.parseDouble(nl.item(i).getAttributes().getNamedItem("lat").getNodeValue()), 0, null,
						Arrays.asList(amenity));

				NodeList children = nl.item(i).getChildNodes();
				for (int j = 0; j < children.getLength(); j++) {

					// locations id is a name of found amenity
					if (children.item(j).getNodeName().equals("tag")
							&& children.item(j).getAttributes().getNamedItem("k").toString().contains("name")) {
						l.setName(children.item(j).getAttributes().getNamedItem("v").getNodeValue());
					}
				}
				interpolatorQueryMap.put(nl.item(i).getAttributes().getNamedItem("id").getNodeValue(), l);
			}
			return interpolatorQueryMap;
		} catch (SAXException | IOException | ParserConfigurationException e) {
			log.info("Exception while parsing OpenStreetMap (overpass-api) XML response!");
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Formatting received interpolator response to a list of DSI responses.
	 * 
	 * @param interpolatorQuery
	 * @param interpolatorResponse
	 * @return
	 */
	public List<DomainSpecificInterfaceResponse> formatResponse(QueryPoiInterpolatedValues interpolatorQuery,
			QueryPoiInterpolatedValuesResponse interpolatorResponse) {

		List<DomainSpecificInterfaceResponse> formatedResponse = new LinkedList<DomainSpecificInterfaceResponse>();

		for (Entry<String, WGS84Location> entry : interpolatorQuery.thePoints.entrySet()) {
			DomainSpecificInterfaceResponse place = new DomainSpecificInterfaceResponse();
			place.setId(entry.getKey());
			place.setName(entry.getValue().getName());
			place.setLatitude(String.valueOf(entry.getValue().getLatitude()));
			place.setLongitude(String.valueOf(entry.getValue().getLongitude()));
			List<ObservationValue> observations = new LinkedList<ObservationValue>();

			try {
				for (Map.Entry<String, ObservationValue> e : interpolatorResponse.theData
						.get(entry.getKey()).interpolatedValues.entrySet()) {
					observations.add(e.getValue());
				}
			} catch (NullPointerException e) {
				log.info("Error occurred! Interpolator doesn't have any data for requested POIs.");
			}

			place.setObservation(observations);
			formatedResponse.add(place);
		}
		return formatedResponse;
	}

	/**
	 * HTTP-GET request to a specified URL
	 * 
	 * @param address
	 * @return
	 * @throws Exception
	 */
	public static String sendGetHttpRequest(String address) throws Exception {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(5000);
		factory.setReadTimeout(10000);
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
