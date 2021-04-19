package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
	
	@GetMapping("/test1")
	public String sayHello()
	{
		return "Test1 result!!!";
	}

}
