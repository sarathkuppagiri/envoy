# JSON Web Token (JWT) Authentication
The JSON Web Token (JWT) Authentication filter checks if the incoming request has a valid JSON Web Token (JWT). It checks the validity of the JWT by verifying the JWT signature, audiences and issuer based on the HTTP filter configuration. The JWT Authentication filter could be configured to either reject the request with invalid JWT immediately or defer the decision to later filters by passing the JWT payload to other filters.

The JWT Authentication filter supports to check the JWT under various conditions of the request, it could be configured to check JWT only on specific paths so that you could allowlist some paths from the JWT authentication, which is useful if a path is accessible publicly and doesn’t require any JWT authentication.

The JWT Authentication filter supports to extract the JWT from various locations of the request and could combine multiple JWT requirements for the same request. The JSON Web Key Set (JWKS) needed for the JWT signature verification could be either specified inline in the filter config or fetched from remote server via HTTP/HTTPS.

The JWT Authentication filter also supports to write the header and payload of the successfully verified JWT to Dynamic State so that later filters could use it to make their own decisions based on the JWT payloads.

authorization-server> mvn clean install -e 
authorization-server> mvn spring-boot:run

curl --location --request GET 'http://localhost:8083/getAccessToken'

response :

eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJaZ25HN0E2WWREMGZOSktfbnNHcDNTVE1ZNHMzSkpsaXBfRVRlNEtWS19VIn0.eyJleHAiOjE2NjA1NDA5MjksImlhdCI6MTY2MDU0MDYyOSwiYXV0aF90aW1lIjoxNjYwNTQwNjI5LCJqdGkiOiIxZTE3NzllZi0yYzdiLTRlMGEtYjkyOC1mMjUwNDRjMGE1ZTgiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODMvYXV0aC9yZWFsbXMvYmFlbGR1bmciLCJzdWIiOiJhNTQ2MTQ3MC0zM2ViLTRiMmQtODJkNC1iMDQ4NGU5NmFkN2YiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJmb29DbGllbnQiLCJzZXNzaW9uX3N0YXRlIjoiMWRjNGI3MzEtMmE3My00NzAyLTk2NDgtNTYxYTlmMWE0MWQ5Iiwic2NvcGUiOiJwcm9maWxlIHJlYWQiLCJzaWQiOiIxZGM0YjczMS0yYTczLTQ3MDItOTY0OC01NjFhOWYxYTQxZDkiLCJvcmdhbml6YXRpb24iOiJiYWVsZHVuZyIsInByZWZlcnJlZF91c2VybmFtZSI6ImpvaG5AdGVzdC5jb20ifQ.QlGYg85rRM-u6lICss7pjUOghTjA0WdUhvPyu7gvDTx5H_ZMfAMOyAzAF5NbMn83O5rMmmzRruW9RHr-aoPzm6eCRnPtHE6Ahvc7ujZpDoVLLee6gEjywoywojYpDKRiUVivM70g9skmjbPqUYV5qMnuoffXmMV_y7EACJf93zbrSHPxpKE02dGrgsTH8paov9c05N8pnh4F-ahosIu3HI0zyFrPEf2AYAJ0-THX11xusF6eJvle2ZU4m6J5d42-Pgtt_0KFv41C7BWugcKbMlmIL4bzpztokVFYdkRr16PDCAuJsKG8TLggLL5nu5HS-RENB10y393CMH7a8_qj9A



envoy -c envoy-jwt.yaml
