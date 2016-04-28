DNS Resolver Plugin for Graylog
===============================

[![Build Status](https://travis-ci.org/Graylog2/graylog-plugin-dnsresolver.svg)](https://travis-ci.org/Graylog2/graylog-plugin-dnsresolver)

This message filter plugin can be used to do DNS lookups for the `source` field in Graylog messages.

**Required Graylog version:** 2.0 and later

Please use version 1.1.2 of this plugin if you are still running Graylog 1.x

## Installation

[Download the plugin](https://github.com/Graylog2/graylog-plugin-dnsresolver/releases)
and place the `.jar` file in your Graylog plugin directory. The plugin directory
is the `plugins/` folder relative from your `graylog-server` directory by default
and can be configured in your `graylog.conf` file.

Restart `graylog-server` and you are done.

## Configuration

The following configuration options can be added to the Graylog configuration file.

* `dns_resolver_enabled` -- Set to `true` if the message filter should be run. (default `false`)
* `dns_resolver_run_before_extractors` -- Set to `true` if the DNS lookup should be done before running extractors. (default `true`)
* `dns_resolver_timeout` -- The timeout for the DNS lookup. (default `2s`)

## Build

This project is using Maven and requires Java 7 or higher.

You can build a plugin (JAR) with `mvn package`.

DEB and RPM packages can be build with `mvn jdeb:jdeb` and `mvn rpm:rpm` respectively.

## Plugin Release

We are using the maven release plugin:

```
$ mvn release:prepare
[...]
$ mvn release:perform
```

This sets the version numbers, creates a tag and pushes to GitHub. TravisCI will build the release artifacts and upload to GitHub automatically.
