package com.example.demo;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.CepService.HelloRequest;
import com.example.demo.CepService.HelloResponse;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;

@RestController
public class DemoController {

	@PostMapping(path = "/HelloService/hello")
	public ResponseEntity<?> hello(HttpServletResponse servletResponse, @RequestBody byte[] body,
			@RequestHeader Map<String, String> headers) {
		HelloRequest helloRequest;
		try {
			helloRequest = HelloRequest.newBuilder().build().parseFrom(body);
			System.out.println("helloRequest...." + helloRequest.getData());
			Gson gson = new Gson();
			System.out.println(gson.toJson(helloRequest));
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}

		headers.forEach((key, value) -> {
			System.out.println(String.format("Header '%s' = %s", key, value));
		});
		HelloResponse resp = HelloResponse.newBuilder().setResponse("sample response from demo service").build();
		return new ResponseEntity(resp.toByteArray(), HttpStatus.OK);

	}

	@GetMapping("/")
	public ResponseEntity<?> healthCheckFail() {
		System.out.println("healthCheckFail....");
		return new ResponseEntity("Error", HttpStatus.SERVICE_UNAVAILABLE);

	}

}
