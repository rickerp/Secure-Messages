package pt.tecnico;

import java.net.*;
import java.security.MessageDigest;
import java.util.*;

import com.google.gson.*;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;


public class SecureServer {

	/**
	 * Maximum size for a UDP packet. The field size sets a theoretical limit of
	 * 65,535 bytes (8 byte header + 65,527 bytes of data) for a UDP datagram.
	 * However the actual limit for the data length, which is imposed by the IPv4
	 * protocol, is 65,507 bytes (65,535 − 8 byte UDP header − 20 byte IP header.
	 */
	private static final int MAX_UDP_DATA_SIZE = (64 * 1024 - 1) - 8 - 20;

	/** Buffer size for receiving a UDP packet. */
	private static final int BUFFER_SIZE = MAX_UDP_DATA_SIZE;

	public static void main(String[] args) throws Exception {
		// Check arguments
		if (args.length < 1) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", JsonServer.class.getName());
			return;
		}
		final int port = Integer.parseInt(args[0]);

		// Create server socket
		DatagramSocket socket = new DatagramSocket(port);
		System.out.printf("Server will receive packets on port %d %n", port);

		// Wait for client packets
		byte[] buf = new byte[BUFFER_SIZE];
		while (true) {
			// Receive packet
			DatagramPacket clientPacket = new DatagramPacket(buf, buf.length);
			socket.receive(clientPacket);
			InetAddress clientAddress = clientPacket.getAddress();
			int clientPort = clientPacket.getPort();
			int clientLength = clientPacket.getLength();
			byte[] clientData = clientPacket.getData();
			System.out.printf("Received request packet from %s:%d!%n", clientAddress, clientPort);
			System.out.printf("%d bytes %n", clientLength);

			// Convert request to string
			String clientText = new String(clientData, 0, clientLength);
			System.out.println("Received request: " + clientText);

			// Parse JSON and extract arguments
			JsonObject requestJson = JsonParser.parseString(clientText).getAsJsonObject();
			String fromRec = null, toRec = null, signRec = null, bodyRec = null;
			{
				System.out.println(requestJson);
				JsonObject infoJson = requestJson.getAsJsonObject("info");
				System.out.println(infoJson == null);
				fromRec = infoJson.get("from").getAsString();
				toRec = infoJson.get("to").getAsString();
				signRec = infoJson.get("sign").getAsString();
				bodyRec = requestJson.get("body").getAsString();
			}
			System.out.printf("Message from '%s' to '%s':%n%s%n", fromRec, toRec, bodyRec);

			// Digest body
			MessageDigest hash = MessageDigest.getInstance("SHA-256");
			byte[] digest = hash.digest(bodyRec.getBytes());

			// Decrypt Received signature
			RSACipher cipher = new RSACipher();
			System.out.println(Base64.getDecoder().decode(signRec).length + " : " + Base64.getDecoder().decode(signRec));
			byte[] digestRec = cipher.decrypt(Base64.getDecoder().decode(signRec), "keys/alice.pubkey", Cipher.PUBLIC_KEY);

			// Verify Integrity
			if (!Arrays.equals(digest, digestRec)) {
				throw new Exception("Fuck you Trudy!");
			}

			// Message content
			final String from = "Bob";
			final String to = "Alice";
			final String payload = "Yes. See you tomorrow!";

			// Digest payload
			MessageDigest hashRes = MessageDigest.getInstance("SHA-256");
			byte[] digestRes = hashRes.digest(payload.getBytes());

			// Encrypt digest
			byte[] sign = cipher.encrypt(digestRes, "keys/bob.privkey", Cipher.PRIVATE_KEY);

			// Create response message
			JsonObject responseJson = JsonParser.parseString("{}").getAsJsonObject();
			{
				JsonObject infoJson = JsonParser.parseString("{}").getAsJsonObject();
				infoJson.addProperty("from", from);
				infoJson.addProperty("to", to);
				infoJson.addProperty("sign", Base64.getEncoder().encodeToString(sign));
				responseJson.add("info", infoJson);
				responseJson.addProperty("body", payload);
			}
			System.out.println("Response message: " + responseJson);

			// Send response
			byte[] serverData = responseJson.toString().getBytes();
			System.out.printf("%d bytes %n", serverData.length);

			DatagramPacket serverPacket = new DatagramPacket(serverData, serverData.length, clientPacket.getAddress(), clientPacket.getPort());
			socket.send(serverPacket);
			System.out.printf("Response packet sent to %s:%d!%n", clientPacket.getAddress(), clientPacket.getPort());
		}
	}
}
