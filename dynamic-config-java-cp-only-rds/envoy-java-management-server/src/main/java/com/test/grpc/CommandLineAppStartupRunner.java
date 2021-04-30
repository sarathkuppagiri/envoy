package com.test.grpc;

import java.io.IOException;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

import io.envoyproxy.controlplane.cache.NodeGroup;
import io.envoyproxy.controlplane.cache.v3.SimpleCache;
import io.envoyproxy.controlplane.cache.v3.Snapshot;
import io.envoyproxy.controlplane.server.V2DiscoveryServer;
import io.envoyproxy.controlplane.server.V3DiscoveryServer;
import io.envoyproxy.envoy.api.v2.core.Node;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

@Component
public class CommandLineAppStartupRunner implements CommandLineRunner {

	private static final String GROUP = "key";

	static final String FILTER_ENVOY_ROUTER = "envoy.router";

	static final String FILTER_HTTP_CONNECTION_MANAGER = "envoy.filters.network.http_connection_manager";

	@Override
	public void run(String... args) throws Exception {
		startServer();
	}

	private void startServer() throws IOException, InterruptedException {
		SimpleCache<String> cache = new SimpleCache<>(new NodeGroup<String>() {
			@Override
			public String hash(Node node) {
				return GROUP;
			}

			@Override
			public String hash(io.envoyproxy.envoy.config.core.v3.Node node) {
				return GROUP;
			}
		});

		cache.setSnapshot(GROUP, createSnapshot(true, "some_service", "local_route", "1"));

		V2DiscoveryServer discoveryServer = new V2DiscoveryServer(cache);
		V3DiscoveryServer v3DiscoveryServer = new V3DiscoveryServer(cache);

		ServerBuilder builder = NettyServerBuilder.forPort(12345)
				.addService(discoveryServer.getAggregatedDiscoveryServiceImpl())
				.addService(discoveryServer.getClusterDiscoveryServiceImpl())
				.addService(discoveryServer.getEndpointDiscoveryServiceImpl())
				.addService(discoveryServer.getListenerDiscoveryServiceImpl())
				.addService(discoveryServer.getRouteDiscoveryServiceImpl())
				.addService(v3DiscoveryServer.getAggregatedDiscoveryServiceImpl())
				.addService(v3DiscoveryServer.getClusterDiscoveryServiceImpl())
				.addService(v3DiscoveryServer.getEndpointDiscoveryServiceImpl())
				.addService(v3DiscoveryServer.getListenerDiscoveryServiceImpl())
				.addService(v3DiscoveryServer.getRouteDiscoveryServiceImpl());

		Server server = builder.build();

		server.start();

		System.out.println("Server has started on port " + server.getPort());

		Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

		server.awaitTermination();

	}

	static Snapshot createSnapshot(boolean ads, String clusterName, String routeName, String version) {

		RouteConfiguration route = createRouteV3(routeName, clusterName);

		return Snapshot.create(ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), ImmutableList.of(route),
				ImmutableList.of(), version);
	}

	public static io.envoyproxy.envoy.config.route.v3.RouteConfiguration createRouteV3(String routeName,
			String clusterName) {
		return io.envoyproxy.envoy.config.route.v3.RouteConfiguration.newBuilder().setName(routeName)
				.addVirtualHosts(io.envoyproxy.envoy.config.route.v3.VirtualHost.newBuilder().setName("all")
						.addDomains("*")
						.addRoutes(io.envoyproxy.envoy.config.route.v3.Route.newBuilder()
								.setMatch(io.envoyproxy.envoy.config.route.v3.RouteMatch.newBuilder().setPrefix("/"))
								.setRoute(io.envoyproxy.envoy.config.route.v3.RouteAction.newBuilder()
										.setCluster(clusterName))))
				.build();
	}

}
