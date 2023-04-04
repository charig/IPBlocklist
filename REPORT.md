# IP Blocklist Service Design Report

## Introduction

The purpose of this document is to explain the design choices made in the development of the IP Blocklist service, and how it meets both the functional and non-functional requirements. Additionally, any compromises or trade-offs taken because of time constraints will be discussed.

## Requirements

The IP Blocklist service has the following requirements:

1.  Highly available service, minimizing the time it takes to restart it and the downtime when updating the blocklist.
2.  Service should remain operational under heavy load and be able to respond in a reasonably low time.

## Assumptions
* Service answers are considered valid/consistent during 24hs after syncing with the source
* Source data is mostly stable, but occasional .
* Source data is mostly stable, i.e, between subsequent syncs, no much data is removed and not that many ips are added. In case large changes occur, the service still should be able to update the cache in a timely and efficient manner
* The server should only answer true for the IPs in the list, and for everything else it is ok to just answer false
* Based on the expected time, and lack of deployment infrastructure, I assume that a first version of the blocklist should be enough to run in a single process service.

## Design Choices

The service consists of a simple REST endpoint managed by Quarkus supported by an on memory cache using Caffeine that is populated at runtime and updated periodically (every 24 hours) in a non-blocking manner.

### Quarkus
I decided to use Quarkus because it is lightweight, reliable, simple, boots fast, and has native support for building native images using the GraalVM.

### Caffeine

To speed up the service, based on the assumption of a single process, I chose Caffeine, a Java library that provides a thread-safe in-memory cache using a key-value store model. Caffeine is an in-memory caching library that provides a fast and lightweight cache implementation.

### Asynchronous Syncing
The syncing process is asynchronous to ensure no downtime when updating the blocklist. I decided to compute the difference between the existing and previous list and update the cache accordingly because it should be more efficient based on the assumption of data stability.

### Health Check
The service already includes a health check in `/q/health`. A simple process (not provided) could be run to periodically test for the service health and restart the process in case it is needed.

### GraalVM Images
I provide a GraalVM images because of its fast startup and because of its memory efficient. It also has a reduced memory footprint.

## Alternatives

If a stronger consistency requirement for the IPs is needed, the scheduled task could be changed with a cron with the precise time the source is updated if known. Alternatively, a periodic update that just polls the first lines of the list and updates in case there is a new version (timestamp) or even could poll GitHub APIs to check for a new commit on the repo. The trade-off is clear: a delay between the source update and the cache update or more network traffic and complexity due to polling.

### Horizontal Scaling
If the assumption of a single process is no longer valid, even in the case of vertical scaling, a horizontal scaling approach can be taken. More instances of the service could be deployed in a cluster with multiple instances running simultaneously. IKubernetes can be used to orchestrate the deployment, so if one instance goes down, the other instances can continue to serve requests. The current boot time of the service is fairly low, so the same model could be leveraged. However, if consistency among server responses is needed, the cache should be changed to work on a distributed caching system that can be used to share cache data across multiple machines. Redis is an option. Therefore, the service should be split into two: an update that populates the Redis cache and a REST service that consumes data from that Redis. In the simplest case in which a single service is enough, it would be better to avoid introducing the overhead and complexity of a distributed caching system with its additional monitoring and maintenance efforts.

### Load Balancer and CDN

A load balancer may also be needed to distribute the incoming traffic among the instances. This will help to evenly distribute the load and prevent any one instance from becoming overloaded. In case load is still an issue, another option is to consider using a content delivery network (CDN) to cache the responses and serve them from a location closer to the user.

### Health Check
The health check could be extended to support a cluster deployment

### CI/CD Infrastructure
To ensure the quality and reliability of the IP Blocklist service, it should be integrated into a CI/CD (Continuous Integration/Continuous Deployment) infrastructure. This will ensure that all changes to the codebase are automatically tested, built, and deployed to production.
