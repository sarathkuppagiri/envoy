package com.baeldung.authserver.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.restassured.RestAssured;
import io.restassured.response.Response;

@RestController
public class AccessTokenController {

	@GetMapping(value = "/getAccessToken")
	public String getAccessToken() {
		final String redirectUrl = "http://localhost:8080/";
		final String authorizeUrl = "http://localhost:8083/auth/realms/baeldung/protocol/openid-connect/auth?response_type=code&client_id=fooClient&scope=read&redirect_uri="
				+ redirectUrl;
		final String tokenUrl = "http://localhost:8083/auth/realms/baeldung/protocol/openid-connect/token";
		// obtain authentication url with custom codes
		Response response = RestAssured.given().redirects().follow(false).get(authorizeUrl);
		String authSessionId = response.getCookie("AUTH_SESSION_ID");
		String kcPostAuthenticationUrl = response.asString().split("action=\"")[1].split("\"")[0].replace("&amp;", "&");

		// obtain authentication code and state
		response = RestAssured.given().redirects().follow(false).cookie("AUTH_SESSION_ID", authSessionId)
				.formParams("username", "john@test.com", "password", "123", "credentialId", "")
				.post(kcPostAuthenticationUrl);
		assertThat(HttpStatus.FOUND.value()).isEqualTo(response.getStatusCode());

		// extract authorization code
		String location = response.getHeader(HttpHeaders.LOCATION);
		String code = location.split("code=")[1].split("&")[0];

		// get access token
		Map<String, String> params = new HashMap<String, String>();
		params.put("grant_type", "authorization_code");
		params.put("code", code);
		params.put("client_id", "fooClient");
		params.put("redirect_uri", redirectUrl);
		params.put("client_secret", "fooClientSecret");
		response = RestAssured.given().formParams(params).post(tokenUrl);
		return response.jsonPath().getString("access_token");
	}

}
