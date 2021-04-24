# grpc_http1_reverse_bridge

grpc_http1_reverse_bridge filter which allows gRPC requests to be sent to Envoy and then translated to HTTP/1.1 when sent to the upstream. 
The response is then converted back into gRPC when sent to the downstream. 
This filter can also optionally manage the gRPC frame header, allowing the upstream to not have to be gRPC aware at all.

The filter works by:

Checking the content type of the incoming request. If it’s a gRPC request, the filter is enabled.

The content type is modified to a configurable value. This can be a noop by configuring application/grpc.

The gRPC frame header is optionally stripped from the request body. The content length header will be adjusted if so.

On receiving a response, the content type of the response is validated and the status code is mapped to a grpc-status which is inserted into the response trailers.

The response body is optionally prefixed by the gRPC frame header, again adjusting the content length header if necessary.

Due to being mapped to HTTP/1.1, this filter will only work with unary gRPC calls.



cd grpc-reverse-bridge-java/server/http-server

mvn clean install -e

docker-compose up

### Testing the API

You can test GRPC API with below options

grpcurl -v \
-proto /grpc-reverse-bridge-java/server/hello.proto \
-plaintext \
-import-path ./proto \
-d '{}' \
localhost:8811 \
HelloService/hello

or

echo {"data": "sample data"} | java -jar polyglot.jar call --endpoint=localhost:8811 --full_method=/HelloService/hello

or 

Bloom RPC


