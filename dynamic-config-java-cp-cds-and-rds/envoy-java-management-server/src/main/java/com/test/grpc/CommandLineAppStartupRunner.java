package com.test.grpc;

import java.io.IOException;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.util.Durations;

import io.envoyproxy.controlplane.cache.NodeGroup;
import io.envoyproxy.controlplane.cache.v3.SimpleCache;
import io.envoyproxy.controlplane.cache.v3.Snapshot;
import io.envoyproxy.controlplane.server.V2DiscoveryServer;
import io.envoyproxy.controlplane.server.V3DiscoveryServer;
import io.envoyproxy.envoy.api.v2.core.Node;
import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.envoyproxy.envoy.config.core.v3.ApiVersion;
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

@Component
public class CommandLineAppStartupRunner implements CommandLineRunner {

	private static final String GROUP = "key";

	private static final String XDS_CLUSTER = "xds_cluster";

	static final String FILTER_ENVOY_ROUTER = "envoy.router";

	private static final String ANY_ADDRESS = "0.0.0.0";

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

		cache.setSnapshot(GROUP, createSnapshot(true, "some_service", "127.0.0.1", 8000, "local_route", "1"));

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

	static Snapshot createSnapshot(boolean ads, String clusterName, String endpointAddress, int endpointPort,
			String routeName, String version) {

		Cluster cluster = createClusterV3(clusterName);
		ClusterLoadAssignment endpoint = createEndpointV3(clusterName, endpointAddress, endpointPort);
		RouteConfiguration route = createRouteV3(routeName, clusterName);

		return Snapshot.create(ImmutableList.of(cluster), ImmutableList.of(endpoint), ImmutableList.of(),
				ImmutableList.of(route), ImmutableList.of(), version);
	}

	public static io.envoyproxy.envoy.config.cluster.v3.Cluster createClusterV3(String clusterName) {
		io.envoyproxy.envoy.config.core.v3.ConfigSource edsSource = io.envoyproxy.envoy.config.core.v3.ConfigSource
				.newBuilder().setAds(io.envoyproxy.envoy.config.core.v3.AggregatedConfigSource.getDefaultInstance())
				.setResourceApiVersion(ApiVersion.V3).build();

		return io.envoyproxy.envoy.config.cluster.v3.Cluster.newBuilder().setName(clusterName)
				.setConnectTimeout(Durations.fromSeconds(5))
				.setEdsClusterConfig(io.envoyproxy.envoy.config.cluster.v3.Cluster.EdsClusterConfig.newBuilder()
						.setEdsConfig(edsSource).setServiceName(clusterName))
				.setType(io.envoyproxy.envoy.config.cluster.v3.Cluster.DiscoveryType.EDS).build();
	}

	public static io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment createEndpointV3(String clusterName,
			String address, int port) {
		return io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment.newBuilder().setClusterName(clusterName)
				.addEndpoints(io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints.newBuilder()
						.addLbEndpoints(io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint.newBuilder()
								.setEndpoint(io.envoyproxy.envoy.config.endpoint.v3.Endpoint.newBuilder()
										.setAddress(io.envoyproxy.envoy.config.core.v3.Address.newBuilder()
												.setSocketAddress(io.envoyproxy.envoy.config.core.v3.SocketAddress
														.newBuilder().setAddress(address).setPortValue(port)
														.setProtocol(
																io.envoyproxy.envoy.config.core.v3.SocketAddress.Protocol.TCP))))))
				.build();
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
