# gRPC-Web Hello World Guide

This is a filter which enables the bridging of a gRPC-Web client to a compliant gRPC server.

## Define the Service

First, let's define a gRPC service using
[protocol buffers](https://developers.google.com/protocol-buffers/). Put this
in the `helloworld.proto` file. Here we define a request message, a response
message, and a service with one RPC method: `SayHello`.

```protobuf
syntax = "proto3";

package helloworld;

service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply);
}

message HelloRequest {
  string name = 1;
}

message HelloReply {
  string message = 1;
}
```

## Implement the Service

Then, we need to implement the gRPC Service. In this example, we will use
Java.

```java
package helloworld.grpc.service;

import helloworld.GreeterGrpc.GreeterImplBase;
import helloworld.Helloworld.HelloReply;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class GreeterServiceImpl extends GreeterImplBase {

	@Override
	public void sayHello(helloworld.Helloworld.HelloRequest request,
			io.grpc.stub.StreamObserver<helloworld.Helloworld.HelloReply> responseObserver) {
		System.out.println("sayHello...");
		String message = new StringBuilder().append("Hello, ").append(request.getName()).toString();

		HelloReply response = HelloReply.newBuilder().setMessage(message).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void sayRepeatHello(helloworld.Helloworld.RepeatHelloRequest request,
			io.grpc.stub.StreamObserver<helloworld.Helloworld.HelloReply> responseObserver) {
		System.out.println("sayRepeatHello...");
		String message = new StringBuilder().append("Hello, ").append(request.getName()).toString();
		System.out.println("count..." + request.getCount());

		for (int i = 0; i < request.getCount(); i++) {
			HelloReply response = HelloReply.newBuilder().setMessage(message).build();
			responseObserver.onNext(response);
		}

		responseObserver.onCompleted();
	}

}
```

## Configure the Proxy

Next up, we need to configure the Envoy proxy to forward the browser's gRPC-Web
requests to the backend. Put this in an `envoy.yaml` file. Here we configure
Envoy to listen at port `:8080`, and forward any gRPC-Web requests to a
cluster at port `:9090`.

```yaml
static_resources:
  listeners:
  - name: listener_0
    address:
      socket_address: { address: 0.0.0.0, port_value: 8080 }
    filter_chains:
    - filters:
      - name: envoy.filters.network.http_connection_manager
        typed_config:
          "@type": type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
          codec_type: auto
          stat_prefix: ingress_http
          route_config:
            name: local_route
            virtual_hosts:
            - name: local_service
              domains: ["*"]
              routes:
              - match: { prefix: "/" }
                route:
                  cluster: greeter_service
                  max_stream_duration:
                    grpc_timeout_header_max: 0s
              cors:
                allow_origin_string_match:
                - prefix: "*"
                allow_methods: GET, PUT, DELETE, POST, OPTIONS
                allow_headers: keep-alive,user-agent,cache-control,content-type,content-transfer-encoding,custom-header-1,x-accept-content-transfer-encoding,x-accept-response-streaming,x-user-agent,x-grpc-web,grpc-timeout
                max_age: "1728000"
                expose_headers: custom-header-1,grpc-status,grpc-message
          http_filters:
          - name: envoy.filters.http.grpc_web
          - name: envoy.filters.http.cors
          - name: envoy.filters.http.router
  clusters:
  - name: greeter_service
    connect_timeout: 0.25s
    type: logical_dns
    http2_protocol_options: {}
    lb_policy: round_robin
    load_assignment:
      cluster_name: cluster_0
      endpoints:
        - lb_endpoints:
            - endpoint:
                address:
                  socket_address:
                    address: grpc-server
                    port_value: 9090
```


## Write Client Code

Now, we are ready to write some client code! Put this in a `client.js` file.

```js
const {HelloRequest, HelloReply} = require('./helloworld_pb.js');
const {GreeterClient} = require('./helloworld_grpc_web_pb.js');

var client = new GreeterClient('http://localhost:8080');

var request = new HelloRequest();
request.setName('World');

client.sayHello(request, {}, (err, response) => {
  console.log(response.getMessage());
});
```

The classes `HelloRequest`, `HelloReply` and `GreeterClient` we import here are
generated for you by the `protoc` generator utility (which we will cover in the
next section) from the `helloworld.proto` file we defined earlier.

Then we instantiate a `GreeterClient` instance, set the field in the
`HelloRequest` protobuf object, and we can make a gRPC call via
`client.sayHello()`, just like how we defined in the `helloworld.proto` file.


You will need a `package.json` file. This is needed for both the `server.js` and
the `client.js` files.

```json
{
  "name": "grpc-web-simple-example",
  "version": "0.1.0",
  "description": "gRPC-Web simple example",
  "main": "server.js",
  "devDependencies": {
    "@grpc/grpc-js": "~1.0.5",
    "@grpc/proto-loader": "~0.5.4",
    "async": "~1.5.2",
    "google-protobuf": "~3.14.0",
    "grpc-web": "~1.2.1",
    "lodash": "~4.17.0",
    "webpack": "~4.43.0",
    "webpack-cli": "~3.3.11"
  }
}
```

And finally a simple `index.html` file.

```html
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>gRPC-Web Example</title>
<script src="./dist/main.js"></script>
</head>
<body>
  <p>Open up the developer console and see the logs for the output.</p>
</body>
</html>
```

The `./dist/main.js` file will be generated by `webpack` (which will be covered
in the next section).


And that's it! We have all the code ready. Let's run the example!

## Generate Protobuf Messages and Client Service Stub

To generate the protobuf messages and client service stub class from your
`.proto` definitions, we need:
 - the `protoc` binary, _and_
 - the `protoc-gen-grpc-web` plugin.

> You can download the `protoc-gen-grpc-web` protoc plugin from our
> [release](https://github.com/grpc/grpc-web/releases) page.

  	or

  brew install protoc-gen-grpc-web
>
> If you don't already have `protoc` installed, you will have to download it
> first from [here](https://github.com/protocolbuffers/protobuf/releases).

	or 

  brew install protobuf

>
> Make sure they are both executable and are discoverable from your PATH.
>
> For example, in MacOS, you can do:
>
> ```sh
> $ sudo mv ~/Downloads/protoc-gen-grpc-web-1.2.1-darwin-x86_64 \
>   /usr/local/bin/protoc-gen-grpc-web
> $ sudo chmod +x /usr/local/bin/protoc-gen-grpc-web
> ```


When you have both `protoc` and `protoc-gen-grpc-web` installed, you can now
run this command:

```sh
$ protoc -I=. helloworld.proto \
  --js_out=import_style=commonjs:. \
  --grpc-web_out=import_style=commonjs,mode=grpcwebtext:.
```

After the command runs successfully, you should now see two new files generated
in the current directory:

 - `helloworld_pb.js`: this contains the `HelloRequest` and `HelloReply`
   classes
 - `helloworld_grpc_web_pb.js`: this contains the `GreeterClient` class
 
These are also the 2 files that our `client.js` file imported earlier in the
example.

## Compile the Client JavaScript Code

Next, we need to compile the client side JavaScript code into something that
can be consumed by the browser.

```sh
$ npm install
$ npx webpack client.js
```

Here we use `webpack` and give it an entry point `client.js`. You can also use
`browserify` or other similar tools. This will resolve all the `require()`
statements and produce a `./dist/main.js` file that can be embedded in our
`index.html` file.

## Run the Example!

We are ready to run the Hello World example. The following set of commands will
run the 3 processes all in the background.

 1. Run the Java gRPC Service and envoy proxy together using below commands. Grpc server listens at port `:9090` and envoy listens at port `:8080`.

 ```sh
 $ cd grpc-web-js-java/server/hello-world-grpc-server
   mvn clean install -e
 $ docker-compose up
 ```
 2. Run the simple Web Server. This hosts the static file `index.html` and
 `dist/main.js` we generated earlier.

 ```sh
 $ python3 -m http.server 8081 &
 ```

When these are all ready, you can open a browser tab and navigate to

```
localhost:8081
```

Open up the developer console and you should see the following printed out:

```
Hello! World
```
