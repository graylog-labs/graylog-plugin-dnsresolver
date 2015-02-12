package org.graylog.plugin.filter.dns;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.InetAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * This is the plugin. Your class should implement one of the existing plugin
 * interfaces. (i.e. AlarmCallback, MessageInput, MessageOutput)
 */
public class DnsResolverFilter implements MessageFilter {
    private static final Logger log = LoggerFactory.getLogger(DnsResolverFilter.class);

    private final DnsResolverConfiguration config;
    private final TimeLimiter timeLimiter;
    private final Timer resolveTime;
    private final Meter resolveTimeouts;
    private final long timeout;

    @Inject
    public DnsResolverFilter(DnsResolverConfiguration config, MetricRegistry metricRegistry) {
        this.config = config;
        timeout = config.getResolverTimeout().toStandardDuration().getMillis();
        timeLimiter = new SimpleTimeLimiter(
                Executors.newSingleThreadExecutor(
                        new ThreadFactoryBuilder()
                                .setDaemon(true)
                                .setNameFormat("dns-resolver-thread-%d")
                                .build()
                )
        );
        this.resolveTime = metricRegistry.timer(name(DnsResolverFilter.class, "resolveTime"));
        this.resolveTimeouts = metricRegistry.meter(name(DnsResolverFilter.class, "resolveTimeouts"));
    }

    @Override
    public boolean filter(Message msg) {
        final String source = msg.getSource();
        try {
            try (Timer.Context ignored = resolveTime.time()) {
                final String hostname = timeLimiter.callWithTimeout(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        final InetAddress inetAddress = InetAddresses.forString(source);
                        return inetAddress.getCanonicalHostName();
                    }
                }, timeout, TimeUnit.MILLISECONDS, true);

                if (hostname != null) {
                    msg.setSource(hostname);
                }
            }

        } catch (IllegalArgumentException e) {
            if (log.isDebugEnabled()) {
                log.debug("Source {} of message {} is not an IP literal. Cannot look up the hostname.", source, msg.getId());
            }
        } catch (Exception e) {
            resolveTimeouts.mark();
            if (log.isDebugEnabled()) {
                log.debug("DNS request timed out, skipping looking up {} for message {}", source, msg.getId());
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return "DNS Resolver";
    }

    @Override
    public int getPriority() {
        // MAGIC NUMBER: 10 is the priority of the ExtractorFilter, we either run before or after it, depending on what the user wants.
        return 10 - (config.isRunBeforeExtractors() ? 1 : -1);
    }
}
