# Graylog DnsResolverFilter Plugin

This message filter plugin can be used to do DNS lookups for the `source` field in Graylog messages.

## Configuration

The following configuration options can be added to the Graylog configuration file.

* `dns_resolver_enabled` -- Set to `true` if the message filter should be run. (default `false`)
* `dns_resolver_run_before_extractors` -- Set to `true` if the DNS lookup should be done before running extractors. (default `true`)
* `dns_resolver_timeout` -- The timeout for the DNS lookup. (default `2s`)

## Building

This project is using Maven 3 and requires Java 7 or higher. The plugin will require Graylog 1.0.0 or higher.

* Clone this repository.
* Run `mvn package` to build a JAR file.
* Optional: Run `mvn jdeb:jdeb` and `mvn rpm:rpm` to create a DEB and RPM package respectively.
* Copy generated jar file in target directory to your Graylog2 server plugin directory.
* Restart the Graylog server.
