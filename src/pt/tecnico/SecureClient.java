package pt.tecnico;

import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.*;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SecureClient {

	/** Buffer size for receiving a UDP packet. */
	private static final int BUFFER_SIZE = 65_507;

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
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
		final String to = "Bob"
		final String payload = "Hello." + System.lineSeparator() + "Do you want to meet tomorrow?";

		// Digest payload
		MessageDigest hash = MessageDigest.getInstance("SHA-256");
		byte[] digest = hash.digest(payload.getBytes());
		// Encrypt digest
		RSACipher cipher = new RSACipher();
		byte[] sign = cipher.encrypt(digest, "keys/alice.privkey", Cipher.PRIVATE_KEY);

		// Create socket
		DatagramSocket socket = new DatagramSocket();

        // Create request message
		JsonObject requestJson = JsonParser.parseString​("{}").getAsJsonObject();
		{
			JsonObject infoJson = JsonParser.parseString​("{}").getAsJsonObject();
			infoJson.addProperty("from", from);
			infoJson.addProperty("to", to);
			infoJson.addProperty("sign", sign);
			requestJson.add("info", infoJson);
			requestJson.addProperty("body", payload);
		}
		System.out.println("Request message: " + requestJson);
		
		// Send request		
		byte[] clientData = requestJson.toString().getBytes();
		System.out.printf("%d bytes %n", clientData.length);
		DatagramPacket clientPacket = new DatagramPacket(clientData, clientData.length, serverAddress, serverPort);
		socket.send(clientPacket);
		System.out.printf("Request packet sent to %s:%d!%n", serverAddress, serverPort);
		oos.close();

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
		JsonObject responseJson = JsonParser.parseString​(serverText).getAsJsonObject();
		String from = null, to = null, body = null;
		{
			JsonObject infoJson = responseJson.getAsJsonObject("info");
			from = infoJson.get("from").getAsString();
			to = infoJson.get("to").getAsString();
			body = responseJson.get("body").getAsString();
		}
		System.out.printf("Message from '%s':%n%s%n", from, body);

		// Close socket
		socket.close();
		System.out.println("Socket closed");
	}

}
