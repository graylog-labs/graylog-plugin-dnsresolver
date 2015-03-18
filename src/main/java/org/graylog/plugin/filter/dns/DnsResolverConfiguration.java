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
