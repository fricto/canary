package com.citytechinc.monitoring.services.manager.actors.missioncontrol

import com.citytechinc.monitoring.api.monitor.MonitoredServiceWrapper
import com.citytechinc.monitoring.api.notification.NotificationAgentWrapper
import com.citytechinc.monitoring.api.persistence.RecordPersistenceServiceWrapper
import com.citytechinc.monitoring.api.responsehandler.PollResponseHandler
import com.citytechinc.monitoring.services.jcrpersistence.DetailedPollResponse
import com.citytechinc.monitoring.services.manager.ServiceMonitorRecordHolder
import com.citytechinc.monitoring.services.manager.actors.NotificationAgentActor
import com.citytechinc.monitoring.services.manager.actors.PollResponseHandlerActor
import com.citytechinc.monitoring.services.manager.actors.missioncontrol.messages.MonitoredServiceServiceRegistration
import com.citytechinc.monitoring.services.manager.actors.missioncontrol.messages.NotificationAgentServiceRegistration
import com.citytechinc.monitoring.services.manager.actors.missioncontrol.messages.PollResponseServiceRegistration
import com.citytechinc.monitoring.services.manager.actors.missioncontrol.messages.RecordPersistenceServiceRegistration
import com.citytechinc.monitoring.services.manager.actors.missioncontrol.messages.RegistrationType
import com.citytechinc.monitoring.services.manager.actors.monitor.MonitoredServiceActor
import groovy.util.logging.Slf4j
import groovyx.gpars.actor.DynamicDispatchActor

@Slf4j
/**
 *
 * @author Josh Durbin, CITYTECH, Inc. 2013
 *
 * Copyright 2013 CITYTECH, Inc.
 *
 */
class MissionControlActor extends DynamicDispatchActor {

    Map<MonitoredServiceWrapper, MonitoredServiceActor> monitors = [:]
    Map<NotificationAgentWrapper, NotificationAgentActor> notificationAgents = [:]
    Map<PollResponseHandler, PollResponseHandlerActor> pollResponseHandlers = [:]
    List<RecordPersistenceServiceWrapper> recordPersistenceServices = []

    def getHighestOrderPersistenceService = { def relevantRecord ->

        // singular load record
        def highestOrderService = recordPersistenceServices.sort { it.definition.ranking() }.first().service
        highestOrderService.getRecordHolder(relevantRecord)
    }

    void onMessage(RecordPersistenceServiceRegistration message) {

        def wrapper = new RecordPersistenceServiceWrapper(message.service)

        if (message.type == RegistrationType.register && !recordPersistenceServices.contains(wrapper)) {

            log.info("Registering persistence service ${wrapper.service.class.name}")
            recordPersistenceServices.add(wrapper)
        } else {

            log.info("Unregistering persistence service ${wrapper.service.class.name}")
            recordPersistenceServices.remove(wrapper)
        }
    }

    void onMessage(MonitoredServiceServiceRegistration message) {

        def wrapper = new MonitoredServiceWrapper(message.service)

        if (message.type == RegistrationType.register && !monitors.containsKey(wrapper)) {

            // PULL IN FROM DB
            def actor = new MonitoredServiceActor(wrapper: wrapper, missionControl: this, recordHolder: ServiceMonitorRecordHolder.CREATE_NEW(wrapper))
            actor.start()

            log.info("Starting actor for monitor ${wrapper.monitor.class.name}")

            monitors.put(wrapper, actor)
        } else {

            log.info("Termating actor for monitor ${wrapper.monitor.class.name}")
            monitors.get(wrapper).terminate()
        }
    }

    void onMessage(NotificationAgentServiceRegistration message) {

        def wrapper = new NotificationAgentWrapper(message.service)

        if (message.type == RegistrationType.register && !notificationAgents.containsKey(wrapper)) {

            def actor = new NotificationAgentActor(wrapper: wrapper)
            actor.start()

            log.info("Starting actor for notification agent ${wrapper.monitor.class.name}")

            notificationAgents.put(wrapper, actor)

        } else {

            log.info("Termating actor for notification agent ${wrapper.monitor.class.name}")
            notificationAgents.get(wrapper).terminate()
        }
    }

    void onMessage(PollResponseServiceRegistration message) {

        if (message.type == RegistrationType.register && !pollResponseHandlers.containsKey(message.service)) {

            def actor = new PollResponseHandlerActor(handler: message.service)
            actor.start()

            log.info("Starting actor for poll response handler ${message.service.class.name}")
            pollResponseHandlers.put(message.service, actor)

        } else {

            log.info("Termating actor for poll response handler ${message.service.class.name}")
            pollResponseHandlers.get(message.service).terminate()
        }
    }

    /**
     *
     * If a MonitoredServiceActor sends a
     *
     * @param message
     */
    void onMessage(DetailedPollResponse message) {

        pollResponseHandlers.values().each { it << message }
    }

    /**
     *
     * If a MonitoredServiceActor sends an entire ServiceMonitorRecordHolder, it indicates that it is in
     *   an alarmed state and thus sends it's entire set of records.
     *
     * MissionControlActor is responsible for then notification any NotificationAgentActors with the entire
     *   set of records. MissionControlActor is also responsible for requesting persistence of the alarmed state.
     *
     * @param message
     */
    void onMessage(ServiceMonitorRecordHolder message) {

        notificationAgents.values().each { it << message }
    }
}
