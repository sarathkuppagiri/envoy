package com.example.demo;

import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

	@GetMapping("/data")
	public ResponseEntity<?> getData(HttpServletResponse response) {
		System.out.println("getData......");
		response.setHeader("cache-control", "max-age=60");
		return new ResponseEntity("sample response from demo-service : " + new Date(), HttpStatus.OK);

	}
	
	@GetMapping("/noCachedata")
	public ResponseEntity<?> noCachedata(HttpServletResponse response) {
		System.out.println("noCachedata......");
		response.setHeader("cache-control", "max-age=0, no-cache");
		return new ResponseEntity("sample response from demo-service : " + new Date(), HttpStatus.OK);

	}

}
