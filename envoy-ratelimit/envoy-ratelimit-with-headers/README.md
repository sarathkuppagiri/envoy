## Envoy Global rate limiting

This application demonstrating basic aspects fo Envoy's [Global Rate Limiting](https://www.envoyproxy.io/docs/envoy/v1.5.0/intro/arch_overview/global_rate_limiting) capability.  This app configures a local envoy instance for an [HTTP level rate limit filter](https://www.envoyproxy.io/docs/envoy/v1.5.0/configuration/http_filters/rate_limit_filter#config-http-filters-rate-limit) and then uses [Lyft's RateLimit Service](https://github.com/lyft/ratelimit) as the backend.

Envoy's rate limit functions checks each request to see if it should go through or not either using a local instance specific criteria or globally.  By local, the rate limit counter runs within the context of the single envoy proxy that handles the request.  This means each proxy keeps track of the connections it manages and applies [circuit braking](https://www.envoyproxy.io/docs/envoy/latest/intro/arch_overview/upstream/circuit_breaking).  Global means there a single counter for all proxies will use as a basis for evaluating the request.  Each proxy asks an upstream Rate Limiting Service (Lyfts, in this example) that would run outside of envoy for a decision on the request.

As this is just a tutorial, we will use a single Envoy instance that talks to Lyft's Rate limiting gRPC service that also runs on the same machine.  Realistically, you probably want set this up to run within a kubernetes cluster with envoy as a sidecar and the rate limiting service running by itself.


### Testing

Lets go through the criteria we seutp earlier;

#### 1. Anonymous access

For the stage0 anonymous users:  we can achieve that by applying `header_value_match` [RateLimit.Action](https://www.envoyproxy.io/docs/envoy/v1.5.0/api-v2/rds.proto#ratelimit-action) that emits values if the Authorization header _is not found_ (note the ` expect_match: false` setting)

```yaml
              rate_limits:
              - actions:                                 
                - header_value_match:
                    descriptor_value: quote-path-auth
                    expect_match: false
                    headers:
                    - name: ":path"
                      exact_match: "/quote"
                    - name: "Authorization"                      
                stage: 0
```

THe corresponding Lyft configuration that handles the descriptor above is

```yaml
- key: header_match
  value: quote-path-auth
  rate_limit:
    unit: minute
    requests_per_unit: 2
```

For reference, see
 - [composing actions](https://www.envoyproxy.io/docs/envoy/latest/configuration/http/http_filters/rate_limit_filter#config-http-filters-rate-limit-composing-actions)
 - [route.HeaderMatcher](https://www.envoyproxy.io/docs/envoy/latest/api-v2/api/v2/route/route.proto.html?highlight=route#route-headermatcher)
 - [route.RateLimit.Action](https://www.envoyproxy.io/docs/envoy/latest/api-v2/api/v2/route/route.proto.html?highlight=route#route-ratelimit-action)

Which means a descriptor like `("header_match", "quote-path-auth")` gets sent everytime the Authorization header isn't present an gets throttled at 2/min.


To test, in a new window run:

```bash
$ for i in {1..1000}; do curl -s -o /dev/null -w "$i->%{http_code} "  http://localhost:10000/quote; sleep 1; done
  1->200 2->200 **3->429 4->429 5->429**
```

In the window where you are running the rate limit service, you should see the request counters incremented with each request and eventually a report that this vector is over the limit:

```
DEBU[0110] looking up cache key: apis_header_match_quote-path-auth_1571615400
DEBU[0110] cache key: apis_header_match_quote-path-auth_1571615400 current: 1
DEBU[0111] cache key: apis_header_match_quote-path-auth_1571615400 current: 2
DEBU[0112] cache key: apis_header_match_quote-path-auth_1571615400 current: 3
..
DEBU[0120] [gostats] flushing counter ratelimit.service.rate_limit.apis.header_match_quote-path-auth.over_limit: 8
```

#### 2. Authorized access without Service level

The second compounded limiter occurs if the `Authorization:` header is sent in and the counter the rate limiter uses is ofcourse not based on the simple existence of the header (which would be pretty useless) but a counter on every _distinct_ value of that header.  Which means "Authorization: Bearer Alice" has different counter than "Authorization: Bearer Bob".  (ahd yes, this is a contrived example...a JWT based bearer token can be different for the same user and would get counted different... this is just for demonstration.)

The envoy configuration that emits the Authorization header value only when the `/quote` path is invoked is
```yaml
              - actions:                                 
                - header_value_match:
                    descriptor_value: quote-path-user-limit
                    headers:
                    - name: ":path"
                      exact_match: "/quote"
                - request_headers:
                    header_name: "Authorization"
                    descriptor_key: auth_token                                    
                stage: 1  
```

Which is emitted as:

`("header_match", "quote-path-user-limit").("auth_token", "__value_of_authorization_header__"))`

Which matches the descriptor on the rate limiter:

```yaml
- key: header_match
  value: quote-path-user-limit
  descriptors:
  - key: auth_token
    rate_limit:
      unit: minute
      requests_per_unit: 20
```

Each unique value for the authorization header is granted its own counter since the value itself isn't specified for the `auth_token` within the limiter.

Run the following  and see the requests get denied after 20 or so counts:

```bash
$ for i in {1..1000}; do curl -s -H "Authorization: alice" -o /dev/null -w "$i->%{http_code} "  http://localhost:10000/quote; sleep 1; done
   1->200 2->200 3->200 4->200 5->200 6->200 7->200 8->200 9->200 10->200 11->200 12->200 13->200 14->200 15->200 16->200 17->200 18->200 19->200 20->200 **21->429**

$ for i in {1..1000}; do curl -s -H "Authorization: bob" -o /dev/null -w "$i->%{http_code} "  http://localhost:10000/quote; sleep 1; done
   1->200 2->200 3->200 4->200 5->200 6->200 7->200 8->200 9->200 10->200 11->200 12->200 13->200 14->200 15->200 16->200 17->200 18->200 19->200 20->200 **21->429**
```

Note the cache key counters includes a bit about the value (again, this is a contrived example; don't use the actual Authorization header value!!)
```
DEBU[0193] cache key: apis_header_match_quote-path-user-limit_auth_token_alice_1571618520 current: 1

DEBU[0430] cache key: apis_header_match_quote-path-user-limit_auth_token_bob_1571618760 current: 1
```

#### 3. Authorized access with Service level

The third compound limiter further fine tunes the rate limiter using _two_ header values:  `Authorization` and the `x-service-level` enumberated value.

In the following, we are emitting the `x-service-level` header as value for the `/quote` path.  The `Authorization` header is emitted as well from the previous configuration.  (TODO: find a way to collapse the rules...)
```yaml                                                       
              - actions:
                - header_value_match:
                    descriptor_value: quote-path-vip
                    headers:
                    - name: ":path"
                      exact_match: "/quote"                                       
                - request_headers:
                    header_name: "x-service-level"
                    descriptor_key: service-level                    
                stage: 1
```

Ratelimit

```yaml
- key: header_match
  value: quote-path-vip
  descriptors:
  - key: service-level
    value: "enhanced"  
    rate_limit:
      unit: minute
      requests_per_unit: 15
  - key: service-level
    value: "basic"  
    rate_limit:
      unit: minute
      requests_per_unit: 10
```

What the rules above state is if the client emits an authorized `basic` and `enhanced` requests, the rate limit will hit at 10 and 15 respectively within one minute window (per user as governed by the `quote-path-user-limit` rule)

```bash
$ for i in {1..1000}; do curl -s -H "Authorization: alice" -H "x-service-level: basic" -o /dev/null -w "$i->%{http_code} "  http://localhost:10000/quote; sleep 1; done
  1->200 2->200 3->200 4->200 5->200 6->200 7->200 8->200 9->200 10->200 11->429

$ for i in {1..1000}; do curl -s -H "Authorization: alice" -H "x-service-level: enhanced" -o /dev/null -w "$i->%{http_code} "  http://localhost:10000/quote; sleep 1; done
  1->200 2->200 3->200 4->200 5->200 6->200 7->200 8->200 9->200 10->200 11->200 12->200 13->200 14->200 15->200 16->429 17->429
```

Notice **two** counters are checked: one for the `quote-path-user-limit` and another for `quote-path-vip`.  WHich ever one gets hit first will deny the request.  In our exaple, the `quote-path-vip` was triggered first.

```
DEBU[0018] cache key: apis_header_match_quote-path-user-limit_auth_token_balice_1571619840 current: 1
DEBU[0018] cache key: apis_header_match_quote-path-vip_service-level_basic_1571619840 current: 11

DEBU[0343] cache key: apis_header_match_quote-path-user-limit_auth_token_alice_1571620140 current: 11
DEBU[0343] cache key: apis_header_match_quote-path-vip_service-level_enhanced_1571620140 current: 11
```

To see it the other way, invoke a 'basic' request and cancel it once you see a deny

```bash
$  for i in {1..1000}; do curl -s -H "Authorization: bob" -H "x-service-level: basic" -o /dev/null -w "$i->%{http_code} "  http://localhost:10000/quote; sleep 1; done
1->200 2->200 3->200 4->200 5->200 6->200 7->200 8->200 9->200 10->200 11->429 12->429 ^C
```

Then immediately call the 'enhanced' request.
```bash
$ for i in {1..1000}; do curl -s -H "Authorization: bob" -H "x-service-level: enhanced" -o /dev/null -w "$i->%{http_code} "  http://localhost:10000/quote; sleep 1; done
  1->200 2->200 3->200 4->200 5->200 6->200 7->200 8->200 9->429 10->429 11->429 12->429 13->429 14->429 15->429 16->429
```

Notice that the enhanced request failed on the 9th request or so (well before the 15 we configured).  This is because the `quote-path-user-limit` was evaluated first and that caused the deny:

```
DEBU[0676] cache key: apis_header_match_quote-path-user-limit_auth_token_bob_1571620500 current: 20
DEBU[0676] cache key: apis_header_match_quote-path-vip_service-level_enhanced_1571620500 current: 9
```

### Notes:

* Lyfts Rate limiter is pretty basic: its just a window based limit and lacks more advanced constructs like Token Bucket,etc.  The docs do state they may add on more types as demand increases
* The envoy rate limiting is pretty abstract..it was a bit difficult for me to understand even basic operations...there much i can improve here in this article the more learn.  Please do feel free to suggest improvements if you figure out more advanced usage.


## References
- [Rate-limiting strategies and techniques](https://cloud.google.com/solutions/rate-limiting-strategies-techniques)
- [route.RateLimit](https://www.envoyproxy.io/docs/envoy/latest/api-v2/api/v2/route/route.proto#route-ratelimit)
- [rls.proto](https://github.com/envoyproxy/data-plane-api/blob/master/envoy/service/ratelimit/v2/rls.proto)
- [config.filter.http.rate_limit.v2.RateLimit](https://www.envoyproxy.io/docs/envoy/latest/api-v2/config/filter/http/rate_limit/v2/rate_limit.proto)
- [envoy discovery plane 'hello world'](https://github.com/salrashid123/envoy_control)
