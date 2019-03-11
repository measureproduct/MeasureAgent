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
package org.measure.platform.agent.serviceregistrator;

import javax.inject.Inject;

import org.measure.platform.agent.platform.PlatformServiceProxy;
import org.measure.platform.agent.repository.api.IMeasureCatalogueService;
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

@Configuration
@EnableScheduling
public class PlatformRegistrator implements SchedulingConfigurer {
	
	
	@Value("${measure.server.login}")
	private String login;
	
	
	@Value("${measure.server.password}")
	private String password;
	
	
	@Value("${measure.server.url}")
	private String serverAdress;

	@Value("${measure.agent.name}")
	private String agentName;

	@Inject
	private IMeasureCatalogueService catalogueService;

	@Bean()
	public ThreadPoolTaskScheduler taskScheduler() {
		return new ThreadPoolTaskScheduler();
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setTaskScheduler(taskScheduler());
		
		PlatformServiceProxy platformProxy = new PlatformServiceProxy(serverAdress,login,password);
		platformProxy.registerMeasures(catalogueService.getAllMeasures(),agentName);

	}
	
	
	@Scheduled(fixedRate = 10000)
	public void reportCurrentTime() {
		PlatformServiceProxy platformProxy = new PlatformServiceProxy(serverAdress,login,password);
		platformProxy.registerMeasures(catalogueService.getAllMeasures(),agentName);

	}


}
