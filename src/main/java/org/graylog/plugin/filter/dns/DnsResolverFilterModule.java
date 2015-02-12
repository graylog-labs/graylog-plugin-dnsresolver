package org.graylog.plugin.filter.dns;

import com.google.common.collect.Sets;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Set;

public class DnsResolverFilterModule extends PluginModule {

    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Sets.newHashSet(new DnsResolverConfiguration());
    }

    @Override
    protected void configure() {
        addMessageFilter(DnsResolverFilter.class);
        addConfigBeans();
    }
}
