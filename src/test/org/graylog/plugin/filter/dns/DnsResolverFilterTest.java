package org.graylog.plugin.filter.dns;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.joda.time.Period;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static org.testng.Assert.*;

public class DnsResolverFilterTest {

    private MetricRegistry metricRegistry;

    @BeforeTest
    public void init() {
        metricRegistry = new MetricRegistry();
    }

    @AfterTest
    public void destroy() {
        metricRegistry.removeMatching(MetricFilter.ALL);
        metricRegistry = null;
    }

    @Test
    public void disabledFilter() {
        final DnsResolverFilter resolver = new DnsResolverFilter(Period.seconds(1),
                                                                          false,
                                                                          false,
                                                                          metricRegistry);

        final Message msg = new Message("test", "127.0.0.1", Tools.iso8601());
        final boolean filter = resolver.filter(msg);

        assertFalse(filter, "Message should not be filtered out");

        assertEquals(msg.getSource(), "127.0.0.1", "localhost ip should not be resolved, filter is disabled");
    }

    @Test
    public void filterEnabledAndResolvesLocalhost() {
        final DnsResolverFilter resolver = new DnsResolverFilter(Period.seconds(1),
                                                                 false,
                                                                 true,
                                                                 metricRegistry);

        final Message msg = new Message("test", "127.0.0.1", Tools.iso8601());
        final boolean filter = resolver.filter(msg);

        assertFalse(filter, "Message should not be filtered out");

        assertEquals(msg.getSource(), "localhost", "localhost ip should be resolved, filter is enabled");

        assertEquals(metricRegistry.timer(name(DnsResolverFilter.class, "resolveTime")).getCount(), 1, "should have looked up one time");

    }

    @Test
    public void lookupsTimeout() {
        final DnsResolverFilter resolver = new DnsResolverFilter(Period.seconds(1),
                                                                 false,
                                                                 true,
                                                                 metricRegistry) {
            @Override
            protected Callable<String> getLookupCallable(String source) {
                return new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
                        return "should not be used";
                    }
                };
            }
        };

        final Message msg = new Message("test", "127.0.0.1", Tools.iso8601());

        final boolean filter = resolver.filter(msg);
        assertFalse(filter, "Message should not be filtered out");

        assertNotEquals("should not be used", msg.getSource(), "Late callback results should not be used.");

        // check for metrics
        assertEquals(metricRegistry.meter(name(DnsResolverFilter.class, "resolveTimeouts")).getCount(), 1, "should have timed out once");
    }

}