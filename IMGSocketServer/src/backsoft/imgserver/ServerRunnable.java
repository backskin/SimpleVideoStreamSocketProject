package backsoft.imgserver;

import backsoft.utils.AlertHandler;
import backsoft.utils.FileHandler;
import backsoft.utils.Loader;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ServerRunnable implements Runnable {

    private int port;
    private Controller controller;
    private ServerSocket serverSocket;
    private ArrayList<Socket> currentSockets = new ArrayList<>();
    private Map<Socket, Thread> currentThreads = new HashMap<>();

    class SocketTask implements Runnable {

        Socket clientSocket;
        DataInputStream in;
        DataOutputStream out;

        SocketTask(Socket client) throws IOException {
            super();
            clientSocket = client;
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
        }

        private void handleImageReceive() throws IOException {
            controller.writeToConsole("Клиент "
                    + clientSocket.getRemoteSocketAddress()
                    + " передаёт изображение...");
            String filename = in.readUTF();

            byte[] bytes = FileHandler.readBytesFromBase64("image-end", in);
            BufferedImage bImage = Loader.convertToBuffImage(bytes);

            controller.writeToConsole("От клиента "
                    + clientSocket.getRemoteSocketAddress()
                    + " получено изображение - " + filename);
            out.writeUTF("gotit");
            out.flush();
            controller.showImage(filename, bImage, bytes);
        }

        private void handleQuit() throws IOException {
            controller.writeToConsole("Клиент "
                    + clientSocket.getRemoteSocketAddress()
                    + " запрашивает выход...");
            currentSockets.remove(clientSocket);
            clientSocket.close();
        }

        @Override
        public void run() {

            while (!clientSocket.isClosed()) {
                try {
                    String command = in.readUTF();
                    if (command.equals("image")) handleImageReceive();
                    if (command.equals("quit")) handleQuit();
                } catch (SocketException | EOFException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        clientSocket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    AlertHandler.makeInfo(
                            "Ошибка при работе с клиентом " +
                                    clientSocket.getRemoteSocketAddress()+
                                    ". Соединение остановлено.",
                            null);
                }
            }
            controller.writeToConsole("Клиент "
                    + clientSocket.getRemoteSocketAddress()
                    + " отсоединился");
        }
    }

    ServerRunnable(int port, Controller controller){
        this.port = port;
        this.controller = controller;
    }

   void stop() {
       try {
           for (Thread th : currentThreads.values()){
               th.interrupt();
           }
           for (Socket sock : currentSockets) {
               if (!sock.isClosed() && sock.isConnected()) {
                   DataOutputStream out = new DataOutputStream(sock.getOutputStream());
                   out.writeUTF("close");
                   controller.writeToConsole("Клиент " + sock.getRemoteSocketAddress() + " отключён");
                   out.flush();
               }
           }
           currentSockets.clear();
           if (serverSocket != null) serverSocket.close();
       } catch (IOException e) {
           AlertHandler.makeError("Сервер не остановлен. Ошибка:\n"
                   +e.getLocalizedMessage(),null);
           e.printStackTrace();
       }
   }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(180000);
            controller.setServerWorking(true);
            controller.writeToConsole("Ожидание подключения новых клиентов...");

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                currentSockets.add(clientSocket);

                if (clientSocket.isConnected()) {
                    currentThreads.put(clientSocket, new Thread(new SocketTask(clientSocket)));
                    currentThreads.get(clientSocket).start();
                    controller.writeToConsole("Клиент "
                            + clientSocket.getRemoteSocketAddress()
                            + " подключился!");
                }
            }
        } catch (SocketException ignored) {
        } catch (SocketTimeoutException te){
            controller.writeToConsole("Время ожидание сервера истекло");
        }
        catch (IOException e) {
            AlertHandler.makeError("Ошибка при работе сервера:\n"+e.getLocalizedMessage(), null);
        }
        controller.setServerWorking(false);
    }
}
