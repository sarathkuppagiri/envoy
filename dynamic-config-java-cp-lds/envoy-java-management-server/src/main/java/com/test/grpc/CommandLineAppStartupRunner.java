package com.test.grpc;

import static io.envoyproxy.envoy.config.core.v3.ApiVersion.V3;

import java.io.IOException;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;

import io.envoyproxy.controlplane.cache.NodeGroup;
import io.envoyproxy.controlplane.cache.v3.SimpleCache;
import io.envoyproxy.controlplane.cache.v3.Snapshot;
import io.envoyproxy.controlplane.server.V2DiscoveryServer;
import io.envoyproxy.controlplane.server.V3DiscoveryServer;
import io.envoyproxy.envoy.api.v2.core.Node;
import io.envoyproxy.envoy.config.core.v3.ApiVersion;
import io.envoyproxy.envoy.config.listener.v3.Listener;
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

		cache.setSnapshot(GROUP, createSnapshot(true, "some_service", "sample-listener", 10000, "local_route", "1"));

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

	static Snapshot createSnapshot(boolean ads, String clusterName, String listenerName, int listenerPort,
			String routeName, String version) {

		Listener listener = createListenerV3(ads, V3, V3, listenerName, listenerPort, routeName);
		RouteConfiguration route = createRouteV3(routeName, clusterName);

		return Snapshot.create(ImmutableList.of(), ImmutableList.of(), ImmutableList.of(listener),
				ImmutableList.of(route), ImmutableList.of(), version);
	}

	public static io.envoyproxy.envoy.config.listener.v3.Listener createListenerV3(boolean ads,
			ApiVersion rdsTransportVersion, ApiVersion rdsResourceVersion, String listenerName, int port,
			String routeName) {
		io.envoyproxy.envoy.config.core.v3.ConfigSource.Builder configSourceBuilder = io.envoyproxy.envoy.config.core.v3.ConfigSource
				.newBuilder().setResourceApiVersion(rdsResourceVersion);
		io.envoyproxy.envoy.config.core.v3.ConfigSource rdsSource = ads
				? configSourceBuilder
						.setAds(io.envoyproxy.envoy.config.core.v3.AggregatedConfigSource.getDefaultInstance())
						.setResourceApiVersion(rdsResourceVersion).build()
				: configSourceBuilder.setApiConfigSource(io.envoyproxy.envoy.config.core.v3.ApiConfigSource.newBuilder()
						.setTransportApiVersion(rdsTransportVersion)
						.setApiType(io.envoyproxy.envoy.config.core.v3.ApiConfigSource.ApiType.GRPC)
						.addGrpcServices(io.envoyproxy.envoy.config.core.v3.GrpcService.newBuilder()
								.setEnvoyGrpc(io.envoyproxy.envoy.config.core.v3.GrpcService.EnvoyGrpc.newBuilder()
										.setClusterName(XDS_CLUSTER))))
						.build();

		io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager manager = io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
				.newBuilder()
				.setCodecType(
						io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager.CodecType.AUTO)
				.setStatPrefix("http")
				.setRds(io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.Rds.newBuilder()
						.setConfigSource(rdsSource).setRouteConfigName(routeName))
				.addHttpFilters(io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter
						.newBuilder().setName(FILTER_ENVOY_ROUTER))
				.build();

		return io.envoyproxy.envoy.config.listener.v3.Listener.newBuilder().setName(listenerName)
				.setAddress(io.envoyproxy.envoy.config.core.v3.Address.newBuilder()
						.setSocketAddress(io.envoyproxy.envoy.config.core.v3.SocketAddress.newBuilder()
								.setAddress(ANY_ADDRESS).setPortValue(port)
								.setProtocol(io.envoyproxy.envoy.config.core.v3.SocketAddress.Protocol.TCP)))
				.addFilterChains(
						io.envoyproxy.envoy.config.listener.v3.FilterChain.newBuilder()
								.addFilters(io.envoyproxy.envoy.config.listener.v3.Filter.newBuilder()
										.setName(FILTER_HTTP_CONNECTION_MANAGER).setTypedConfig(Any.pack(manager))))
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
