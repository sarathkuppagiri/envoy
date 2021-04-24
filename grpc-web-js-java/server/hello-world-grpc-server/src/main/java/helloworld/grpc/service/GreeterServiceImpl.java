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
