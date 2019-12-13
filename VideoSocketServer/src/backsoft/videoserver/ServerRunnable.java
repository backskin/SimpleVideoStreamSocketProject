package backsoft.videoserver;

import backsoft.utils.AlertHandler;
import backsoft.utils.FileHandler;
import backsoft.utils.Loader;
import backsoft.utils.Streamer;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static backsoft.utils.CommonPhrases.*;
import static backsoft.utils.CommonPhrases.SIGNAL.*;

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

        private void handleVideoStreamReceive() throws IOException {

            controller.writeToConsole("Клиент "
                    + clientSocket.getRemoteSocketAddress()
                    + " передаёт видео...");


        }

        private void handleImageReceive() throws IOException {
            controller.writeToConsole("Клиент "
                    + clientSocket.getRemoteSocketAddress()
                    + " передаёт изображение...");

            String filename = in.readUTF();
            String hash = in.readUTF();
            int imageHASH = Integer.parseInt(hash);
            controller.visualiseChunksField(true);
            byte[] bytes = Streamer.readBytesFromBase64(imageSignal.get(STOP), in, controller.chunksProperty());

            controller.writeToConsole("От клиента "
                    + clientSocket.getRemoteSocketAddress()
                    + " получено изображение - " + filename);

            if (Arrays.hashCode(bytes) == imageHASH){

                out.writeUTF(imageSignal.get(CORRECT));
                out.flush();
                controller.writeToConsole("Без потерь!");
            } else {
                out.writeUTF(imageSignal.get(MISTAKE));
                out.flush();
                controller.writeToConsole("У нас потери! (хэши разные)");
            }

            FileHandler.saveToFile("temp" + File.separator + filename, bytes);
            BufferedImage bImage = Loader.convertToBuffImage(bytes);

            if (bImage != null) controller.showImage(filename, bImage, bytes);
            controller.visualiseChunksField(false);
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
                    if (command.equals(imageSignal.get(START))) handleImageReceive();
                    if (command.equals(videoSignal.get(START))) handleVideoStreamReceive();
                    if (command.equals(system.get(BYEBYE))) handleQuit();
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
                   out.writeUTF(system.get(BYEBYE));
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
