package com.test.grpc;

import java.io.IOException;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.util.Durations;

import io.envoyproxy.controlplane.cache.NodeGroup;
import io.envoyproxy.controlplane.cache.v2.SimpleCache;
import io.envoyproxy.controlplane.cache.v2.Snapshot;
import io.envoyproxy.controlplane.server.V2DiscoveryServer;
import io.envoyproxy.controlplane.server.V3DiscoveryServer;
import io.envoyproxy.envoy.api.v2.Cluster;
import io.envoyproxy.envoy.api.v2.Cluster.DiscoveryType;
import io.envoyproxy.envoy.api.v2.ClusterLoadAssignment;
import io.envoyproxy.envoy.api.v2.RouteConfiguration;
import io.envoyproxy.envoy.api.v2.core.Address;
import io.envoyproxy.envoy.api.v2.core.Node;
import io.envoyproxy.envoy.api.v2.core.SocketAddress;
import io.envoyproxy.envoy.api.v2.core.SocketAddress.Protocol;
import io.envoyproxy.envoy.api.v2.endpoint.Endpoint;
import io.envoyproxy.envoy.api.v2.endpoint.LbEndpoint;
import io.envoyproxy.envoy.api.v2.endpoint.LocalityLbEndpoints;
import io.envoyproxy.envoy.api.v2.route.Route;
import io.envoyproxy.envoy.api.v2.route.RouteAction;
import io.envoyproxy.envoy.api.v2.route.RouteMatch;
import io.envoyproxy.envoy.api.v2.route.VirtualHost;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

@Component
public class CommandLineAppStartupRunner implements CommandLineRunner {

	private static final String GROUP = "key";

	private static final String XDS_CLUSTER = "xds_cluster";
	static final String FILTER_ENVOY_ROUTER = "envoy.router";
	static final String FILTER_HTTP_CONNECTION_MANAGER = "envoy.http_connection_manager";
	private static final String ANY_ADDRESS = "0.0.0.0";

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

		Snapshot.create(ImmutableList.of(createCluster("some_service", "127.0.0.1", 8000)), ImmutableList.of(),
				ImmutableList.of(), ImmutableList.of(getRouteConfigurationBuild("local_route", "some_service", "*", "/")),
				ImmutableList.of(), "1");

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

	public Cluster createCluster(String clusterName, String address, int port) {
		return Cluster.newBuilder().setName(clusterName).setConnectTimeout(Durations.fromSeconds(5))
				.setType(DiscoveryType.STATIC)
				.setLoadAssignment(ClusterLoadAssignment.newBuilder().setClusterName(clusterName)
						.addEndpoints(LocalityLbEndpoints.newBuilder()
								.addLbEndpoints(LbEndpoint.newBuilder()
										.setEndpoint(Endpoint.newBuilder().setAddress(Address.newBuilder()
												.setSocketAddress(SocketAddress.newBuilder().setAddress(address)
														.setPortValue(port).setProtocolValue(Protocol.TCP_VALUE)))))))
				.build();
	}

	private RouteConfiguration getRouteConfigurationBuild(String routeName, String clusterName0, String domain,
			String route_prefix) {
		return RouteConfiguration.newBuilder().setName(routeName)
				.addVirtualHosts(VirtualHost.newBuilder().setName(clusterName0).addDomains(domain)
						.addRoutes(Route.newBuilder().setMatch(RouteMatch.newBuilder().setPrefix(route_prefix).build())
								.setRoute(RouteAction.newBuilder().setCluster(clusterName0).build()).build())
						.addRoutes(Route.newBuilder().setMatch(RouteMatch.newBuilder().setPrefix("/").build())
								.setRoute(RouteAction.newBuilder().setCluster("some_service").build()).build())
						.build())
				.build();
	}
}
