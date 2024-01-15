// Andrew Basil-Jones
// 998263
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TicTacToeServer {
    public static Queue<Player> waitingQueue = new ConcurrentLinkedQueue<>();


    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java TicTacToeServer <ip> <port>");
            return;
        }
        
        String ip = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            ServerSocket serverSocket = new ServerSocket(port, 50, inetAddress);
            Thread queueChecker = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                checkAndStartNewGame();
            }
        });
        queueChecker.start();
            while (true) {
                Socket clientSocket = serverSocket.accept();
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String username = in.readLine().split(" ")[1];
                Player newPlayer = new Player(username, clientSocket, out, in);

                waitingQueue.add(newPlayer);
                out.println("WAITING");
                checkAndStartNewGame();
                }
                
            
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void handleGame(Socket player1Socket, Socket player2Socket, String username1, String username2, Player player1, Player player2) {
        try {
            System.out.println("Game Found");
            PrintWriter out1 = new PrintWriter(player1Socket.getOutputStream(), true);
            BufferedReader in1 = new BufferedReader(new InputStreamReader(player1Socket.getInputStream()));
            PrintWriter out2 = new PrintWriter(player2Socket.getOutputStream(), true);
            BufferedReader in2 = new BufferedReader(new InputStreamReader(player2Socket.getInputStream()));
            
            out1.println("START");
            out2.println("START");
            out1.println("PLAYER 1 " + username1 + " " + username2);
            out2.println("PLAYER 2 " + username2 + " " + username1);
            
            Game game = new Game();
            
            Thread player1Handler = new Thread(() -> handlePlayer(in1, out1, out2, username1, game, "X", player1));
            Thread player2Handler = new Thread(() -> handlePlayer(in2, out2, out1, username2, game, "O", player2));
            player1Handler.start();
            player2Handler.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void handlePlayer(BufferedReader in, PrintWriter out, PrintWriter outOpponent, String username, Game game, String piece, Player player) {
        try {
            while (true) {
                String move = in.readLine();
                if (move.startsWith("DISCONNECT")){
                    outOpponent.println("WINNER " + (piece.equals("X") ? "O" : "X"));
                }
                if (move.startsWith("MOVE")) {
                    if (game.isPlayer1Turn() && piece.equals("X") || !game.isPlayer1Turn() && piece.equals("O")) {
                        int index = Integer.parseInt(move.split(" ")[1]);
                        if (game.makeMove(index, piece)) {
                            out.println("UPDATE " + index + " " + piece);
                            outOpponent.println("UPDATE " + index + " " + piece);
                            String winner = game.checkWinner();
                            if ("X".equals(winner) || "O".equals(winner)) {
                                out.println("WINNER " + winner);
                                outOpponent.println("WINNER " + winner);
                                if ("NEWGAME".equals(in.readLine())) {
                                    waitingQueue.add(player);
                                    player.out.println("WAITING");
                                    return;
                                }
                            } else if ("DRAW".equals(winner)) {
                                out.println("DRAW");
                                outOpponent.println("DRAW");
                                if ("NEWGAME".equals(in.readLine())) {
                                    waitingQueue.add(player);
                                    player.out.println("WAITING");
                                    return;
                                }
                            }
                            out.println("WAIT");
                            outOpponent.println("YOURTURN");
                        }
                    }
                } else if (move.startsWith("CHAT")) {
                    String msg = move.substring(5);
                    outOpponent.println("CHAT " + username + ": " + msg);
                }
            }
        } catch (SocketException e) {
            player.isConnected = false;
            outOpponent.println("WINNER " + (piece.equals("X") ? "O" : "X"));
        } catch (IOException e) {
            e.printStackTrace();
            player.isConnected = false;
            outOpponent.println("WINNER " + (piece.equals("X") ? "O" : "X"));
        }
    }
    
    public static class Game {
        private String[] board = new String[9];
        private boolean player1Turn = true;
    
        public synchronized boolean makeMove(int index, String player) {
            if (board[index] == null) {
                board[index] = player;
                player1Turn = !player1Turn;
                return true;
            }
            return false;
        }
    
        public synchronized boolean isPlayer1Turn() {
            return player1Turn;
        }

        public synchronized String checkWinner() {
            for (int i = 0; i < 9; i += 3) {
                if (board[i] != null && board[i].equals(board[i + 1]) && board[i].equals(board[i + 2])) {
                    return board[i];
                }
            }
            for (int i = 0; i < 3; i++) {
                if (board[i] != null && board[i].equals(board[i + 3]) && board[i].equals(board[i + 6])) {
                    return board[i];
                }
            }
            if (board[0] != null && board[0].equals(board[4]) && board[0].equals(board[8])) {
                return board[0];
            }
            if (board[2] != null && board[2].equals(board[4]) && board[2].equals(board[6])) {
                return board[2];
            }
            if (Arrays.stream(board).allMatch(s -> s != null)) {
                return "DRAW";
            }
        
            return null;        
        }
        
    }
    public static class Player extends Thread {
        String username;
        Socket socket;
        PrintWriter out;
        BufferedReader in;
        public boolean isConnected = true;

        public Player(String username, Socket socket, PrintWriter out, BufferedReader in) {
            this.username = username;
            this.socket = socket;
            this.out = out;
            this.in = in;
        }
    }
    public static void startNewGame(Player player1, Player player2) {
        Thread gameThread = new Thread(() -> {
            String username1 = player1.username;
            String username2 = player2.username;
            handleGame(player1.socket, player2.socket, username1, username2, player1, player2);
        });
        gameThread.start();
    }
    public static void checkAndStartNewGame() {
        synchronized(waitingQueue) {
            waitingQueue.removeIf(player -> !player.isConnected);
            if (waitingQueue.size() >= 2) {
                System.out.println("Waiting Q > 2");
                Player player1 = waitingQueue.poll();
                Player player2 = waitingQueue.poll();
    
                if (player1 != null && player2 != null) {
                    startNewGame(player1, player2);
                }
            }
        }
    }
    
    
       
}