package org.graylog.plugin.filter.dns;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class DnsResolverFilterTest {

    private MetricRegistry metricRegistry;

    @Before
    public void init() {
        metricRegistry = new MetricRegistry();
    }

    @Test
    public void disabledFilter() {
        final DnsResolverFilter resolver = new DnsResolverFilter(Period.seconds(1),
                                                                          false,
                                                                          false,
                                                                          metricRegistry);

        final Message msg = new Message("test", "127.0.0.1", Tools.nowUTC());
        final boolean filter = resolver.filter(msg);

        assertFalse("Message should not be filtered out", filter);

        assertEquals("localhost ip should not be resolved, filter is disabled", "127.0.0.1", msg.getSource());
    }

    @Test
    public void filterEnabledAndResolvesLocalhost() {
        final DnsResolverFilter resolver = new DnsResolverFilter(Period.seconds(1),
                                                                 false,
                                                                 true,
                                                                 metricRegistry);

        final Message msg = new Message("test", "127.0.0.1", Tools.nowUTC());
        final boolean filter = resolver.filter(msg);

        assertFalse("Message should not be filtered out", filter);

        assertEquals("localhost ip should be resolved, filter is enabled", "localhost", msg.getSource());

        assertEquals("should have looked up one time", 1, metricRegistry.timer(name(DnsResolverFilter.class, "resolveTime")).getCount());

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

        final Message msg = new Message("test", "127.0.0.1", Tools.nowUTC());

        final boolean filter = resolver.filter(msg);
        assertFalse("Message should not be filtered out", filter);

        assertNotEquals("Late callback results should not be used.", "should not be used", msg.getSource());

        // check for metrics
        assertEquals("should have timed out once", 1, metricRegistry.meter(name(DnsResolverFilter.class, "resolveTimeouts")).getCount());
    }

}