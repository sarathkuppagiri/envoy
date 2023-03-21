# Transcoding gRPC to HTTP/JSON

Sample project showing how to expose a gRPC service as a HTTP/JSON api. 

Built with Java 11, but 1.8 should also be supported.

Requirements: docker


## exposing the gRPC service as HTTP/JSON using Envoy proxy

Requirements:  
 * protoc (to generate a service definition that envoy understands)
 * docker (envoy comes in a docker container)

### Installing protoc
1. Goto: https://github.com/protocolbuffers/protobuf/releases/latest" +
2. download choose the precompiled version " +

       for linux:   protoc-3.6.1-linux-x86_64.zip" +
       for windows: protoc-3.6.1-win32.zip" +
       for mac:     protoc-3.6.1-osx-x86_64.zip or brew install protobuf" +

3. extract it somewhere in your PATH
   Run below command
   git clone https://github.com/googleapis/googleapis
   GOOGLEAPIS_DIR=<your-local-googleapis-folder>
   
   Run the protoc command from within this project's root directory! 
   
   protoc -I$(GOOGLEAPIS_DIR) -I. --include_imports --include_source_info \
    --descriptor_set_out=address.pb /Users/sarathkumarreddy/github/envoy/gRPC-JSON-transcoder-java/server/grpc-server/src/main/proto/CepService.proto

### Running Envoy to transcode our service

	cd server/grpc-server> mvn clean install -e
    docker-compose up
    
 
 
### Testing the REST API 
  

 curl --location --request POST 'http://localhost:8811/getAddr' \
--header 'Content-Type: application/json' \
--header 'Host: grpc' \
--data-raw '{"cep": "13960000"}'
        
   Example output:
   
   ```json
    {
    	"cep": "13960000",
    	"cidade": "Socorro"
	}
    ```
