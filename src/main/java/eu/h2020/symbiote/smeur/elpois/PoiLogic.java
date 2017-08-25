package eu.h2020.symbiote.smeur.elpois;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.h2020.symbiote.EnablerLogic;
import eu.h2020.symbiote.ProcessingLogic;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;

@Component
public class PoiLogic implements ProcessingLogic{
	private static final Logger log = LoggerFactory.getLogger(PoiLogic.class);

	private EnablerLogic enablerLogic;
	
	@Override
	public void init(EnablerLogic enablerLogic) {
		this.enablerLogic = enablerLogic;
		
	}

	@Override
	public void measurementReceived(EnablerLogicDataAppearedMessage arg0) {		
	}

}
