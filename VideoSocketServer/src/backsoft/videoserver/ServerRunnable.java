package backsoft.videoserver;

import backsoft.utils.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
    private Map<Socket, SocketTask> currentTasks = new HashMap<>();
    int SERVER_TIMEOUT = 6000;

    class SocketTask implements Runnable {

        Socket clientSocket;
        ClientStreamWindow streamWindowController;
        Stage clientWindow;
        DataInputStream in;
        DataOutputStream out;

        SocketTask(Socket client) throws IOException {
            super();
            clientSocket = client;
            clientWindow = new Stage();
            clientWindow.setTitle("Стрим Клиента " + client.getInetAddress());
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            Pair<Parent, ClientStreamWindow> fxml =Loader.loadFXML(
                    ServerRunnable.class.getResource("clientStreamWindow.fxml"));

            streamWindowController = fxml.getTwo();
            Platform.runLater(()->
                    Loader.openInAWindow(
                            clientWindow,
                            new Scene(fxml.getOne()),
                            true));
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
//            controller.visualiseChunksField(true);
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

            if (bImage != null)
                streamWindowController.streamImageView.setImage(
                        SwingFXUtils.toFXImage(bImage, null));

//            if (bImage != null) controller.showImage(filename, bImage, bytes);
            controller.visualiseChunksField(false);
        }

        private void handleQuit() {
            controller.writeToConsole("Клиент "
                    + clientSocket.getRemoteSocketAddress()
                    + " запрашивает выход...");
            removeClient(clientSocket);
            currentSockets.remove(clientSocket);
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
           currentSockets.forEach(this::removeClient);
           for (Socket sock : currentSockets) removeClient(sock);
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
            serverSocket.setSoTimeout(SERVER_TIMEOUT);
            controller.setServerWorking(true);
            controller.writeToConsole("Ожидание подключения новых клиентов...");

            while (!serverSocket.isClosed()) {

                Socket clientSocket = serverSocket.accept();
                addClient(clientSocket);
            }
        } catch (SocketException ignored) {
        } catch (SocketTimeoutException te){
            controller.writeToConsole("Время ожидания сервера ("+ SERVER_TIMEOUT/1000 +" сек) истекло");
        }
        catch (IOException e) {
            AlertHandler.makeError("Ошибка при работе сервера:\n"+e.getLocalizedMessage(), null);
        }
        controller.setServerWorking(false);
    }

    private void addClient(Socket client) throws IOException {

        currentSockets.add(client);
        if (client.isConnected()) {
            currentTasks.put(client, new SocketTask(client));
            serverSocket.setSoTimeout(0);

            currentThreads.put(client, new Thread(currentTasks.get(client)));
            currentThreads.get(client).start();
            controller.writeToConsole("Клиент "
                    + client.getRemoteSocketAddress()
                    + " подключился!");
        }
    }

    private void removeClient(Socket client) {

        try {
            if (!client.isClosed() && client.isConnected()) {
                DataOutputStream out = new DataOutputStream(client.getOutputStream());
                out.writeUTF(system.get(BYEBYE));
                out.flush();
                controller.writeToConsole("Клиент " + client.getRemoteSocketAddress() + " отключён");
                if (currentSockets.isEmpty()) serverSocket.setSoTimeout(SERVER_TIMEOUT);
            }
        }catch (SocketException se){
            AlertHandler.makeError(
                    se.getLocalizedMessage()
                            +"\nПри установке Timeout у ServerSocket", null);
            se.printStackTrace();
        } catch (IOException e){
            AlertHandler.makeError(e.getLocalizedMessage(), null);
        }

        currentTasks.remove(client);
        if (currentThreads.get(client).isAlive() )
            currentThreads.get(client).interrupt();

        currentThreads.remove(client);
    }
}
