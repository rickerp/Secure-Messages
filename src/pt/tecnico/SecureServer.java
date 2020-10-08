package pt.tecnico;

import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.*;

import javax.crypto.Cipher;
import javax.crypto.SealedObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

	public static void main(String[] args) throws IOException, Exception {
		// Check arguments
		if (args.length < 1) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", SecureServer.class.getName());
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

			ByteArrayInputStream bais = new ByteArrayInputStream(buf);
			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(bais));
			SealedObject encData = null;
			try {
				encData = (SealedObject) ois.readObject();
			} catch (Exception e) {
				System.out.println("DEU MERDA!!!");
			}
			ois.close();
			
			// Decrypt hash from SealedObject
			RSACipher cipher = new RSACipher();
			byte[] digest = (byte[]) cipher.decrypt(encData, "keys/alice.pubkey", Cipher.PUBLIC_KEY);
			System.out.println(digest.length + " : " + digest);

			// Convert request to string
			String clientText = null; 
			System.out.println("Received request: " + clientText);

			// Parse JSON and extract arguments
			JsonObject requestJson = JsonParser.parseString​(clientText).getAsJsonObject();
			String from = null, to = null, body = null;
			{
				JsonObject infoJson = requestJson.getAsJsonObject("info");
				from = infoJson.get("from").getAsString();
				to = infoJson.get("to").getAsString();
				sign = infoJson.get("sign").getAsString();
				body = requestJson.get("body").getAsString();
			}

			// Verify integrity
			MessageDigest hash = MessageDigest.getInstance("SHA-256");
			byte[] digest = hash.digest(body.getBytes());
			
			RSACipher cipher = new RSACipher();
			byte[] signRec = cipher.decrypt(digest, "keys/alice.pubkey", Cipher.PUBLIC_KEY);

			if !Array.equals(sign, signRec) 
				throw new Exception("Fuck you Trudy!");
			

			System.out.printf("Message from '%s':%n%s%n", from, body);

			// Create response message
			JsonObject responseJson = JsonParser.parseString​("{}").getAsJsonObject();
			{
				JsonObject infoJson = JsonParser.parseString​("{}").getAsJsonObject();
				infoJson.addProperty("from", "Bob");
				infoJson.addProperty("to", "Alice");
				responseJson.add("info", infoJson);

				String bodyText = "Yes. See you tomorrow!";
				responseJson.addProperty("body", bodyText);
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
