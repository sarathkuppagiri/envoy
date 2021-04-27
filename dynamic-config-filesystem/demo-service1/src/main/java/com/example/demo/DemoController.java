package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
	
	@GetMapping("/")
	public ResponseEntity<?> healthCheckFail() {
		return new ResponseEntity("Response from service1", HttpStatus.SERVICE_UNAVAILABLE);

	}

}
