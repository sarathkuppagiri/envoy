### envoy

ENVOY IS AN OPEN SOURCE EDGE AND SERVICE PROXY, DESIGNED FOR CLOUD-NATIVE APPLICATIONS.

Envoy is an L7 proxy and communication bus designed for large modern service oriented architectures. 
The network should be transparent to applications.
When network and application problems do occur it should be easy to determine the source of the problem.

It is a self contained, high performance server with a small memory footprint.

It is a modern, high-performance, small footprint edge, and service proxy. Envoy adds resilience and observability to your services, and it does so in a way that’s transparent to your service implementation.

- It can proxy any TCP protocol.
- It can do SSL. Either direction.
- It makes HTTP/2 a first class citizen and can translate between HTTP/2 and HTTP/1.1 (either direction).
- It has good flexibility around discovery and load balancing.
- It’s meant to increase visibility into your system.
	- In particular, Envoy can generate a lot of traffic statistics and such that can otherwise be hard to get.
	- In some cases (like MongoDB and Amazon RDS), Envoy actually knows how to look into the wire protocol and do transparent monitoring.
- It’s less of a nightmare to set up than some others.
- It’s a sidecar process, so it’s completely agnostic to your services’ implementation language(s).
- High performance ,low latency, developer productivity.
- L3/L4 filter architecture.
- Service/config discovery and  active/passive health checking.
- It supports advanced load balancing features including automatic retries, circuit breaking, global rate limiting, request shadowing, zone local load balancing, traffic shifting etc.
-  It runs alongside any application language or framework.
- Envoy has first class support for HTTP/2 and gRPC for both incoming and outgoing connections. It is a transparent HTTP/1.1 to HTTP/2 proxy.
- Envoy provides robust APIs for dynamically managing its configuration.
- Deep observability of L7 traffic, native support for distributed tracing, and wire-level observability of MongoDB, DynamoDB, and more.
- Edge proxy - It acts as an edge proxy (load balancer).
- service/middle/edge proxy.
- Observability.
- Hot restart. (reload envoy without dropping any connections)