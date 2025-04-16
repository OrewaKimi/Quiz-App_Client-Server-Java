/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package javaapplication7;

/**
 *
 * @author Davit Zarly
 */
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public class QuizServer extends JFrame {
    private static final int PORT = 5000;
    private static boolean isExamOpen = true;
    private static final int TIME_LIMIT = 30;
    private static final ArrayList<ClientHandler> clients = new ArrayList<>();
    
    // Komponen GUI
    private JTextArea logArea;
    private JButton startButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private JLabel clientCountLabel;
    
    // Daftar pertanyaan dan jawaban
    private static final String[] QUESTIONS = {
        "Apakah Jakarta adalah ibu kota Indonesia?",
        "Apakah Bumi berbentuk datar?",
        "Apakah air mendidih pada suhu 100 derajat Celcius?",
        "Apakah Indonesia memiliki 34 provinsi?",
        "Apakah matahari terbit dari barat?",
        "Apakah manusia memiliki 206 tulang?",
        "Apakah bulan memiliki gravitasi?",
        "Apakah ikan bernafas menggunakan paru-paru?",
        "Apakah Indonesia merdeka pada tahun 1945?",
        "Apakah komputer pertama kali ditemukan pada abad ke-20?"
    };
    
    // Jawaban dari daftar pertanyaan 
    private static final boolean[] ANSWERS = {
        true,   // Jakarta adalah ibu kota Indonesia
        false,  // Bumi tidak datar
        true,   // Air mendidih pada 100°C
        true,   // Indonesia memiliki 34 provinsi
        false,  // Matahari terbit dari timur
        true,   // Manusia memiliki 206 tulang
        true,   // Bulan memiliki gravitasi
        false,  // Ikan bernafas dengan insang
        true,   // Indonesia merdeka 1945
        true    // Komputer ditemukan abad 20
    };
    
    public QuizServer() {
        // Setup JFrame
        setTitle("Quiz Server Control Panel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        
        // Inisialisasi komponen
        initComponents();
        
        // Mulai server di thread terpisah
        new Thread(this::startServer).start();
    }
    
    private void initComponents() {
        // Panel utama
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel atas untuk kontrol
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        
        statusLabel = new JLabel("Status: Server Belum Berjalan");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        clientCountLabel = new JLabel("Client Terhubung: 0");
        clientCountLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        startButton = new JButton("Mulai Ujian");
        startButton.setBackground(new Color(46, 204, 113));
        startButton.setForeground(Color.WHITE);
        
        stopButton = new JButton("Tutup Ujian");
        stopButton.setBackground(new Color(231, 76, 60));
        stopButton.setForeground(Color.WHITE);
        stopButton.setEnabled(false);
        
        topPanel.add(statusLabel);
        topPanel.add(clientCountLabel);
        topPanel.add(startButton);
        topPanel.add(stopButton);
        
        // Area log
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Server Log"));
        
        // Tambahkan ke panel utama
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Event handlers
        startButton.addActionListener(e -> startExam());
        stopButton.addActionListener(e -> stopExam());
        
        add(mainPanel);
    }
    
    private void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            updateLog("Server berjalan pada port " + PORT);
            updateStatus("Server Aktif", Color.GREEN);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                updateClientCount();
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            updateLog("Error pada server: " + e.getMessage());
            updateStatus("Server Error", Color.RED);
        }
    }
    
    private void startExam() {
        isExamOpen = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        updateLog("Ujian dimulai");
        broadcastExamStatus();
    }
    
    private void stopExam() {
        isExamOpen = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        updateLog("Ujian ditutup");
        broadcastExamStatus();
    }
    
    private void broadcastExamStatus() {
        for (ClientHandler client : clients) {
            client.sendExamStatus();
        }
    }
    
    private void updateLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(String.format("[%tT] %s%n", new Date(), message));
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void updateStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Status: " + status);
            statusLabel.setForeground(color);
        });
    }
    
    private void updateClientCount() {
        SwingUtilities.invokeLater(() -> {
            clientCountLabel.setText("Client Terhubung: " + clients.size());
        });
    }
    
    class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String playerName = "Anonim";
        private int score = 0;
        private int currentQuestion = 0;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                updateLog("Client baru terhubung: " + socket.getInetAddress());
            } catch (IOException e) {
                updateLog("Error saat membuat handler: " + e.getMessage());
            }
        }
        
        @Override
        public void run() {
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("NAME:")) {
                        playerName = inputLine.substring(5);
                        updateLog("Pemain baru: " + playerName);
                        sendExamStatus();
                    } else if (inputLine.equals("GET_QUESTIONS")) {
                        sendQuestions();
                    } else if (inputLine.startsWith("ANSWER:")) {
                        handleAnswer(inputLine.substring(7));
                    } else if (inputLine.startsWith("FINAL_SCORE:")) {
                        handleFinalScore(inputLine.substring(12));
                    }
                }
            } catch (IOException e) {
                updateLog("Error pada client handler: " + e.getMessage());
            } finally {
                cleanup();
            }
        }
        
        private void sendExamStatus() {
            if (isExamOpen) {
                out.println("EXAM_OPENED:" + TIME_LIMIT);
            } else {
                out.println("EXAM_CLOSED");
            }
        }
        
        private void sendQuestions() {
            if (!isExamOpen) {
                out.println("EXAM_CLOSED");
                return;
            }
            
            for (String question : QUESTIONS) {
                out.println("QUESTION:" + question);
            }
            out.println("END_QUESTIONS");
        }
        
        private void handleAnswer(String answerStr) {
            if (!isExamOpen || currentQuestion >= QUESTIONS.length) {
                return;
            }
            
            boolean userAnswer = Boolean.parseBoolean(answerStr);
            boolean isCorrect = userAnswer == ANSWERS[currentQuestion];
            
            out.println("ANSWER_RESULT:" + isCorrect);
            
            if (isCorrect) {
                score++;
            }
            
            currentQuestion++;
            updateLog(playerName + " - Pertanyaan " + currentQuestion + ": " + 
                     (isCorrect ? "Benar" : "Salah") + " (Skor: " + score + ")");
        }
        
        private void handleFinalScore(String scoreStr) {
            try {
                int finalScore = Integer.parseInt(scoreStr);
                updateLog("Skor akhir " + playerName + ": " + finalScore);
            } catch (NumberFormatException e) {
                updateLog("Error saat memproses skor akhir");
            }
        }
        
        private void cleanup() {
            try {
                clients.remove(this);
                updateClientCount();
                if (socket != null) socket.close();
                if (in != null) in.close();
                if (out != null) out.close();
                updateLog("Client terputus: " + playerName);
            } catch (IOException e) {
                updateLog("Error saat cleanup: " + e.getMessage());
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new QuizServer().setVisible(true);
        });
    }
}