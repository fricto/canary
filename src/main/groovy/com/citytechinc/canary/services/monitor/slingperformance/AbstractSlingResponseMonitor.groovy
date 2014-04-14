package com.citytechinc.canary.services.monitor.slingperformance

import com.citytechinc.canary.Constants
import com.citytechinc.canary.api.monitor.MonitoredService
import com.citytechinc.canary.api.monitor.PollResponse
import com.google.common.base.Stopwatch
import groovyx.gpars.agent.Agent
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.ConfigurationPolicy
import org.apache.felix.scr.annotations.Modified
import org.apache.felix.scr.annotations.Property
import org.apache.felix.scr.annotations.Properties
import org.apache.felix.scr.annotations.Service
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.request.RequestPathInfo
import org.apache.sling.commons.osgi.PropertiesUtil
import org.osgi.framework.Constants as OsgiConstants
import org.slf4j.Logger

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

@Component(componentAbstract = true, policy = ConfigurationPolicy.REQUIRE, immediate = true)
@Service
@Properties(value = [
        @Property(name = OsgiConstants.SERVICE_VENDOR, value = Constants.CITYTECH_SERVICE_VENDOR_NAME) ])
abstract class AbstractSlingResponseMonitor implements Filter, MonitoredService {

    private Agent requestStatistics = new RequestStatistics()

    @Property(name = 'warningThreshold', label = 'Response generation time warning threshold', longValue = 10L, description = '')
    private Long warningThreshold

    @Activate
    @Modified
    protected void activate(Map<String, Object> properties) throws Exception {
        warningThreshold = PropertiesUtil.toLong(properties.get('warningThreshold'), 10L)
    }

    @Override
    void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        def slingRequest = servletRequest as SlingHttpServletRequest
        def requestPathInfo = slingRequest.requestPathInfo
        def scrutinizeRequest = scrutinizeRequest(requestPathInfo)

        if (scrutinizeRequest) {

            def stopwatch = Stopwatch.createStarted()

            try {

                filterChain.doFilter(servletRequest, servletResponse)

            } finally {

                def duration = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)

                if (duration > warningThreshold) {

                    requestStatistics << {recordRequest(slingRequest.requestURI, duration, new Date(System.currentTimeMillis() - duration))}
                }
            }

        } else {

            filterChain.doFilter(servletRequest, servletResponse)
        }
    }

    @Override
    void destroy() {

    }

    @Override
    PollResponse poll() {

        Map<String, RequestStatistic> statistics = requestStatistics.val as Map

        requestStatistics << {clearStatistics()}

        def messages = statistics.keySet().collect {

            def stat = statistics.get(it)
            "'${it}' rendered ${stat.numberOfRequests} times, shortest: ${stat.shortestDuration}ms, average: ${stat.averageDuration}ms, longest: ${stat.longestDuration}ms"
        }

        messages ? PollResponse.SUCCESS() : PollResponse.WARNING().addMessages(messages)
    }

    abstract Logger getLogger()

    abstract Boolean scrutinizeRequest(RequestPathInfo requestPathInfo)

    class RequestStatistics extends Agent<Map<String, RequestStatistic>>  {

        def RequestStatistics() {

            super([:])
        }

        private def recordRequest(String requestURI, Long duration, Date occurrence) {

            if (data.containsKey(requestURI)) {

                data.get(requestURI).calculateUpdate(duration, occurrence)
            } else {

                data.put(requestURI, new RequestStatistic(duration, occurrence))
            }
        }

        private def clearStatistics() {

            data.clear()
        }
    }

    class RequestStatistic {

        private final Date firstOccurrence
        private Date mostRecentOccurrence

        private Integer numberOfRequests

        private Long shortestDuration
        private Long averageDuration
        private Long longestDuration

        RequestStatistic(Long duration, Date occurrence) {

            firstOccurrence = occurrence
            mostRecentOccurrence = firstOccurrence

            numberOfRequests = 1

            shortestDuration = duration
            averageDuration = duration
            longestDuration = duration
        }

        void calculateUpdate(Long duration, Date occurrence) {

            if (duration > longestDuration) {
                longestDuration = duration
            }

            if (duration < shortestDuration) {
                shortestDuration = duration
            }

            averageDuration = new BigDecimal(averageDuration * numberOfRequests + duration).divide(numberOfRequests + 1, RoundingMode.HALF_UP) as Long
            mostRecentOccurrence = occurrence

            ++numberOfRequests
        }
    }
}