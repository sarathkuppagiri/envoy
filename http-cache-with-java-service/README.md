## Cache Filter
In this example, we demonstrate how HTTP caching can be utilized in Envoy by using the Cache Filter.

### Step 1: Start all of our containers

 > cd http-cache-with-java-service/demo-service
 
 > mvn clean install -e
 
 > cd http-cache-with-java-service
 
 > docker-compose up
 
### Step 2: Test Envoy’s HTTP caching capabilities

curl -i localhost:8000/data 

This response remains fresh in the cache for a minute. After which, the response gets validated by the backend service before being served from the cache. If found to be updated, the new response is served (and cached). Otherwise, the cached response is served and refreshed.

curl -i localhost:8000/noCachedata

This response has to be validated every time before being served.
 
 
 
 
