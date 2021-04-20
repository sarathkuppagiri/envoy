package com.example.demo;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

	@GetMapping("/listHeaders")
	public ResponseEntity<?> sayHello(@RequestHeader Map<String, String> headers) {
		headers.forEach((key, value) -> {
			System.out.println(String.format("Header '%s' = %s", key, value));
		});
		return new ResponseEntity("Server issues", HttpStatus.INTERNAL_SERVER_ERROR);

	}
	
	@GetMapping("/testTimeout/xxx")
	public ResponseEntity<?> testTimeout() {
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResponseEntity("Success", HttpStatus.OK);

	}

}
