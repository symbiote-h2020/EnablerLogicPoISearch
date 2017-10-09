package eu.h2020.symbiote.smeur.elpois;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import eu.h2020.symbiote.enablerlogic.rap.plugin.RapPlugin;
import eu.h2020.symbiote.enablerlogic.rap.plugin.WritingToResourceListener;
import eu.h2020.symbiote.smeur.elpois.messaging.HttpCommunication;
import eu.h2020.symbiote.smeur.elpois.model.DomainSpecificInterfaceResponse;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValues;
import eu.h2020.symbiote.smeur.messages.QueryPoiInterpolatedValuesResponse;
import eu.h2020.symbiote.cloud.model.data.Result;
import eu.h2020.symbiote.cloud.model.data.parameter.InputParameter;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.core.model.resources.Parameter;
import eu.h2020.symbiote.core.model.resources.Service;
import eu.h2020.symbiote.cloud.model.data.observation.Location;
import eu.h2020.symbiote.cloud.model.data.observation.ObservationValue;
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

	private void registerResources() {
		List<CloudResource> cloudResources = new LinkedList<>();
		cloudResources.add(createServiceResource("23"));

		// Check if working ?
		log.info("starting the registration of resources...");
		// rhClientService.registerResources(cloudResources);
	}

	// set IPs
	private CloudResource createServiceResource(String internalId) {
		CloudResource cloudResource = new CloudResource();
		cloudResource.setInternalId(internalId);
		cloudResource.setPluginId(props.getEnablerName());
		cloudResource.setCloudMonitoringHost("cloudMonitoringHostIP");

		Service service = new Service();
		cloudResource.setResource(service);
		service.setInterworkingServiceURL("https://symbiote-h2020.eu/example/interworkingService/");

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

		service.setParameters(Arrays.asList(parameter1, parameter2, parameter3, parameter4));
		return cloudResource;
	}

	private void registerRapConsumers() {
		rapPlugin.registerWritingToResourceListener(new WritingToResourceListener() {

			String overpassURL = "www.overpass-api.de/api/xapi?node";
			double lat, lon, r;
			String amenity;
			ObjectMapper om = new ObjectMapper();

			@Override
			public Result<Object> writeResource(String resourceId, List<InputParameter> parameters) {
				log.info("writing to resource {} body:{}", resourceId, parameters);

				for (InputParameter ip : parameters) {
					// parse inputParameters
					if (ip.getName().equals("latitude"))
						lat = Double.parseDouble(ip.getValue());
					else if (ip.getName().equals("longitude"))
						lon = Double.parseDouble(ip.getValue());
					else if (ip.getName().equals("radius"))
						r = Double.parseDouble(ip.getValue());
					else if (ip.getName().equals("amenity"))
						amenity = ip.getValue();
				}

				log.info("received parameters are: latitude=" + String.valueOf(lat) + ", longitude="
						+ String.valueOf(lon) + ", radius=" + String.valueOf(r) + ", amenity=" + amenity);
				// calculate bounds of bounding box
				double northBound = lat + ((1 / 111.0) * r);
				double southBound = lat - ((1 / 111.0) * r);
				double eastBound = lon + ((1 / (111.0 * (Math.cos(Math.toRadians(lat))))) * r);
				double westBound = lon - ((1 / (111.0 * Math.cos(Math.toRadians(lat)))) * r);
				log.info("boundingbox: N=" + String.valueOf(northBound) + ";S=" + String.valueOf(southBound) + ";E="
						+ String.valueOf(eastBound) + ";W=" + String.valueOf(westBound));

				try {
					// contact OSM-api to fetch queried PoIs
					String osmResponse = HttpCommunication
							.sendGetHttpRequest("http://" + overpassURL + "[amenity=" + amenity + "][bbox=" + westBound
									+ "," + southBound + "," + eastBound + "," + northBound + "]");
					log.info("Response from overpass-api received: " + osmResponse);
					QueryPoiInterpolatedValues qiv = new QueryPoiInterpolatedValues(parseOsmXml(osmResponse, amenity));
					// contact interpolator to fetch interpolated data
					// INTEGRATION TEST WITH INTERPOLATOR
					QueryPoiInterpolatedValuesResponse response = enablerLogic.sendSyncMessageToEnablerLogic(
							"EnablerLogicInterpolator", qiv, QueryPoiInterpolatedValuesResponse.class);
					log.info("RPC communication with Interpolator successful!");
					log.info("Received response :" + response.toString());
					return new Result<>(false, null, om.writeValueAsString(formatResponse(qiv, response)));

				} catch (Exception e) {
					log.info("HTTP communication with OSM overpass-api failed!");
					e.printStackTrace();
					return null;
				}
			}
		});
	}

	private Map<String, Location> parseOsmXml(String inputXml, String amenity) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			Map<String, Location> interpolatorQueryMap = new HashMap<String, Location>();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new InputSource(new StringReader(inputXml)));

			NodeList nl = document.getElementsByTagName("node");
			for (int i = 0; i < nl.getLength(); i++) {

				Location l = new Location();
				// log.info("->lat:
				// "+nl.item(i).getAttributes().getNamedItem("lat").getNodeValue());
				// log.info("->lat:
				// "+nl.item(i).getAttributes().getNamedItem("lon").getNodeValue());
				l.setLatitude(Double.parseDouble(nl.item(i).getAttributes().getNamedItem("lat").getNodeValue()));
				l.setLongitude(Double.parseDouble(nl.item(i).getAttributes().getNamedItem("lon").getNodeValue()));
				l.setDescription(amenity);

				NodeList children = nl.item(i).getChildNodes();
				for (int j = 0; j < children.getLength(); j++) {

					if (children.item(j).getNodeName().equals("tag")
							&& children.item(j).getAttributes().getNamedItem("k").toString().contains("name")) {
						// log.info("->"+children.item(j).getAttributes().getNamedItem("v"));
						l.setId(children.item(j).getAttributes().getNamedItem("v").getNodeValue());
					}
				}
				// log.info(l.getId() + " : " + l.getLatitude() + " : " +
				// l.getLongitude());
				interpolatorQueryMap.put(UUID.randomUUID().toString(), l);
			}
			log.info("XML document parsed, and map for interpolator ready!");
			return interpolatorQueryMap;
		} catch (SAXException | IOException | ParserConfigurationException e) {
			log.info("Exception while parsing OpenStreetMap (overpass-api) XML response!");
			e.printStackTrace();
			return null;
		}
	}

	private List<DomainSpecificInterfaceResponse> formatResponse(QueryPoiInterpolatedValues interpolatorQuery,
			QueryPoiInterpolatedValuesResponse interpolatorResponse) {

		List<DomainSpecificInterfaceResponse> formatedResponse = new LinkedList<DomainSpecificInterfaceResponse>();

		for (Map.Entry<String, Location> entry : interpolatorQuery.thePoints.entrySet()) {
			DomainSpecificInterfaceResponse place = new DomainSpecificInterfaceResponse();
			place.setName(entry.getValue().getName());
			place.setLatitude(String.valueOf(entry.getValue().getLatitude()));
			place.setLongitude(String.valueOf(entry.getValue().getLongitude()));
			List<ObservationValue> observations = new LinkedList<ObservationValue>();
			try{
			for (Map.Entry<String, ObservationValue> e : interpolatorResponse.theData
					.get(entry.getKey()).interpolatedValues.entrySet()) {
				observations.add(e.getValue());
			}
			} catch (NullPointerException e){
				log.info("Error occurred! Interpolator doesnt have any data for requested POIs.");
			}
			place.setObservation(observations);
			formatedResponse.add(place);
		}
		return formatedResponse;
	}

	@Override
	public void notEnoughResources(NotEnoughResourcesAvailable arg0) {
		// TODO Auto-generated method stub
	}

}
