package com.citytechinc.canary.components.dashboard

import com.citytechinc.canary.services.manager.ServiceManager
import com.citytechinc.canary.servlets.AbstractJSONResponseServlet
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.sling.SlingServlet
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse

/**
 *
 * @author Josh Durbin, CITYTECH, Inc. 2014
 *
 * Copyright 2014 CITYTECH, Inc.
 *
 */
@SlingServlet(
        resourceTypes = 'canary/components/page/dashboard',
        methods = 'POST',
        selectors = 'resetalarm',
        extensions = 'json')
class ResetAlarmForMonitor extends AbstractJSONResponseServlet {

    @Reference
    ServiceManager serviceManager

    @Override
    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {

        def recordHolder = serviceManager.getRecordHolder(request.getParameter('identifier'))
        def resetSuccess = false

        if (recordHolder.present) {

            if (recordHolder.get().isAlarmed()) {

                serviceManager.resetAlarm('identifier')
                resetSuccess = true
            }
        }

        writeJsonResponse(response, resetSuccess)
    }
}
