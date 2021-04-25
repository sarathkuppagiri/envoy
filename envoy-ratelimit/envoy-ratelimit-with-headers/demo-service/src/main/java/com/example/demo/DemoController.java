package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

	@GetMapping("/healthCheck")
	public ResponseEntity<?> healthCheck() {
		System.out.println("healthCheck....");
		return new ResponseEntity("Success", HttpStatus.OK);

	}

	@GetMapping("/quote")
	public ResponseEntity<?> quote() {
		return new ResponseEntity("quote response", HttpStatus.OK);

	}

}
