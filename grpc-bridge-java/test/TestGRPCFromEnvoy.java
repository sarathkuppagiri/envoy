package test;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import br.com.rformagio.grpc.server.grpcserver.AddressRequest;

public class TestGRPCFromEnvoy {

	public static void main(String[] args) {

		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.set("content-Type", "application/grpc");
		headers.set("Host", "grpc");
		AddressRequest addressRequest = AddressRequest.newBuilder().setCep("13960000").build();
		byte[] b = serializeGRPCRequest(addressRequest);
		System.out.println(b);

		HttpEntity<byte[]> entity = new HttpEntity<>(b, headers);
		ResponseEntity<?> rs = restTemplate.exchange(
				"http://localhost:9911/br.com.rformagio.grpc.server.grpcserver.CepService/getAddress", HttpMethod.POST,
				entity, String.class);
		System.out.println(rs.getBody());
		System.out.println(rs.getHeaders());

	}

	public static byte[] serializeGRPCRequest(AddressRequest addressRequest) {

		byte[] bytes = addressRequest.toByteArray();
		byte[] length = intToNetworkByteOrder(bytes.length);
		byte[] data = new byte[bytes.length + 5];
		data[0] = (byte) '\0';
		System.arraycopy(length, 0, data, 1, length.length);
		System.arraycopy(bytes, 0, data, 5, bytes.length);
		return data;
	}

	public static byte[] intToNetworkByteOrder(int num) {
		byte[] buf = new byte[4];
		for (int i = 3; i >= 0; i--) {
			buf[i] = (byte) (num & 0xff);
			num >>>= 8;
		}
		return buf;
	}

}
