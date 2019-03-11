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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.measure.platform.agent.repository.api.IMeasureCatalogueService;
import org.measure.platform.agent.smmengine.api.IRemoteExecutionService;
import org.measure.platform.agent.smmengine.api.IShedulingService;
import org.measure.platform.agent.smmengine.impl.RemoteExecutionService;
import org.measure.smm.log.MeasureLog;
import org.measure.smm.measure.api.IDirectMeasure;
import org.measure.smm.remote.RemoteMeasureInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class SchedulingService implements IShedulingService {

	@Autowired
	private TaskScheduler taskScheduler;

	@Inject
	private IRemoteExecutionService measureExecutionService;

	@Inject
	private IMeasureCatalogueService measureCatalogue;

	private Map<Long, ScheduledFuture> jobs;
	
	private final Logger log = LoggerFactory.getLogger(RemoteExecutionService.class);


	@PostConstruct
	public void doSomething() {
		this.jobs = new HashMap<>();
	}

	@Override
	public synchronized Boolean scheduleMeasure(RemoteMeasureInstance measure) {
		if (measure.getShedulingExpression() != null && measure.getShedulingExpression().matches("\\d+")) {
			return scheduleLocalExecution(measure);
		}
		return false;
	}

	private boolean scheduleLocalExecution(RemoteMeasureInstance measure) {
		Integer rate = Integer.valueOf(measure.getShedulingExpression());

		try {
			IDirectMeasure measureImpl = measureCatalogue.getMeasureImplementation(measure.getMeasureName().substring(0, measure.getMeasureName().indexOf("(") - 1));

			ScheduledFuture job = taskScheduler.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					// Execute Measure
					MeasureLog result =measureExecutionService.executeMeasure(measure, measureImpl);
					measureExecutionService.sendExecutionResult(result);
				}
			}, rate);
			this.jobs.put(measure.getMeasureId(), job);
		} catch (Throwable e) {
			
			log.error("Execution Failled [" + measure.getMeasureName() + "] :" + e.getMessage());
			e.printStackTrace();
			
			MeasureLog errorlog = new MeasureLog();
			errorlog.setExceptionMessage(e.getMessage());
			errorlog.setMeasureInstanceId(measure.getMeasureId());
			errorlog.setMeasureName(measure.getMeasureName());
			errorlog.setMeasureInstanceName(measure.getInstanceName());
			errorlog.setSuccess(false);
			measureExecutionService.sendExecutionResult(errorlog);	
			return false;
		}
		
		return true;
	}

	@Override
	public synchronized Boolean removeMeasure(Long measureInstanceId) {
		// Stop Measures executed Localy
		ScheduledFuture job = jobs.get(measureInstanceId);
		if (job != null) {
			job.cancel(true);
			this.jobs.remove(measureInstanceId);
		}
		return true;
	}

	@Override
	public synchronized Set<Long> getSheduledMeasures() {
		return this.jobs.keySet();
	}

	@Override
	public synchronized Boolean isShedule(Long measureInstanceId) {
		return jobs.containsKey(measureInstanceId);
	}

}
