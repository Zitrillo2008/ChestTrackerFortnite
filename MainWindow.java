package com.mycompany.chest_counter_aplication;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.IntSummaryStatistics;
import java.util.Properties;

public class MainWindow extends JFrame {

    // Paleta de colores y constantes UI
    private static final Color COLOR_BG = Color.decode("#181825");
    private static final Color COLOR_SURFACE = Color.decode("#1E1E2E");
    private static final Color COLOR_CARD = Color.decode("#313244");
    private static final Color COLOR_ACCENT_BLUE = Color.decode("#89B4FA");
    private static final Color COLOR_ACCENT_GREEN = Color.decode("#A6E3A1");
    private static final Color COLOR_ACCENT_GOLD = Color.decode("#F9E2AF");
    private static final Color COLOR_TEXT_MUTED = Color.decode("#A6ADC8");
    private static final Color COLOR_TEXT_MAIN = Color.decode("#CDD6F4");
    private static final Color COLOR_DANGER = Color.decode("#F38BA8");
    
    private static final Font FONT_MAIN = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_EMOJI = new Font("Segoe UI Emoji", Font.PLAIN, 12);
    private static final Font FONT_NUMBERS = new Font("Segoe UI", Font.BOLD, 46);

    private static final String LOG_FILE_PATH = "historial_cofres.txt";

    // Gestor de persistencia
    private final ConfigManager configManager = new ConfigManager();

    // Contadores principales
    private int count = 2000;
    private int matchCount = 0;
    private int todayCount = 0;
    private int targetCount = 500;
    private String todayDate;

    // Atajos de teclado por defecto
    private int keyAdd = NativeKeyEvent.VC_HOME;
    private int keyAdd5 = NativeKeyEvent.VC_PAGE_DOWN;
    private int keySub = NativeKeyEvent.VC_END;
    private int keyReset = NativeKeyEvent.VC_F10;
    private int keyNewMatch = NativeKeyEvent.VC_PAGE_UP;
    private int keyToggle = NativeKeyEvent.VC_F11;

    // Control de captura de teclas en Ajustes
    private static KeyCaptureTarget activeCaptureTarget = null;
    private static KeySetter activeKeySetter = null;

    // Componentes de interfaz
    private JLabel lblTotalNumber;
    private JLabel lblMatchNumber;
    private JProgressBar progressBar;
    private JLabel lblProgress;
    private JTextField txtInitialInput;
    private JLabel lblLegend1;
    private JLabel lblLegend2;

    public MainWindow() {
        todayDate = LocalDate.now().toString();
        loadData();
        setupMainWindow();
        buildUI();
        positionOnSecondMonitor();
    }

    private void setupMainWindow() {
        setTitle("Fortnite Chest Tracker");
        setSize(460, 510);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout());
    }

    private void buildUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // --- 1. TARJETA DE CONTADORES ---
        JPanel pnlCard = new RoundedPanel(16, COLOR_SURFACE);
        pnlCard.setLayout(new BoxLayout(pnlCard, BoxLayout.Y_AXIS));
        pnlCard.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel pnlTitles = new JPanel(new GridLayout(1, 2));
        pnlTitles.setOpaque(false);
        pnlTitles.add(createLabel("TOTAL ACUMULADO", COLOR_ACCENT_BLUE, new Font("Segoe UI", Font.BOLD, 11), SwingConstants.CENTER));
        pnlTitles.add(createLabel("PARTIDA ACTUAL", COLOR_ACCENT_BLUE, new Font("Segoe UI", Font.BOLD, 11), SwingConstants.CENTER));

        JPanel pnlNumbers = new JPanel(new GridLayout(1, 2));
        pnlNumbers.setOpaque(false);
        lblTotalNumber = createLabel(String.valueOf(count), COLOR_ACCENT_GREEN, FONT_NUMBERS, SwingConstants.CENTER);
        lblMatchNumber = createLabel(String.valueOf(matchCount), COLOR_ACCENT_BLUE, FONT_NUMBERS, SwingConstants.CENTER);
        pnlNumbers.add(lblTotalNumber);
        pnlNumbers.add(lblMatchNumber);

        pnlCard.add(pnlTitles);
        pnlCard.add(Box.createVerticalStrut(4));
        pnlCard.add(pnlNumbers);

        // --- 2. BARRA DE PROGRESO ---
        lblProgress = createLabel("", COLOR_TEXT_MAIN, new Font("Segoe UI", Font.PLAIN, 12), SwingConstants.CENTER);
        lblProgress.setAlignmentX(Component.CENTER_ALIGNMENT);

        progressBar = new JProgressBar(0, 100);
        progressBar.setMaximumSize(new Dimension(400, 16));
        progressBar.setPreferredSize(new Dimension(400, 16));
        progressBar.setForeground(COLOR_ACCENT_GREEN);
        progressBar.setBackground(COLOR_CARD);
        progressBar.setBorderPainted(false);

        // --- 3. CAMBIAR NÚMERO INICIAL ---
        JPanel pnlInput = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        pnlInput.setOpaque(false);
        
        txtInitialInput = new JTextField(String.valueOf(count), 7);
        txtInitialInput.setFont(FONT_MAIN);
        txtInitialInput.setHorizontalAlignment(JTextField.CENTER);
        txtInitialInput.setBackground(COLOR_SURFACE);
        txtInitialInput.setForeground(COLOR_TEXT_MAIN);
        txtInitialInput.setCaretColor(COLOR_TEXT_MAIN);
        txtInitialInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_CARD, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        JButton btnApplyInput = createStyledButton("Aplicar", COLOR_CARD, COLOR_TEXT_MAIN, 80, 28, FONT_MAIN);
        btnApplyInput.addActionListener(e -> applyInitialInput());

        pnlInput.add(createLabel("Inicio:", COLOR_TEXT_MUTED, FONT_MAIN, SwingConstants.LEFT));
        pnlInput.add(txtInitialInput);
        pnlInput.add(btnApplyInput);

        // --- 4. BOTONES DE CONTROL ---
        JPanel pnlControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        pnlControls.setOpaque(false);

        JButton btnAdd = createStyledButton("+1", COLOR_SURFACE, COLOR_ACCENT_GREEN, 75, 36, FONT_MAIN);
        JButton btnAdd5 = createStyledButton("+5 🏦", COLOR_SURFACE, COLOR_ACCENT_GOLD, 85, 36, FONT_EMOJI);
        JButton btnSub = createStyledButton("-1", COLOR_SURFACE, COLOR_DANGER, 75, 36, FONT_MAIN);
        JButton btnNewMatch = createStyledButton("Nueva Partida", COLOR_ACCENT_BLUE, COLOR_BG, 130, 36, FONT_MAIN);

        btnAdd.addActionListener(e -> changeCount(1));
        btnAdd5.addActionListener(e -> changeCount(5));
        btnSub.addActionListener(e -> changeCount(-1));
        btnNewMatch.addActionListener(e -> resetMatchCount());

        pnlControls.add(btnAdd);
        pnlControls.add(btnAdd5);
        pnlControls.add(btnSub);
        pnlControls.add(btnNewMatch);

        // --- 5. LEYENDAS INFERIORES ---
        Font fontLegend = new Font("Segoe UI", Font.PLAIN, 11);
        lblLegend1 = createLabel("", COLOR_TEXT_MUTED, fontLegend, SwingConstants.CENTER);
        lblLegend2 = createLabel("", COLOR_TEXT_MUTED, fontLegend, SwingConstants.CENTER);
        lblLegend1.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblLegend2.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- 6. BOTONES AUXILIARES ---
        JPanel pnlBottomBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        pnlBottomBtns.setOpaque(false);

        JButton btnConfig = createStyledButton("⚙ Ajustes", COLOR_SURFACE, COLOR_TEXT_MAIN, 115, 32, FONT_EMOJI);
        JButton btnLog = createStyledButton("📋 Historial", COLOR_SURFACE, COLOR_TEXT_MAIN, 115, 32, FONT_EMOJI);
        JButton btnStats = createStyledButton("📊 Estadísticas", COLOR_SURFACE, COLOR_TEXT_MAIN, 115, 32, FONT_EMOJI);

        btnConfig.addActionListener(e -> openConfigDialog());
        btnLog.addActionListener(e -> openLogViewer());
        btnStats.addActionListener(e -> openStatsViewer());

        pnlBottomBtns.add(btnConfig);
        pnlBottomBtns.add(btnLog);
        pnlBottomBtns.add(btnStats);

        // Ensamblado
        mainPanel.add(pnlCard);
        mainPanel.add(Box.createVerticalStrut(14));
        mainPanel.add(lblProgress);
        mainPanel.add(Box.createVerticalStrut(6));
        mainPanel.add(progressBar);
        mainPanel.add(Box.createVerticalStrut(16));
        mainPanel.add(pnlInput);
        mainPanel.add(Box.createVerticalStrut(16));
        mainPanel.add(pnlControls);
        mainPanel.add(Box.createVerticalStrut(14));
        mainPanel.add(lblLegend1);
        mainPanel.add(Box.createVerticalStrut(3));
        mainPanel.add(lblLegend2);
        mainPanel.add(Box.createVerticalStrut(16));
        mainPanel.add(pnlBottomBtns);

        add(mainPanel, BorderLayout.CENTER);
        updateProgressUI();
    }

    private JLabel createLabel(String text, Color color, Font font, int alignment) {
        JLabel lbl = new JLabel(text, alignment);
        lbl.setForeground(color);
        lbl.setFont(font);
        return lbl;
    }

    private JButton createStyledButton(String text, Color bg, Color fg, int width, int height, Font font) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(width, height));
        btn.setFont(font);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void positionOnSecondMonitor() {
        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if (screens.length >= 2) {
            Rectangle bounds = screens[1].getDefaultConfiguration().getBounds();
            setLocation(bounds.x + (bounds.width - getWidth()) / 2, bounds.y + (bounds.height - getHeight()) / 2);
        } else {
            setLocationRelativeTo(null);
        }
    }

    // --- MÉTODOS PÚBLICOS DE CONTROL ---
    public synchronized void changeCount(int amount) {
        count = Math.max(0, count + amount);
        matchCount = Math.max(0, matchCount + amount);
        todayCount = Math.max(0, todayCount + amount);
        updateLabelsAndSave();
    }

    public synchronized void resetMatchCount() {
        if (matchCount > 0) configManager.logMatch(matchCount);
        matchCount = 0;
        updateLabelsAndSave();
    }

    public synchronized void resetCount() {
        count = 0;
        matchCount = 0;
        todayCount = 0;
        updateLabelsAndSave();
    }

    public void toggleVisibility() {
        setVisible(!isVisible());
    }

    public boolean isCapturingKey() {
        return activeCaptureTarget != null && activeKeySetter != null;
    }

    public void assignCapturedKey(int code) {
        if (activeKeySetter != null) {
            SwingUtilities.invokeLater(() -> {
                activeKeySetter.setKey(code);
                activeCaptureTarget = null;
                activeKeySetter = null;
            });
        }
    }

    public int getKeyAdd() { return keyAdd; }
    public int getKeyAdd5() { return keyAdd5; }
    public int getKeySub() { return keySub; }
    public int getKeyReset() { return keyReset; }
    public int getKeyNewMatch() { return keyNewMatch; }
    public int getKeyToggle() { return keyToggle; }

    private void applyInitialInput() {
        try {
            int val = Integer.parseInt(txtInitialInput.getText().trim());
            if (val >= 0) {
                count = val;
                updateLabelsAndSave();
            }
        } catch (NumberFormatException ignored) {}
    }

    private void updateLabelsAndSave() {
        lblTotalNumber.setText(String.valueOf(count));
        lblMatchNumber.setText(String.valueOf(matchCount));
        txtInitialInput.setText(String.valueOf(count));
        updateProgressUI();
        saveData();
    }

    private void updateProgressUI() {
        int pct = targetCount > 0 ? Math.min(100, (int) (((double) todayCount / targetCount) * 100)) : 100;
        progressBar.setValue(pct);
        lblProgress.setText(String.format("Meta Hoy: %d / %d (%d%%)", todayCount, targetCount, pct));
        lblTotalNumber.setForeground(pct >= 100 ? COLOR_ACCENT_GOLD : COLOR_ACCENT_GREEN);

        lblLegend1.setText(String.format("%s (+1)  ·  %s (+5 Bóveda)  ·  %s (-1)", getKeyName(keyAdd), getKeyName(keyAdd5), getKeyName(keySub)));
        lblLegend2.setText(String.format("%s (N.Partida)  ·  %s (Reiniciar)  ·  %s (Ocultar)", getKeyName(keyNewMatch), getKeyName(keyReset), getKeyName(keyToggle)));
    }

    private String getKeyName(int keyCode) {
        return NativeKeyEvent.getKeyText(keyCode);
    }

    // --- CARGA Y GUARDADO DE CONFIGURACIÓN ---
    private void saveData() {
        Properties prop = new Properties();
        prop.setProperty("UltimoConteo", String.valueOf(count));
        prop.setProperty("ConteoPartida", String.valueOf(matchCount));
        prop.setProperty("ConteoDiario", String.valueOf(todayCount));
        prop.setProperty("Meta", String.valueOf(targetCount));
        prop.setProperty("FechaMeta", todayDate);
        prop.setProperty("KeyAdd", String.valueOf(keyAdd));
        prop.setProperty("KeyAdd5", String.valueOf(keyAdd5));
        prop.setProperty("KeySub", String.valueOf(keySub));
        prop.setProperty("KeyReset", String.valueOf(keyReset));
        prop.setProperty("KeyNewMatch", String.valueOf(keyNewMatch));
        prop.setProperty("KeyToggle", String.valueOf(keyToggle));
        configManager.saveProperties(prop);
    }

    private void loadData() {
        Properties prop = configManager.loadProperties();
        if (!prop.isEmpty()) {
            todayCount = todayDate.equals(prop.getProperty("FechaMeta", "")) ? Integer.parseInt(prop.getProperty("ConteoDiario", "0")) : 0;
            count = Integer.parseInt(prop.getProperty("UltimoConteo", "2000"));
            matchCount = Integer.parseInt(prop.getProperty("ConteoPartida", "0"));
            targetCount = Integer.parseInt(prop.getProperty("Meta", "500"));
            keyAdd = Integer.parseInt(prop.getProperty("KeyAdd", String.valueOf(NativeKeyEvent.VC_HOME)));
            keyAdd5 = Integer.parseInt(prop.getProperty("KeyAdd5", String.valueOf(NativeKeyEvent.VC_PAGE_DOWN)));
            keySub = Integer.parseInt(prop.getProperty("KeySub", String.valueOf(NativeKeyEvent.VC_END)));
            keyReset = Integer.parseInt(prop.getProperty("KeyReset", String.valueOf(NativeKeyEvent.VC_F10)));
            keyNewMatch = Integer.parseInt(prop.getProperty("KeyNewMatch", String.valueOf(NativeKeyEvent.VC_PAGE_UP)));
            keyToggle = Integer.parseInt(prop.getProperty("KeyToggle", String.valueOf(NativeKeyEvent.VC_F11)));
        }
    }

    // --- VENTANAS AUXILIARES Y DE AJUSTES ---
    private void openStatsViewer() {
        JDialog dialog = createDialog("Estadísticas de Loot", 330, 240);
        Path logPath = Paths.get(LOG_FILE_PATH);
        IntSummaryStatistics stats = new IntSummaryStatistics();
        
        if (Files.exists(logPath)) {
            try {
                stats = Files.lines(logPath, StandardCharsets.UTF_8)
                    .filter(line -> line.contains("Partida finalizada:"))
                    .mapToInt(line -> Integer.parseInt(line.split("Partida finalizada:")[1].replace("cofres", "").trim()))
                    .summaryStatistics();
            } catch (IOException | NumberFormatException ignored) {}
        }

        String avgStr = stats.getCount() == 0 ? "0.00" : String.format("%.2f", stats.getAverage());
        int worstStr = stats.getCount() == 0 ? 0 : stats.getMin();
        int bestStr = stats.getCount() == 0 ? 0 : stats.getMax();

        JPanel pnl = new JPanel(new GridLayout(6, 1, 4, 4));
        pnl.setOpaque(false);
        pnl.setBorder(new EmptyBorder(14, 18, 14, 18));

        pnl.add(createLabel("📊 Rendimiento de Farmeo", COLOR_ACCENT_BLUE, new Font("Segoe UI Emoji", Font.BOLD, 14), SwingConstants.CENTER));
        pnl.add(createLabel("Partidas registradas: " + stats.getCount(), COLOR_TEXT_MAIN, FONT_EMOJI, SwingConstants.LEFT));
        pnl.add(createLabel("Cofres guardados: " + stats.getSum(), COLOR_TEXT_MAIN, FONT_EMOJI, SwingConstants.LEFT));
        pnl.add(createLabel("Promedio / partida: " + avgStr + " cofres", COLOR_ACCENT_GREEN, FONT_EMOJI, SwingConstants.LEFT));
        pnl.add(createLabel("Mejor partida: " + bestStr + " cofres", COLOR_TEXT_MAIN, FONT_EMOJI, SwingConstants.LEFT));
        pnl.add(createLabel("Peor partida: " + worstStr + " cofres", COLOR_TEXT_MAIN, FONT_EMOJI, SwingConstants.LEFT));

        dialog.add(pnl);
        dialog.setVisible(true);
    }

    private void openLogViewer() {
        JDialog dialog = createDialog("Historial de Partidas", 390, 330);
        dialog.setLayout(new BorderLayout());

        JTextArea txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setBackground(COLOR_SURFACE);
        txtLog.setForeground(COLOR_TEXT_MAIN);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLog.setBorder(new EmptyBorder(8, 8, 8, 8));

        File file = new File(LOG_FILE_PATH);
        if (file.exists()) {
            try (BufferedReader br = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                txtLog.read(br, null);
            } catch (IOException e) {
                txtLog.setText("Error leyendo archivo.");
            }
        } else {
            txtLog.setText("Aún no hay partidas registradas.");
        }

        JScrollPane scroll = new JScrollPane(txtLog);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        dialog.add(scroll, BorderLayout.CENTER);

        JPanel pnlBtns = new JPanel(new FlowLayout());
        pnlBtns.setOpaque(false);
        JButton btnClear = createStyledButton("Borrar Historial", COLOR_SURFACE, COLOR_DANGER, 140, 30, FONT_MAIN);
        
        btnClear.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                dialog, 
                "¿Seguro que quieres borrar todo el historial?", 
                "Confirmar", 
                JOptionPane.YES_NO_OPTION
            );
            
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    if (Files.exists(file.toPath())) {
                        Files.delete(file.toPath());
                    }
                    txtLog.setText("Aún no hay partidas registradas.");
                    JOptionPane.showMessageDialog(dialog, "Historial borrado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(dialog, "No se pudo borrar el archivo (puede estar en uso).", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        pnlBtns.add(btnClear);
        dialog.add(pnlBtns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JDialog createDialog(String title, int w, int h) {
        JDialog d = new JDialog(this, title, true);
        d.setSize(w, h);
        d.setLocationRelativeTo(this);
        d.getContentPane().setBackground(COLOR_BG);
        return d;
    }

    private int[] tempKeys = new int[6];

    private void openConfigDialog() {
        JDialog dialog = createDialog("Configurar Teclas y Meta", 380, 430);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        tempKeys = new int[]{keyAdd, keyAdd5, keySub, keyNewMatch, keyReset, keyToggle};
        String[] labels = {"Sumar (+1):", "Bóveda (+5):", "Restar (-1):", "Nueva Partida:", "Reiniciar Todo:", "Mostrar/Ocultar:"};
        
        JButton[] btnKeys = new JButton[6];
        for (int i = 0; i < 6; i++) {
            final int index = i;
            btnKeys[i] = createStyledButton(getKeyName(tempKeys[i]), COLOR_SURFACE, COLOR_TEXT_MAIN, 140, 28, FONT_MAIN);
            btnKeys[i].addActionListener(e -> startCapture(btnKeys[index], code -> tempKeys[index] = code, tempKeys[index]));
            addConfigRow(dialog, gbc, i, labels[i], btnKeys[i]);
        }

        JTextField txtTarget = new JTextField(String.valueOf(targetCount));
        txtTarget.setBackground(COLOR_SURFACE);
        txtTarget.setForeground(COLOR_TEXT_MAIN);
        txtTarget.setCaretColor(COLOR_TEXT_MAIN);
        txtTarget.setFont(FONT_MAIN);
        txtTarget.setHorizontalAlignment(JTextField.CENTER);
        addConfigRow(dialog, gbc, 6, "Meta Diaria:", txtTarget);

        JPanel pnlActionBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        pnlActionBtns.setOpaque(false);

        JButton btnResetDefaults = createStyledButton("Restaurar Default", COLOR_CARD, COLOR_TEXT_MUTED, 130, 32, FONT_MAIN);
        btnResetDefaults.addActionListener(e -> {
            tempKeys[0] = NativeKeyEvent.VC_HOME;
            tempKeys[1] = NativeKeyEvent.VC_PAGE_DOWN;
            tempKeys[2] = NativeKeyEvent.VC_END;
            tempKeys[3] = NativeKeyEvent.VC_PAGE_UP;
            tempKeys[4] = NativeKeyEvent.VC_F10;
            tempKeys[5] = NativeKeyEvent.VC_F11;
            txtTarget.setText("500");

            for (int i = 0; i < 6; i++) {
                btnKeys[i].setText(getKeyName(tempKeys[i]));
            }
        });

        JButton btnSave = createStyledButton("Guardar Ajustes", COLOR_ACCENT_BLUE, COLOR_BG, 130, 32, FONT_MAIN);
        btnSave.addActionListener(e -> {
            keyAdd = tempKeys[0]; keyAdd5 = tempKeys[1]; keySub = tempKeys[2];
            keyNewMatch = tempKeys[3]; keyReset = tempKeys[4]; keyToggle = tempKeys[5];
            try { targetCount = Integer.parseInt(txtTarget.getText().trim()); } catch (Exception ignored) {}
            updateLabelsAndSave();
            activeCaptureTarget = null;
            activeKeySetter = null;
            dialog.dispose();
        });

        pnlActionBtns.add(btnResetDefaults);
        pnlActionBtns.add(btnSave);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 8, 8, 8);
        gbc.anchor = GridBagConstraints.CENTER;
        dialog.add(pnlActionBtns, gbc);

        dialog.setVisible(true);
    }

    private void addConfigRow(JDialog dialog, GridBagConstraints gbc, int row, String labelText, Component comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        dialog.add(createLabel(labelText, COLOR_TEXT_MAIN, new Font("Segoe UI", Font.PLAIN, 12), SwingConstants.LEFT), gbc);
        gbc.gridx = 1;
        dialog.add(comp, gbc);
    }

    private void startCapture(JButton button, KeySetter setter, int currentCode) {
        button.setText("Presiona...");
        button.setBackground(COLOR_ACCENT_GOLD);
        button.setForeground(COLOR_BG);
        activeCaptureTarget = new KeyCaptureTarget(button, currentCode);
        activeKeySetter = (code) -> {
            setter.setKey(code);
            button.setText(NativeKeyEvent.getKeyText(code));
            button.setBackground(COLOR_SURFACE);
            button.setForeground(COLOR_TEXT_MAIN);
        };
    }

    private static class KeyCaptureTarget {
        JButton button; int newCode;
        KeyCaptureTarget(JButton b, int c) { button = b; newCode = c; }
    }
    
    private interface KeySetter { void setKey(int nativeKeyCode); }

    private static class RoundedPanel extends JPanel {
        private final int radius; private final Color bgColor;
        public RoundedPanel(int r, Color bg) { this.radius = r; this.bgColor = bg; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), radius, radius));
        }
    }
}