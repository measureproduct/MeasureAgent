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
package org.measure.platform.agent.smmengine.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.measure.platform.agent.platform.PlatformServiceProxy;
import org.measure.platform.agent.smmengine.api.IRemoteExecutionService;
import org.measure.smm.log.MeasureLog;
import org.measure.smm.measure.api.IDirectMeasure;
import org.measure.smm.measure.api.IMeasurement;
import org.measure.smm.measure.defaultimpl.measurements.DefaultMeasurement;
import org.measure.smm.remote.RemoteMeasureInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RemoteExecutionService implements IRemoteExecutionService {
	
	@Value("${measure.server.login}")
	private String login;	
	
	@Value("${measure.server.password}")
	private String password;
	
	@Value("${measure.server.url}")
	private String serverAdress;

	@Value("${measure.agent.name}")
	private String agentName;

	private final Logger log = LoggerFactory.getLogger(RemoteExecutionService.class);

	@Override
	public MeasureLog executeMeasure(RemoteMeasureInstance measureData, IDirectMeasure measure) {

		MeasureLog executionLog = new MeasureLog();

		executionLog.setMeasureInstanceName(measureData.getInstanceName());
		String measureName = measureData.getMeasureName().replace(" ("+agentName+")", "");
		executionLog.setMeasureName(measureName);
		executionLog.setMeasureInstanceId(measureData.getMeasureId());

		try {
			if (measure != null) {
				Map<String, String> ollProperties = new HashMap<>(measureData.getProperties());
				for (String key : measureData.getProperties().keySet()) {
					measure.getProperties().put(key, measureData.getProperties().get(key));
				}

				Date start = new Date();
				List<IMeasurement> measurements = measure.getMeasurement();
				
				
				List<DefaultMeasurement> defaultMeasurements = new ArrayList<>();
				for (IMeasurement measirement : measurements) {
					DefaultMeasurement newDef = new DefaultMeasurement();
				
					for(Entry<String,Object> entry : measirement.getValues().entrySet()){
						newDef.addValue(entry.getKey(), entry.getValue());
					}
					defaultMeasurements.add(newDef);
				}

				for (String key : ollProperties.keySet()) {
					if (ollProperties.get(key) != null && !ollProperties.get(key).equals(measure.getProperties().get(key))) {
						executionLog.getUpdatedParameters().put(key, measure.getProperties().get(key));
					}
				}

				executionLog.setExectionDate(new Date());
				executionLog.setExecutionTime(new Date().getTime() - start.getTime());
				executionLog.setMesurement(defaultMeasurements);
				executionLog.setSuccess(true);

				// Store Updated Properties
				measureData.setProperties(measure.getProperties());

			} else {
				executionLog.setExceptionMessage("Measure Unknown on Agent");
				executionLog.setSuccess(false);
			}
		} catch (Exception e) {
			log.error("Execution Failled [" + measureData.getMeasureName() + "] :" + e.getMessage());
			e.printStackTrace();
			executionLog.setExceptionMessage(e.getMessage());
			executionLog.setSuccess(false);
		}

		//sendExecutionResult(executionLog);

		return executionLog;
	}

	@Override
	public void sendExecutionResult(MeasureLog executionLog) {
	
		PlatformServiceProxy proxy = new PlatformServiceProxy(serverAdress, login, password);
		proxy.sendMeasureExecutionResult(executionLog);
	}

}
