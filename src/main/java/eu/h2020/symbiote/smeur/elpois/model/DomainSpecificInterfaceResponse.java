package eu.h2020.symbiote.smeur.elpois.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.h2020.symbiote.cloud.model.data.observation.ObservationValue;

public class DomainSpecificInterfaceResponse {
	
	@JsonProperty
	private String latitude;
	
	@JsonProperty
	private String longitude;
	
	@JsonProperty
	private String name;
	
	@JsonProperty
	private List<ObservationValue> observation;
	
	public DomainSpecificInterfaceResponse(){
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<ObservationValue> getObservation() {
		return observation;
	}

	public void setObservation(List<ObservationValue> observation) {
		this.observation = observation;
	}
	
	
	
	

}