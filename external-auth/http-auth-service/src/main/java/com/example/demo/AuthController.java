package com.example.demo;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class AuthController {

	@GetMapping("/")
	public ResponseEntity<?> auth(@RequestHeader(name = "authorization", required = false) String auth,
			HttpServletResponse response) {
		System.out.println("auth heade...." + auth);
		if (auth != null) {
			response.setHeader("x-current-user", "sarath");
			return new ResponseEntity("Success", HttpStatus.OK);
		} else
			return new ResponseEntity("Forbidden", HttpStatus.FORBIDDEN);

	}
}
