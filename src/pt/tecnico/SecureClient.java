package pt.tecnico;

import java.net.*;
import java.security.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import com.google.gson.*;

import javax.crypto.Cipher;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

public class SecureClient {

	/** Buffer size for receiving a UDP packet. */
	private static final int BUFFER_SIZE = 65_507;

	public static void main(String[] args) throws Exception {
		// Check arguments
		if (args.length < 2) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s host port%n", SecureClient.class.getName());
			return;
		}
		final String serverHost = args[0];
		final InetAddress serverAddress = InetAddress.getByName(serverHost);
		final int serverPort = Integer.parseInt(args[1]);

		// Message content
		final String from = "Alice";
		final String to = "Bob";
		final String payload = "Hello." + System.lineSeparator() + "Do you want to meet tomorrow?";
		final String ts = Instant.now().toEpochMilli() + "";

		// Digest payload
		MessageDigest hash = MessageDigest.getInstance("SHA-256");
		byte[] digest = hash.digest(payload.getBytes());

		// Encrypt digest
		RSACipher cipher = new RSACipher();
		byte[] sign = cipher.encrypt(digest, "keys/alice.privkey", Cipher.PRIVATE_KEY);

		// Digest and encrypt timestamp
		byte[] digestTS = hash.digest(ts.getBytes());
		byte[] signTS = cipher.encrypt(digestTS, "keys/alice.privkey", Cipher.PRIVATE_KEY);

		// Create socket
		DatagramSocket socket = new DatagramSocket();

        // Create request message
		JsonObject requestJson = JsonParser.parseString("{}").getAsJsonObject();
		{
			JsonObject infoJson = JsonParser.parseString("{}").getAsJsonObject();
			infoJson.addProperty("from", from);
			infoJson.addProperty("to", to);
			infoJson.addProperty("sign", Base64.getEncoder().encodeToString(sign));
			infoJson.addProperty("ts", ts);
			infoJson.addProperty("signTS", Base64.getEncoder().encodeToString(signTS));
			requestJson.add("info", infoJson);
			requestJson.addProperty("body", payload);
		}
		//System.out.println("Request message: " + requestJson);

		// Test replay attack
		//TimeUnit.SECONDS.sleep(1);
		
		// Send request		
		byte[] clientData = requestJson.toString().getBytes();
		System.out.printf("%d bytes %n", clientData.length);
		DatagramPacket clientPacket = new DatagramPacket(clientData, clientData.length, serverAddress, serverPort);
		socket.send(clientPacket);
		System.out.printf("Request packet sent to %s:%d!%n", serverAddress, serverPort);

		// Receive response
		byte[] serverData = new byte[BUFFER_SIZE];
		DatagramPacket serverPacket = new DatagramPacket(serverData, serverData.length);
		System.out.println("Wait for response packet...");
		socket.receive(serverPacket);
		System.out.printf("Received packet from %s:%d!%n", serverPacket.getAddress(), serverPacket.getPort());
		System.out.printf("%d bytes %n", serverPacket.getLength());

		// Convert response to string
		String serverText = new String(serverPacket.getData(), 0, serverPacket.getLength());
		System.out.println("Received response: " + serverText);

		// Parse JSON and extract arguments
		JsonObject responseJson = JsonParser.parseString(serverText).getAsJsonObject();

		JsonObject infoJson = responseJson.getAsJsonObject("info");
		String fromRec = infoJson.get("from").getAsString();
		String toRec = infoJson.get("to").getAsString();
		String signRec = infoJson.get("sign").getAsString();
		String tsRec = infoJson.get("ts").getAsString();
		String signTSRec = infoJson.get("signTS").getAsString();
		String bodyRec = responseJson.get("body").getAsString();

		// Digest and decrypt timestamp
		byte[] digestTS2 = hash.digest(tsRec.getBytes());
		byte[] digestTSRec = cipher.decrypt(
				Base64.getDecoder().decode(signTSRec),
				"keys/bob.pubkey",
				Cipher.PUBLIC_KEY
		);

		// Verify TS Integrity
		if (!Arrays.equals(digestTS2, digestTSRec)) {
			throw new Exception("Jesus Trudy get a life!");
		}

		// Verify Freshness
		if (Instant.now().toEpochMilli() - Long.parseLong(tsRec) > 150) {
			throw new Exception("Not Fresh! Replay attack from Trudy!");
		}

		// Digest body
		byte[] digest2 = hash.digest(bodyRec.getBytes());

		// Decrypt Received signature
		byte[] digestRec = cipher.decrypt(
				Base64.getDecoder().decode(signRec),
				"keys/bob.pubkey",
				Cipher.PUBLIC_KEY
		);

		// Verify Integrity
		if (!Arrays.equals(digest2, digestRec)) {
			throw new Exception("Jesus Trudy get a life!");
		}

		System.out.printf("Message from '%s' to '%s':%n%s%n", fromRec, toRec, bodyRec);

		// Close socket
		socket.close();
		System.out.println("Socket closed");
	}

}
