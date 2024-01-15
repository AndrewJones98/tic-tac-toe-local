//Andrew Basil-Jones
//998263
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Random;

public class TicTacToeClient {
    private static JButton[] buttons;
    private static String playerSymbol;
    private static boolean myTurn;
    private static JLabel turnLabel;
    private static JLabel countdownLabel;
    private static String receivedUsername;
    private static String opponentUsername;
    private static String opponentSymbol;
    private static LinkedList<String> chatMessages = new LinkedList<>();
    private static JTextArea chatArea;
    private static PrintWriter out;
    private static Timer timer;
    private static int timeLeft = 20;
    private static int playerRank;
    private static int opponentRank;


    public static void main(String[] args) {
        try {
            if (args.length != 3) {
                System.out.println("Usage: java TicTacToeClient <username> <ip> <port>");
                return;
            }

            String username = args[0];
            String ip = args[1];
            int port; 
            
            try {
                port = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. It should be an integer.");
                return;
            }

            Socket socket;
                        try {
                socket = new Socket(ip, port);
            } catch (UnknownHostException e) {
                System.out.println("Invalid IP address.");
                return;
            } catch (IOException e) {
                System.out.println("Could not connect to the server. Make sure the server is running and the port is correct.");
                return;
            }

            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("USERNAME " + username);
            String playerInfo = in.readLine();

            JFrame frame = new JFrame(username + " - Tic-Tac-Toe");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            turnLabel = new JLabel("Finding Player");
            turnLabel.setHorizontalAlignment(SwingConstants.CENTER);
            frame.add(turnLabel, BorderLayout.NORTH);

            JPanel boardPanel = new JPanel(new GridLayout(3, 3));
            buttons = new JButton[9];
            for (int i = 0; i < 9; i++) {
                buttons[i] = new JButton("");
                buttons[i].setFont(new Font("Arial", Font.PLAIN, 50));
                buttons[i].setFocusPainted(false);
                buttons[i].addActionListener(new ButtonClickListener(i));
                boardPanel.add(buttons[i]);
            }
            frame.add(boardPanel, BorderLayout.CENTER);

            chatArea = new JTextArea(10, 20);
            chatArea.setEditable(false);
            JScrollPane scroll = new JScrollPane(chatArea);
            frame.add(scroll, BorderLayout.EAST);

            JTextField chatField = new JTextField();
            chatField.addActionListener(e -> {
                String message = chatField.getText();
                if (message.length() <= 20 && !message.isEmpty()) {
                    out.println("CHAT " + message);
                    addChatMessage(username + ": " + message);
                }
                chatField.setText("");
            });
            frame.add(chatField, BorderLayout.SOUTH);

            JButton quitButton = new JButton("Quit");
            quitButton.addActionListener(e -> {
                try {
                    socket.close();
                    out.println("DISCONNECT");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                System.exit(0);
            });
            JPanel leftPanel = new JPanel(new FlowLayout());
            leftPanel.add(quitButton);
            countdownLabel = new JLabel("20");
            leftPanel.add(countdownLabel);
            frame.add(leftPanel, BorderLayout.WEST);

            frame.setSize(600, 500);
            frame.setVisible(true);

            Thread messageListener = new Thread(() -> {
                try {
                    while (true) {
                        String update = in.readLine();
                        if (update.startsWith("UPDATE")) {
                            String[] parts = update.split(" ");
                            int index = Integer.parseInt(parts[1]);
                            String symbol = parts[2];
                            SwingUtilities.invokeLater(() -> {
                                buttons[index].setText(symbol);
                                buttons[index].setEnabled(false);
                            });
                        } else if ("YOURTURN".equals(update)) {
                            myTurn = true;
                            SwingUtilities.invokeLater(() -> {
                                turnLabel.setText(receivedUsername + "'s turn (" + playerSymbol + ")");
                                startNewTurn();
                            });
                        } else if ("WAIT".equals(update)) {
                            myTurn = false;
                            if (timer != null) {
                                timer.stop();
                            }
                            SwingUtilities.invokeLater(() -> {
                                turnLabel.setText(opponentUsername + "'s turn (" + opponentSymbol + ")");
                            });
                        } else if (update.startsWith("CHAT")) {
                            addChatMessage(update.substring(5));
                        } else if (update.startsWith("WINNER")) {
                            String winnerSymbol = update.split(" ")[1];
                            SwingUtilities.invokeLater(() -> {
                                turnLabel.setText("Winner: " + (winnerSymbol.equals(playerSymbol) ? receivedUsername : opponentUsername));
                                showEndGameDialog(frame, socket, out, in);
                            });
                        } else if ("DRAW".equals(update)) {
                            SwingUtilities.invokeLater(() -> {
                                turnLabel.setText("It's a draw!");
                                showEndGameDialog(frame, socket, out, in);
                            });
                        }
                    }
                } catch (SocketException e) {
                    out.println("DISCONNECT");
                    System.out.println("Connection closed by server.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            messageListener.start();

            turnLabel.setText("Finding Player");

            while ("WAITING".equals(playerInfo)) {
                playerInfo = in.readLine();
            }
            if ("START".equals(playerInfo)) {
                playerInfo = in.readLine();
            }

            String[] parts = playerInfo.split(" ");
            String player = parts[0] + " " + parts[1];
            receivedUsername = parts[2];
            opponentUsername = parts[3];
            playerSymbol = player.equals("PLAYER 1") ? "X" : "O";
            opponentSymbol = "PLAYER 1".equals(player) ? "O" : "X";

            System.out.println("You are " + player + " as " + receivedUsername);

            myTurn = "PLAYER 1".equals(player);
            SwingUtilities.invokeLater(() -> {
                turnLabel.setText(myTurn ? receivedUsername + "'s turn (" + playerSymbol + ")" : opponentUsername + "'s turn (" + opponentSymbol + ")");
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void showEndGameDialog(JFrame frame, Socket socket, PrintWriter out, BufferedReader in) {
        Object[] options = {"Find New Match", "Quit"};
        int n = JOptionPane.showOptionDialog(frame,
                "Would you like to find a new match or quit?",
                "Game Over",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (n == 0) {
            try {
                out.println("NEWGAME");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
    }

    private static void startNewTurn() {
        timeLeft = 20;
        countdownLabel.setText("20");

        if (timer != null) {
            timer.stop();
        }

        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                countdownLabel.setText(String.valueOf(timeLeft));
                if (timeLeft <= 0) {
                    timer.stop();
                    makeRandomMove();
                }
                timeLeft--;
            }
        });

        timer.start();
    }

    private static void makeRandomMove() {
        int randomIndex = findRandomEmptyCell();
        if (randomIndex >= 0) {
            buttons[randomIndex].doClick();
        }
    }

    private static int findRandomEmptyCell() {
        Random random = new Random();
        int[] availableIndices = new int[9];
        int counter = 0;
        for (int i = 0; i < 9; i++) {
            if (buttons[i].getText().isEmpty()) {
                availableIndices[counter++] = i;
            }
        }
        return counter == 0 ? -1 : availableIndices[random.nextInt(counter)];
    }

    private static void addChatMessage(String message) {
        chatMessages.add(message);
        if (chatMessages.size() > 10) {
            chatMessages.removeFirst();
        }
        chatArea.setText(String.join("\n", chatMessages));
    }

    private static class ButtonClickListener implements ActionListener {
        int index;

        public ButtonClickListener(int index) {
            this.index = index;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (myTurn && buttons[index].getText().isEmpty()) {
                buttons[index].setText(playerSymbol);
                buttons[index].setEnabled(false);
                out.println("MOVE " + index);
            }
        }
    }
}
