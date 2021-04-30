# Java envoy management server with EDS (Endpoint Discovery Service)
This is a test java envoy management server used by Envoy to fetch cluster members.

Endpoint discovery service (EDS)

The endpoint discovery service is a xDS management server based on gRPC or REST-JSON API server used by Envoy to fetch cluster members. The cluster members are called “endpoint” in Envoy terminology. For each cluster, Envoy fetch the endpoints from the discovery service. EDS is the preferred service discovery mechanism for a few reasons:

Envoy has explicit knowledge of each upstream host (vs. routing through a DNS resolved load balancer) and can make more intelligent load balancing decisions.

Extra attributes carried in the discovery API response for each host inform Envoy of the host’s load balancing weight, canary status, zone, etc. These additional attributes are used globally by the Envoy mesh during load balancing, statistic gathering, etc.



## Building
```
cd dynamic-config-java-cp-eds/demo-service
mvn clean install -e
docker build -t demo-service:latest .

cd dynamic-config-java-cp-eds/envoy-java-management-server/V2/envoy-java-management-server
mvn clean install -e
docker build -t envoy-java-management-server:latest .

```

## Running
 * start control plane
```
docker run --rm -p 12345:12345 envoy-java-management-server:latest
```
 *  start demo-service
 ```
docker run --rm -p 8000:8000 demo-service:latest
```
 * connect envoy using sample config file
```
cd dynamic-config-java-cp-eds
 envoy -c envoy/envoy.yaml
```
 * test api
```
curl -s http://localhost:10000
```
