package services;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

public class RequestClientService {
    
    public RequestResult sendRequest(String entry) throws IOException {
        try (
            Socket socket = new Socket("localhost", 9806);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        ) {
            // Send the entry to the server
            outputStream.writeUTF(entry);

            // Wait for the server's response
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            String ack = inreadUTF();
            String decision = in.readUTF();

            return new RequestResult(ack, decision);
        } catch (ConnectException e) {
            throw new IOException("Unable to connect to the server. Please ensure the server is running.", e);
        }
    }

    public static class RequestResult {
        private final String ack;
        private final String decision;

        public RequestResult(String ack, String decision) {
            this.ack = ack;
            this.decision = decision;
        }

        public String getAck() {
            return ack;
        }

        public String getDecision() {
            return decision;
        }
    }
}
