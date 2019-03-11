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
package org.measure.platform.agent.smmengine.impl.sheduller;

import javax.inject.Inject;

import org.measure.platform.agent.platform.PlatformServiceProxy;
import org.measure.platform.agent.repository.api.IMeasureCatalogueService;
import org.measure.platform.agent.smmengine.impl.RemoteExecutionService;
import org.measure.smm.log.MeasureLog;
import org.measure.smm.measure.api.IDirectMeasure;
import org.measure.smm.measure.api.IMeasurement;
import org.measure.smm.remote.RemoteMeasureExternal;
import org.measure.smm.remote.RemoteMeasureInstance;
import org.measure.smm.remote.RemoteMeasureInstanceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableScheduling
public class MeasureSchedulingConfigurer implements SchedulingConfigurer {


	@Value("${measure.server.login}")
	private String login;
		
	@Value("${measure.server.password}")
	private String password;
	
	@Value("${measure.server.url}")
	private String platformUrl;

	@Value("${measure.agent.name}")
	private String agentName;
	
	@Inject
	private SchedulingService sheduleService;
	
	@Inject
	private RemoteExecutionService executionService;
	
	@Inject
	private IMeasureCatalogueService measureCatalogue;
	
	private final Logger log = LoggerFactory.getLogger(SchedulingService.class);


	@Bean()
	public ThreadPoolTaskScheduler taskScheduler() {
		return new ThreadPoolTaskScheduler();
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setTaskScheduler(taskScheduler());	
	}
	
	
	@Scheduled(fixedRate = 10000)
	public void reportCurrentTime() {
		
		try {

			PlatformServiceProxy proxy = new PlatformServiceProxy(platformUrl, login, password);
			RemoteMeasureInstanceList result = proxy.getMeasureInstanceList(agentName);
		
			
			// Remove old measure execution		
			for(Long measureId : sheduleService.getSheduledMeasures()){
				RemoteMeasureInstance existingInstance = null; 
				for(RemoteMeasureInstance instance : result.getRemoteInstances()){
					if(instance.getMeasureId().equals(measureId)){
						existingInstance = instance;
						break;
					}
				}		
				if(existingInstance == null){
					sheduleService.removeMeasure(measureId);
					log.info("Remove Measure Instance : " + measureId);
				}			
			}
			
			// Execution Request
			for(RemoteMeasureExternal executionRequest : result.getSingleExecutions()){	
				IDirectMeasure measure = measureCatalogue.getMeasureImplementation(executionRequest.getMeasureName().substring(0, executionRequest.getMeasureName().indexOf("(") - 1));
				MeasureLog executionResult = executionService.executeMeasure(executionRequest, measure);
				
				if(executionResult.isSuccess()){
					if(executionRequest.getAlternateDate() != null && executionRequest.getDateField() != null){
						for(IMeasurement measurement : executionResult.getMesurement()){
							measurement.getValues().put(executionRequest.getDateField(), executionRequest.getAlternateDate());
						}
					}
			
					executionService.sendExecutionResult(executionResult);
				}
			}
			// Add new measure execution
			for(RemoteMeasureInstance instance : result.getRemoteInstances()){
				if(!sheduleService.isShedule(instance.getMeasureId())){
					// Scheduling Request
					
					sheduleService.scheduleMeasure(instance);
					log.info("Add Measure Instance : " + instance.getInstanceName());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Measure Scheduling Error : " + e.getMessage());
			return;
		}
		
	}
}
