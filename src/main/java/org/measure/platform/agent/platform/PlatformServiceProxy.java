/*******************************************************************************
 * Copyright (C) 2019 Softeam
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.measure.platform.agent.platform;

import java.util.List;

import org.measure.smm.log.MeasureLog;
import org.measure.smm.measure.model.SMMMeasure;
import org.measure.smm.remote.RemoteMeasureInstanceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class PlatformServiceProxy {
		
	private final Logger log = LoggerFactory.getLogger(PlatformServiceProxy.class);	
	private static final String REGISTRATION_API = "/api/remote-measure/registration";
	private static final String EXECUTION_API = "/api/remote-measure/measure-execution";
	private static final String EXECUTIONLIST_API = "/api/remote-measure/execution-list";



	private String login;
	private String password;
	private String platformURL;

	public PlatformServiceProxy(String platformURL,String login,String password) {
		this.login = login;
		this.password = password;
		this.platformURL = platformURL;
	}

	
	public void registerMeasures(List<SMMMeasure> measures, String agent){		
		String cookie = authenticate();
		
		for (SMMMeasure measure : measures) {
			measure.setName(measure.getName() + " (" + agent + ")");
			measure.setAgentId(agent);
				
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			httpHeaders.add("Cookie", cookie);
			HttpEntity<?> httpEntity = new HttpEntity<Object>(measure, httpHeaders);
			try {
				 restTemplate.exchange(platformURL + REGISTRATION_API, HttpMethod.PUT, httpEntity,Object.class);
					log.error("Measure Registred : " + measure.getName());
			} catch (Exception e) {
				e.printStackTrace();
				log.error("Registration Error : " + e.getMessage());
			}	
		}
	}
	
	public void sendMeasureExecutionResult(MeasureLog executionLog){			
		String cookie = authenticate();
		
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		httpHeaders.add("Cookie", cookie);
		HttpEntity<?> httpEntity = new HttpEntity<Object>(executionLog, httpHeaders);
		try {
			 restTemplate.exchange(platformURL + EXECUTION_API, HttpMethod.PUT, httpEntity,Object.class);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Execution Error : " + e.getMessage());
		}	
	}
	
	
	public RemoteMeasureInstanceList getMeasureInstanceList(String agentName) {
		String cookie = authenticate();
		
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		httpHeaders.add("Cookie", cookie);
		
		
		HttpEntity<?> httpEntity = new HttpEntity<Object>(null, httpHeaders);
		try {
			return restTemplate.exchange(platformURL + EXECUTIONLIST_API +"?id=" + agentName, HttpMethod.POST, httpEntity,RemoteMeasureInstanceList.class).getBody();
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Execution Error : " + e.getMessage());
		}
		return null;		
	}
	

	
	private String authenticate() {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();
		body.add("j_username", login);
		body.add("j_password", password);

		HttpEntity<?> httpEntity = new HttpEntity<Object>(body, httpHeaders);
		ResponseEntity<Object> response = restTemplate.exchange(platformURL + "/api/authentication", HttpMethod.POST,
				httpEntity, Object.class);
		return response.getHeaders().get("Set-Cookie").get(0);
	}
}
