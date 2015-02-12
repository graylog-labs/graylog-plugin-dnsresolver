package org.graylog.plugin.filter.dns;

import com.github.joschi.jadconfig.Parameter;
import org.graylog2.plugin.PluginConfigBean;
import org.joda.time.Period;

public class DnsResolverConfiguration implements PluginConfigBean {

    @Parameter(value = "dns_resolver_timeout")
    private Period resolverTimeout = Period.seconds(2);

    @Parameter(value = "dns_resolver_run_before_extractors")
    private boolean runBeforeExtractors = true;

    @Parameter(value = "dns_resolver_enabled")
    private boolean enabled = false;

    public boolean isRunBeforeExtractors() {
        return runBeforeExtractors;
    }

    public Period getResolverTimeout() {
        return resolverTimeout;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
