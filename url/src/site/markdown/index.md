## About URL Handler

URL resolving and processing.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.handler.url/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.wcm/io.wcm.handler.url)


### Documentation

* [Usage][usage]
* [Suffix Builder and Parser][suffix-builder-parser]
* [Integrator Template Mode][integrator]
* [Sling Rewriter Integration][rewriter]
* [API documentation][apidocs]
* [Changelog][changelog]


### Overview

The URL Handler provides:

* Building URLs from path, selectors, extension, suffix query string an fragment parts
* Externalizing links for page links and frontend resources
* Supporting different URL Modes for externalizing to HTTP/HTTPs, with full hostname or protocol-relative mode
* Hostnames used for externalization for HTTP and HTTPs are stored in [context-specific configuration][config]
* Rewrites URLs to current site
* [Suffix Builder and Parser][suffix-builder-parser] for passing around information via Sling Suffix string
* Supports externalizing URLs for [Integrator Template Mode][integrator] with placeholders or Full URLs
* Supports externalizing URLs in generated markup via [Sling Rewriter][rewriter]
* Generic Sling Models for usage in views: [Sling Models][ui-package]


[usage]: usage.html
[suffix-builder-parser]: suffix-builder-parser.html
[integrator]: integrator.html
[rewriter]: rewriter.html
[apidocs]: apidocs/
[changelog]: changes-report.html
[config]: ../../config/
[ui-package]: apidocs/io/wcm/handler/url/ui/package-summary.html
