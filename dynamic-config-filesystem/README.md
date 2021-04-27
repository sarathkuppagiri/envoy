### Dynamic configuration (filesystem)

This example walks through configuring Envoy using filesystem-based dynamic configuration.

It demonstrates how configuration provided to Envoy dynamically can be updated without restarting the server.

### Step 1: Start the proxy container

Change directory to dynamic-config-filesystem in the Envoy repository.

Build and start the containers.

This should also start two upstream HTTP servers, demo-service1 and demo-service2.

docker-compose build --pull

docker-compose up -d

docker-compose ps

### Step 2: Check web response

```
curl -s http://localhost:10000
Response from service1

```

#### Step 3: Edit cds.yaml inside the container to update upstream cluster

docker-compose exec -T proxy sed -i s/service1/service2/ /var/lib/envoy/cds.yaml

### Step 5: Check Envoy uses updated configuration

```
curl -s http://localhost:10000
Response from service2

```
