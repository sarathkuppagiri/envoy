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
import io.envoyproxy.envoy.config.core.v3.Address;
import io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.endpoint.v3.Endpoint;
import io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

@Component
public class CommandLineAppStartupRunner implements CommandLineRunner {

	private static final String GROUP = "key";

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

		cache.setSnapshot(
		        GROUP,
		        Snapshot.create(
		            ImmutableList.of(),
		            ImmutableList.of(createEndpoint("some_service", 8000)),
		            ImmutableList.of(),
		            ImmutableList.of(),
		            ImmutableList.of(),
		            "1"));

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
		
		Thread.sleep(1000);
		
		cache.setSnapshot(
		        GROUP,
		        Snapshot.create(
		            ImmutableList.of(),
		            ImmutableList.of(createEndpoint("some_service", 1234)),
		            ImmutableList.of(),
		            ImmutableList.of(),
		            ImmutableList.of(),
		            "1"));

		server.awaitTermination();

	}

	public ClusterLoadAssignment createEndpoint(String clusterName, int port) {
		return ClusterLoadAssignment.newBuilder().setClusterName(clusterName)
				.addEndpoints(
						LocalityLbEndpoints.newBuilder()
								.addLbEndpoints(LbEndpoint.newBuilder()
										.setEndpoint(Endpoint.newBuilder().setAddress(Address.newBuilder()
												.setSocketAddress(SocketAddress.newBuilder().setAddress("127.0.0.1")
														.setPortValue(port).setProtocol(SocketAddress.Protocol.TCP))))))
				.build();
	}

}
