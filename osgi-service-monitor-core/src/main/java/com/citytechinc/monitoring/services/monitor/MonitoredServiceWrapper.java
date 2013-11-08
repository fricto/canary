package com.citytechinc.monitoring.services.monitor;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Josh Durbin, CITYTECH, Inc. 2013
 *
 * Copyright 2013 CITYTECH, Inc.
 *
 */
public final class MonitoredServiceWrapper {

    private final MonitoredService monitor;
    private final MonitoredServiceDefinition definition;
    private final AutoResumingPoller pollerDefinition;
    private final Long pollIntervalInMilliseconds;

    public MonitoredServiceWrapper(final MonitoredService monitor) {
        this.monitor = monitor;
        definition = monitor.getClass().getAnnotation(MonitoredServiceDefinition.class);
        pollerDefinition = monitor.getClass().getAnnotation(AutoResumingPoller.class);
        pollIntervalInMilliseconds = TimeUnit.MILLISECONDS.convert(definition.pollFrequency(), definition.pollFrequencyUnit());
    }

    public MonitoredServiceDefinition getDefinition() {
        return definition;
    }

    public MonitoredService getMonitor() {
        return monitor;
    }

    public AutoResumingPoller getPollerDefinition() {
        return pollerDefinition;
    }

    public Long getPollIntervalInMilliseconds() {
        return pollIntervalInMilliseconds;
    }
}
