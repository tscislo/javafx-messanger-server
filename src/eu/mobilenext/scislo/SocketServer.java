package eu.mobilenext.scislo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class SocketServer {

    private static final int PORT = 9001;

    private static ConcurrentHashMap<Integer, PrintWriter> clients = new ConcurrentHashMap<Integer, PrintWriter>();

    public static void main(String[] args) throws Exception {

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("The server is running.");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connects...");
                RequestHandler requestHandler = new RequestHandler(socket);
                requestHandler.start();
            }
        }
    }

    private static class RequestHandler extends Thread {
        private int id;
        private String name;
        private Socket socket;
        private BufferedReader inputBufferedReader;
        private PrintWriter outputPrintWriter;

        public RequestHandler(Socket socket) {
            this.socket = socket;
            id = new Random().nextInt(Integer.MAX_VALUE);
        }

        @Override
        public void run() {
            try {
                inputBufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputPrintWriter = new PrintWriter(socket.getOutputStream(), true);
                System.out.println("Sending RDY");
                outputPrintWriter.println("RDY");

                name = inputBufferedReader.readLine();
                clients.putIfAbsent(id, outputPrintWriter);
                System.out.println("Name: " + name);

                outputPrintWriter.println("UID" + String.valueOf(id));
                System.out.println("UID" + String.valueOf(id));

                while (true) {
                    String clientMsg = inputBufferedReader.readLine();
                    if (clientMsg == null) {
                        return;
                    }
                    for (ConcurrentHashMap.Entry<Integer, PrintWriter> entry : clients.entrySet()) {
                        String serverMsg = "MSG" + id + "\t" + name + "\t" + clientMsg;
                        PrintWriter printWriter = entry.getValue();
                        printWriter.println(serverMsg); // wysyłanie komunikatu do klienta
                        System.out.println(serverMsg);
                    }
                }
            } catch (IOException e) {
                System.out.println("Client reset connection");
            } finally {
                clients.remove(id);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
