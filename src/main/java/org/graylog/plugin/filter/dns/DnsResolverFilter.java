/**
 * Copyright (C) 2015 Graylog, Inc. (hello@graylog.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.graylog.plugin.filter.dns;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
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

    private final TimeLimiter timeLimiter;
    private final Timer resolveTime;
    private final Meter resolveTimeouts;
    private final long timeout;
    private final boolean shouldRunBeforeExtractors;
    private final boolean enabled;

    @Inject
    public DnsResolverFilter(@Named("dns_resolver_timeout") Period resolverTimeout,
                             @Named("dns_resolver_run_before_extractors") boolean shouldRunBeforeExtractors,
                             @Named("dns_resolver_enabled") boolean enabled,
                             MetricRegistry metricRegistry) {
        this.shouldRunBeforeExtractors = shouldRunBeforeExtractors;
        this.enabled = enabled;
        timeout = resolverTimeout.toStandardDuration().getMillis();
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
        if (!enabled) {
            return false;
        }

        final String source = msg.getSource();
        try {
            try (Timer.Context ignored = resolveTime.time()) {
                final String hostname = timeLimiter.callWithTimeout(getLookupCallable(source), timeout, TimeUnit.MILLISECONDS, true);

                if (hostname != null) {
                    msg.setSource(hostname);
                }
            }

        } catch (IllegalArgumentException e) {
            log.debug("Source {} of message {} is not an IP literal. Cannot look up the hostname.", source, msg.getId());
        } catch (Exception e) {
            resolveTimeouts.mark();
            log.debug("DNS request timed out, skipping looking up {} for message {}", source, msg.getId());
        }
        return false;
    }

    @VisibleForTesting
    protected Callable<String> getLookupCallable(final String source) {
        return new Callable<String>() {
            @Override
            public String call() throws Exception {
                final InetAddress inetAddress = InetAddresses.forString(source);
                return inetAddress.getCanonicalHostName();
            }
        };
    }

    @Override
    public String getName() {
        return "DNS Resolver";
    }

    @Override
    public int getPriority() {
        // MAGIC NUMBER: 10 is the priority of the ExtractorFilter, we either run before or after it, depending on what the user wants.
        return 10 - (shouldRunBeforeExtractors ? 1 : -1);
    }
}
