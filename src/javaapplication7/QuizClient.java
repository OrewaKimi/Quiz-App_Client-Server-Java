/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package javaapplication7;

/**
 *
 * @author Kimi Maulana
 */
import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class QuizClient extends JFrame {
    private JTextArea questionArea;
    private JRadioButton trueButton;
    private JRadioButton falseButton;
    private ButtonGroup answerGroup;
    private JButton submitButton;
    private JLabel statusLabel;
    private JLabel timerLabel;
    private JLabel scoreLabel;
    private JProgressBar timerBar;
    private PrintWriter out;
    private BufferedReader in;
    private int score = 0;
    private int currentQuestion = 0;
    private String[] questions;
    private boolean isExamOpen = false;
    private javax.swing.Timer reconnectTimer;
    private javax.swing.Timer questionTimer;
    private int timeLimit = 30;
    private int remainingTime;
    private String playerName = "Anonim";
    private Socket socket;
    
    public QuizClient() {
        setTitle("Quiz Client - Benar/Salah");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        
        // Panel utama
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Panel atas untuk status dan informasi
        JPanel topPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        
        // Status label
        statusLabel = new JLabel("Menunggu koneksi ke server...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(Color.GRAY);
        
        // Timer label dan progress bar
        JPanel timerPanel = new JPanel(new BorderLayout(5, 0));
        timerLabel = new JLabel("Waktu: --:--");
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timerBar = new JProgressBar(0, 100);
        timerBar.setStringPainted(true);
        timerPanel.add(timerLabel, BorderLayout.WEST);
        timerPanel.add(timerBar, BorderLayout.CENTER);
        
        // Score label
        scoreLabel = new JLabel("Skor: 0");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        topPanel.add(statusLabel);
        topPanel.add(timerPanel);
        topPanel.add(scoreLabel);
        
        // Area pertanyaan
        questionArea = new JTextArea();
        questionArea.setEditable(false);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setFont(new Font("Arial", Font.PLAIN, 16));
        JScrollPane scrollPane = new JScrollPane(questionArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Pertanyaan"));
        
        // Panel jawaban
        JPanel answerPanel = new JPanel(new BorderLayout(5, 5));
        
        // Radio buttons untuk jawaban
        answerGroup = new ButtonGroup();
        trueButton = new JRadioButton("Benar");
        falseButton = new JRadioButton("Salah");
        trueButton.setFont(new Font("Arial", Font.PLAIN, 14));
        falseButton.setFont(new Font("Arial", Font.PLAIN, 14));
        
        answerGroup.add(trueButton);
        answerGroup.add(falseButton);
        
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        radioPanel.add(trueButton);
        radioPanel.add(falseButton);
        
        submitButton = new JButton("Kirim Jawaban");
        submitButton.setBackground(new Color(70, 130, 180));
        submitButton.setForeground(Color.WHITE);
        submitButton.setFont(new Font("Arial", Font.BOLD, 14));
        
        answerPanel.add(radioPanel, BorderLayout.CENTER);
        answerPanel.add(submitButton, BorderLayout.SOUTH);
        
        // Menambahkan komponen ke panel utama
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(answerPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Event listener untuk tombol submit
        submitButton.addActionListener(e -> submitAnswer());
        
        // Timer untuk reconnect
        reconnectTimer = new javax.swing.Timer(5000, e -> connectToServer());
        reconnectTimer.setRepeats(true);
        
        // Timer untuk pertanyaan
        questionTimer = new javax.swing.Timer(1000, e -> updateTimer());
        questionTimer.setRepeats(true);
        
        // Nonaktifkan komponen sampai terhubung
        setComponentsEnabled(false);
        
        // Dialog untuk input nama
        showNameDialog();
        
        // Mulai koneksi ke server
        connectToServer();
    }
    
    private void setComponentsEnabled(boolean enabled) {
        trueButton.setEnabled(enabled);
        falseButton.setEnabled(enabled);
        submitButton.setEnabled(enabled);
    }
    
    private void showNameDialog() {
        String name = JOptionPane.showInputDialog(this,
            "Masukkan nama Anda:",
            "Selamat Datang",
            JOptionPane.QUESTION_MESSAGE);
            
        if (name != null && !name.trim().isEmpty()) {
            playerName = name.trim();
        }
    }
    
    private void connectToServer() {
        try {
            socket = new Socket("localhost", 5000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            reconnectTimer.stop();
            statusLabel.setText("Terhubung ke server");
            statusLabel.setForeground(Color.GREEN);
            
            // Kirim nama ke server
            out.println("NAME:" + playerName);
            
            // Mulai thread untuk membaca respons server
            new Thread(this::handleServerResponses).start();
            
        } catch (IOException e) {
            handleConnectionLost();
        }
    }
    
    private void handleServerResponses() {
        try {
            String response;
            while ((response = in.readLine()) != null) {
                final String finalResponse = response;
                SwingUtilities.invokeLater(() -> processServerResponse(finalResponse));
            }
        } catch (IOException e) {
            handleConnectionLost();
        }
    }
    
    private void processServerResponse(String response) {
        if (response.startsWith("EXAM_OPENED:")) {
            timeLimit = Integer.parseInt(response.substring(12));
            handleExamOpened();
        } else if (response.equals("EXAM_CLOSED")) {
            handleExamClosed();
        } else if (response.startsWith("QUESTION:")) {
            addQuestion(response.substring(9));
        } else if (response.equals("END_QUESTIONS")) {
            displayQuestion(0);
        } else if (response.startsWith("ANSWER_RESULT:")) {
            boolean isCorrect = Boolean.parseBoolean(response.substring(14));
            handleAnswerResult(isCorrect);
        }
    }
    
    private void handleConnectionLost() {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Koneksi terputus - Mencoba menghubungkan kembali...");
            statusLabel.setForeground(Color.RED);
            setComponentsEnabled(false);
            questionTimer.stop();
            reconnectTimer.start();
        });
    }
    
    private void handleExamClosed() {
        isExamOpen = false;
        statusLabel.setText("Ujian ditutup");
        statusLabel.setForeground(Color.RED);
        setComponentsEnabled(false);
        questionTimer.stop();
        timerLabel.setText("Waktu: --:--");
        timerBar.setValue(0);
        questionArea.setText("Ujian sedang ditutup.\nSilakan tunggu hingga ujian dibuka kembali.");
    }
    
    private void handleExamOpened() {
        isExamOpen = true;
        statusLabel.setText("Ujian dibuka");
        statusLabel.setForeground(Color.GREEN);
        setComponentsEnabled(true);
        out.println("GET_QUESTIONS");
    }
    
    private void addQuestion(String question) {
        if (questions == null) {
            questions = new String[]{question};
        } else {
            String[] newQuestions = new String[questions.length + 1];
            System.arraycopy(questions, 0, newQuestions, 0, questions.length);
            newQuestions[questions.length] = question;
            questions = newQuestions;
        }
    }
    
    private void displayQuestion(int index) {
        if (index < questions.length) {
            currentQuestion = index;
            questionArea.setText("Pertanyaan " + (index + 1) + " dari " + questions.length + ":\n\n" + questions[index]);
            answerGroup.clearSelection();
            setComponentsEnabled(true);
            
            // Reset dan mulai timer
            remainingTime = timeLimit;
            updateTimer();
            questionTimer.restart();
        } else {
            showFinalScore();
        }
    }
    
    private void updateTimer() {
        if (remainingTime > 0) {
            remainingTime--;
            int percentage = (remainingTime * 100) / timeLimit;
            timerBar.setValue(percentage);
            timerLabel.setText(String.format("Waktu: %02d:%02d", remainingTime / 60, remainingTime % 60));
            
            if (remainingTime <= 5) {
                timerBar.setForeground(Color.RED);
            } else if (remainingTime <= 10) {
                timerBar.setForeground(Color.ORANGE);
            } else {
                timerBar.setForeground(new Color(70, 130, 180));
            }
        } else {
            questionTimer.stop();
            submitAnswer();
        }
    }
    
    private void handleAnswerResult(boolean isCorrect) {
        if (isCorrect) {
            score++;
            scoreLabel.setText("Skor: " + score);
        }
        displayQuestion(currentQuestion + 1);
    }
    
    private void submitAnswer() {
        if (!isExamOpen) {
            JOptionPane.showMessageDialog(this,
                "Ujian sedang ditutup. Silakan tunggu hingga ujian dibuka kembali.",
                "Ujian Ditutup",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (trueButton.isSelected() || falseButton.isSelected()) {
            questionTimer.stop();
            boolean answer = trueButton.isSelected();
            out.println("ANSWER:" + answer);
            setComponentsEnabled(false);
        } else {
            JOptionPane.showMessageDialog(this,
                "Silakan pilih jawaban Benar atau Salah",
                "Peringatan",
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void showFinalScore() {
        out.println("FINAL_SCORE:" + score);
        questionArea.setText("Quiz Selesai!\n\n" +
            "Nama: " + playerName + "\n" +
            "Total Pertanyaan: " + questions.length + "\n" +
            "Jawaban Benar: " + score + "\n" +
            "Skor Akhir: " + (score * 10) + "\n" +
            "Terima kasih telah berpartisipasi.");
        setComponentsEnabled(false);
        questionTimer.stop();
        timerLabel.setText("Waktu: --:--");
        timerBar.setValue(0);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new QuizClient().setVisible(true);
        });
    }
}