package com.example.demo;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
	
	@GetMapping("/healthCheck/{sub}")
	public ResponseEntity<?> healthCheck(@RequestHeader Map<String,String> reqHeaders, @PathVariable(name = "sub") String sub) {
		System.out.println("healthCheck...."+reqHeaders);
		return new ResponseEntity(sub, HttpStatus.OK);

	}
	
	@GetMapping("/")
	public ResponseEntity<?> healthCheckFail() {
		System.out.println("healthCheckFail....");
		return new ResponseEntity("Error", HttpStatus.SERVICE_UNAVAILABLE);

	}

}
