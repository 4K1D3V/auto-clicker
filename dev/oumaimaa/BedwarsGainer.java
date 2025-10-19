package dev.oumaimaa;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

/**
 * Bedwars Gainer Application for Minecraft 1.8 with Premium Features and Hotkey Customization.
 * 
 * Premium Features (unlocked with key "grokpremium"):
 * - Click Statistics: Total clicks, avg CPS, session time displayed in GUI.
 * - Higher CPS Limit: Free max 15 CPS, premium max 30 CPS.
 * - Macro Recording/Playback: Record and replay mouse clicks with delays.
 * 
 * GUI: Optimized with GridBagLayout, grouped panels, tooltips.
 * Compatibility: Badlion, Lunar, Silent Client with anti-detection features.
 */
public class BedwarsGainer extends JFrame {

    private final Robot robot;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean simulating = new AtomicBoolean(false);
    private final AtomicBoolean keyHolding = new AtomicBoolean(false);
    private final AtomicBoolean mouseHolding = new AtomicBoolean(false);
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Config config = new Config();
    private final Clicker clicker = new Clicker(config);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File profileFile = new File(System.getProperty("user.home"), "bedwars_gainer_profiles.json");
    private final Map<String, Config> profiles = new HashMap<>();
    private JComboBox<String> profileComboBox;
    private int toggleKeyCode = NativeKeyEvent.VC_F1;
    private int holdKeyCode = NativeKeyEvent.VC_F2;
    private boolean premium = false;
    private long sessionStartTime = 0;
    private int totalClicks = 0;
    private JLabel statsLabel;
    private JTextField premiumKeyField;
    private JButton activatePremiumButton, recordMacroButton, playMacroButton;
    private List<MacroEvent> macroEvents = new ArrayList<>();
    private long macroStartTime = 0;
    private Timer statsTimer;

    // GUI Components
    private JButton toggleButton, saveProfileButton, loadProfileButton;
    private JSpinner minCpsSpinner, maxCpsSpinner, minBurstSpinner, maxBurstSpinner, minHoldSpinner, maxHoldSpinner, wTapSpinner,
            breakEverySpinner, breakMinSpinner, breakMaxSpinner, toggleKeySpinner, holdKeySpinner;
    private JSlider stdDevSlider, jitterAmpSlider;
    private JRadioButton leftButtonRb, rightButtonRb, boostModeRb, keyHoldModeRb, mouseHoldModeRb,
            gaussianRb, uniformRb, poissonRb, fixedBurstRb, randomBurstRb,
            randomJitterRb, circularJitterRb, linearJitterRb, figure8JitterRb;
    private JCheckBox reachCheck, jitterCheck, breaksCheck, minecraftFocusCheck, butterflyCheck;

    public BedwarsGainer() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            JOptionPane.showMessageDialog(this, "Failed to initialize Robot: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            throw new RuntimeException(e);
        }

        // Initialize preset profiles
        initializePresetProfiles();
        loadCustomProfiles();

        ListenerRegistry.registerListeners(this, toggleKeyCode, holdKeyCode);

        setTitle("Bedwars Gainer - Ultimate CPS Booster (1.8)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        createGUI();
        pack();
        setLocationRelativeTo(null);
    }

    private void initializePresetProfiles() {
        // Safe PvP: Moderate CPS, safe for anti-cheat
        Config safePvP = new Config();
        safePvP.minCPS = 8;
        safePvP.maxCPS = 12;
        safePvP.minBurst = 2;
        safePvP.maxBurst = 4;
        safePvP.randomBurst = false;
        safePvP.minHoldTime = 20;
        safePvP.maxHoldTime = 80;
        safePvP.stdDevPercent = 0.15;
        safePvP.delayDistribution = "Gaussian";
        safePvP.leftClick = true;
        safePvP.holdMode = false;
        safePvP.mouseHoldMode = false;
        safePvP.butterflyEnabled = false;
        safePvP.reachEnabled = true;
        safePvP.wTapDuration = 50;
        safePvP.jitterEnabled = true;
        safePvP.jitterAmplitude = 5;
        safePvP.jitterPattern = "Random";
        safePvP.enableBreaks = true;
        safePvP.breakEvery = 50;
        safePvP.breakMin = 500;
        safePvP.breakMax = 2000;
        safePvP.onlyInMinecraft = true;
        safePvP.toggleKeyCode = NativeKeyEvent.VC_F1;
        safePvP.holdKeyCode = NativeKeyEvent.VC_F2;
        profiles.put("Safe PvP", safePvP);

        // Aggressive PvP: High CPS, riskier
        Config aggressivePvP = new Config();
        aggressivePvP.minCPS = 12;
        aggressivePvP.maxCPS = 16;
        aggressivePvP.minBurst = 3;
        aggressivePvP.maxBurst = 5;
        aggressivePvP.randomBurst = true;
        aggressivePvP.minHoldTime = 15;
        aggressivePvP.maxHoldTime = 60;
        aggressivePvP.stdDevPercent = 0.10;
        aggressivePvP.delayDistribution = "Poisson";
        aggressivePvP.leftClick = true;
        aggressivePvP.holdMode = true;
        aggressivePvP.mouseHoldMode = false;
        aggressivePvP.butterflyEnabled = true;
        aggressivePvP.reachEnabled = true;
        aggressivePvP.wTapDuration = 40;
        aggressivePvP.jitterEnabled = true;
        aggressivePvP.jitterAmplitude = 8;
        aggressivePvP.jitterPattern = "Figure8";
        aggressivePvP.enableBreaks = false;
        aggressivePvP.breakEvery = 50;
        aggressivePvP.breakMin = 500;
        aggressivePvP.breakMax = 2000;
        aggressivePvP.onlyInMinecraft = true;
        aggressivePvP.toggleKeyCode = NativeKeyEvent.VC_F1;
        aggressivePvP.holdKeyCode = NativeKeyEvent.VC_F3;
        profiles.put("Aggressive PvP", aggressivePvP);

        // Bridging: Right-click focus, lower CPS
        Config bridging = new Config();
        bridging.minCPS = 6;
        bridging.maxCPS = 10;
        bridging.minBurst = 1;
        bridging.maxBurst = 3;
        bridging.randomBurst = false;
        bridging.minHoldTime = 30;
        bridging.maxHoldTime = 100;
        bridging.stdDevPercent = 0.20;
        bridging.delayDistribution = "Uniform";
        bridging.leftClick = false;
        bridging.holdMode = false;
        bridging.mouseHoldMode = true;
        bridging.butterflyEnabled = false;
        bridging.reachEnabled = false;
        bridging.wTapDuration = 50;
        bridging.jitterEnabled = true;
        bridging.jitterAmplitude = 4;
        bridging.jitterPattern = "Linear";
        bridging.enableBreaks = true;
        bridging.breakEvery = 60;
        bridging.breakMin = 600;
        bridging.breakMax = 2500;
        bridging.onlyInMinecraft = true;
        bridging.toggleKeyCode = NativeKeyEvent.VC_F1;
        bridging.holdKeyCode = NativeKeyEvent.VC_F2;
        profiles.put("Bridging", bridging);
    }

    private void loadCustomProfiles() {
        if (profileFile.exists()) {
            try {
                Map<String, Config> loadedProfiles = objectMapper.readValue(profileFile, objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Config.class));
                profiles.putAll(loadedProfiles);
            } catch (IOException e) {
                System.err.println("Failed to load profiles: " + e.getMessage());
            }
        }
    }

    private void saveCustomProfiles() {
        try {
            objectMapper.writeValue(profileFile, profiles);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save profiles: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createGUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        // Profile Panel
        JPanel profilePanel = new JPanel(new GridBagLayout());
        profilePanel.setBorder(BorderFactory.createTitledBorder("Profile Selection"));
        gbc.gridx = 0; gbc.gridy = row++;
        add(profilePanel, gbc);

        gbcProfilePanel(profilePanel, 0, "Profile:", profileComboBox = new JComboBox<>(profiles.keySet().toArray(new String[0])), "Select a preset or custom profile");
        gbcProfilePanel(profilePanel, 1, saveProfileButton = new JButton("Save Profile"), "Save current settings as a custom profile");
        gbcProfilePanel(profilePanel, 2, loadProfileButton = new JButton("Load Profile"), "Load selected profile settings");
        profileComboBox.addActionListener(e -> applyProfile());
        saveProfileButton.addActionListener(e -> saveProfile());
        loadProfileButton.addActionListener(e -> applyProfile());

        // Hotkey Panel
        JPanel hotkeyPanel = new JPanel(new GridBagLayout());
        hotkeyPanel.setBorder(BorderFactory.createTitledBorder("Hotkey Customization"));
        gbc.gridy = row++;
        add(hotkeyPanel, gbc);

        gbcHotkeyPanel(hotkeyPanel, 0, "Toggle Key (VK):", toggleKeySpinner = new JSpinner(new SpinnerNumberModel(NativeKeyEvent.VC_F1, 0, 65535, 1)), "Virtual key code for toggle (e.g., 112 for F1)");
        gbcHotkeyPanel(hotkeyPanel, 1, "Hold Key (VK):", holdKeySpinner = new JSpinner(new SpinnerNumberModel(NativeKeyEvent.VC_F2, 0, 65535, 1)), "Virtual key code for hold mode (e.g., 113 for F2)");
        toggleKeySpinner.addChangeListener(e -> {
            toggleKeyCode = (Integer) toggleKeySpinner.getValue();
            ListenerRegistry.updateToggleKey(toggleKeyCode);
        });
        holdKeySpinner.addChangeListener(e -> {
            holdKeyCode = (Integer) holdKeySpinner.getValue();
            ListenerRegistry.updateHoldKey(holdKeyCode);
        });

        // Premium Panel
        JPanel premiumPanel = new JPanel(new GridBagLayout());
        premiumPanel.setBorder(BorderFactory.createTitledBorder("Premium Features"));
        gbc.gridy = row++;
        add(premiumPanel, gbc);

        gbcPremiumPanel(premiumPanel, 0, "Premium Key:", premiumKeyField = new JPasswordField(), "Enter premium key to unlock features");
        gbcPremiumPanel(premiumPanel, 1, activatePremiumButton = new JButton("Activate"), "Activate premium features");
        gbcPremiumPanel(premiumPanel, 2, statsLabel = new JLabel("Stats: N/A"), "Click statistics (premium only)");
        gbcPremiumPanel(premiumPanel, 3, recordMacroButton = new JButton("Record Macro"), "Record macro (premium only)");
        gbcPremiumPanel(premiumPanel, 4, playMacroButton = new JButton("Play Macro"), "Play recorded macro (premium only)");
        recordMacroButton.setEnabled(false);
        playMacroButton.setEnabled(false);
        activatePremiumButton.addActionListener(e -> activatePremium());
        recordMacroButton.addActionListener(e -> toggleRecording());
        playMacroButton.addActionListener(e -> playMacro());

        // CPS Panel
        JPanel cpsPanel = new JPanel(new GridBagLayout());
        cpsPanel.setBorder(BorderFactory.createTitledBorder("Click Speed (CPS)"));
        gbc.gridy = row++;
        add(cpsPanel, gbc);

        gbcCpsPanel(cpsPanel, 0, "Min CPS:", minCpsSpinner = new JSpinner(new SpinnerNumberModel(8, 1, premium ? 30 : 15, 1)), "Minimum clicks per second");
        gbcCpsPanel(cpsPanel, 1, "Max CPS:", maxCpsSpinner = new JSpinner(new SpinnerNumberModel(12, 1, premium ? 30 : 15, 1)), "Maximum clicks per second");

        // Burst Panel
        JPanel burstPanel = new JPanel(new GridBagLayout());
        burstPanel.setBorder(BorderFactory.createTitledBorder("Burst Settings"));
        gbc.gridy = row++;
        add(burstPanel, gbc);

        gbcBurstPanel(burstPanel, 0, "Min Burst:", minBurstSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1)), "Minimum clicks per burst");
        gbcBurstPanel(burstPanel, 1, "Max Burst:", maxBurstSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 10, 1)), "Maximum clicks per burst");
        gbcBurstPanel(burstPanel, 2, "Pattern:", fixedBurstRb = new JRadioButton("Fixed", true), "Fixed burst length");
        gbcBurstPanel(burstPanel, 3, "", randomBurstRb = new JRadioButton("Random", false), "Random burst length");
        ButtonGroup burstGroup = new ButtonGroup();
        burstGroup.add(fixedBurstRb);
        burstGroup.add(randomBurstRb);
        fixedBurstRb.addActionListener(e -> config.randomBurst = false);
        randomBurstRb.addActionListener(e -> config.randomBurst = true);

        // Hold Time Panel
        JPanel holdPanel = new JPanel(new GridBagLayout());
        holdPanel.setBorder(BorderFactory.createTitledBorder("Hold Time"));
        gbc.gridy = row++;
        add(holdPanel, gbc);

        gbcHoldPanel(holdPanel, 0, "Min Hold (ms):", minHoldSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 100, 1)), "Minimum click hold time");
        gbcHoldPanel(holdPanel, 1, "Max Hold (ms):", maxHoldSpinner = new JSpinner(new SpinnerNumberModel(80, 1, 100, 1)), "Maximum click hold time");

        // Randomization Panel
        JPanel randPanel = new JPanel(new GridBagLayout());
        randPanel.setBorder(BorderFactory.createTitledBorder("Randomization"));
        gbc.gridy = row++;
        add(randPanel, gbc);

        gbcRandPanel(randPanel, 0, "Std Dev (%):", stdDevSlider = new JSlider(0, 50, 15), "Standard deviation for click variation");
        stdDevSlider.setMajorTickSpacing(5);
        stdDevSlider.setPaintTicks(true);
        stdDevSlider.setPaintLabels(true);
        gbcRandPanel(randPanel, 1, "Delay Dist:", gaussianRb = new JRadioButton("Gaussian", true), "Gaussian delay distribution");
        gbcRandPanel(randPanel, 2, "", uniformRb = new JRadioButton("Uniform", false), "Uniform delay distribution");
        gbcRandPanel(randPanel, 3, "", poissonRb = new JRadioButton("Poisson", false), "Poisson delay distribution");
        ButtonGroup distGroup = new ButtonGroup();
        distGroup.add(gaussianRb);
        distGroup.add(uniformRb);
        distGroup.add(poissonRb);
        gaussianRb.addActionListener(e -> config.delayDistribution = "Gaussian");
        uniformRb.addActionListener(e -> config.delayDistribution = "Uniform");
        poissonRb.addActionListener(e -> config.delayDistribution = "Poisson");

        // Click Type Panel
        JPanel clickPanel = new JPanel(new GridBagLayout());
        clickPanel.setBorder(BorderFactory.createTitledBorder("Click Type"));
        gbc.gridy = row++;
        add(clickPanel, gbc);

        gbcClickPanel(clickPanel, 0, "Type:", leftButtonRb = new JRadioButton("Left (PvP)", true), "Left click for PvP");
        gbcClickPanel(clickPanel, 1, "", rightButtonRb = new JRadioButton("Right (Bridging)", false), "Right click for bridging");
        ButtonGroup clickGroup = new ButtonGroup();
        clickGroup.add(leftButtonRb);
        clickGroup.add(rightButtonRb);
        leftButtonRb.addActionListener(e -> config.leftClick = true);
        rightButtonRb.addActionListener(e -> config.leftClick = false);

        // Mode Panel
        JPanel modePanel = new JPanel(new GridBagLayout());
        modePanel.setBorder(BorderFactory.createTitledBorder("Click Mode"));
        gbc.gridy = row++;
        add(modePanel, gbc);

        gbcModePanel(modePanel, 0, "Mode:", boostModeRb = new JRadioButton("Boost on Click", true), "Boost clicks on mouse press");
        gbcModePanel(modePanel, 1, "", keyHoldModeRb = new JRadioButton("Key Hold", false), "Auto-click while holding key");
        gbcModePanel(modePanel, 2, "", mouseHoldModeRb = new JRadioButton("Mouse Hold", false), "Auto-click while holding mouse");
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(boostModeRb);
        modeGroup.add(keyHoldModeRb);
        modeGroup.add(mouseHoldModeRb);
        boostModeRb.addActionListener(e -> { config.holdMode = false; config.mouseHoldMode = false; });
        keyHoldModeRb.addActionListener(e -> { config.holdMode = true; config.mouseHoldMode = false; });
        mouseHoldModeRb.addActionListener(e -> { config.mouseHoldMode = true; config.holdMode = false; });

        // Butterfly Panel
        JPanel butterflyPanel = new JPanel(new GridBagLayout());
        butterflyPanel.setBorder(BorderFactory.createTitledBorder("Butterfly Clicking"));
        gbc.gridy = row++;
        add(butterflyPanel, gbc);

        gbcButterflyPanel(butterflyPanel, 0, butterflyCheck = new JCheckBox("Enable Butterfly Mode", false), "Enable double-clicking for butterfly effect");
        butterflyCheck.addActionListener(e -> config.butterflyEnabled = butterflyCheck.isSelected());

        // Reach Panel
        JPanel reachPanel = new JPanel(new GridBagLayout());
        reachPanel.setBorder(BorderFactory.createTitledBorder("Reach Gainer"));
        gbc.gridy = row++;
        add(reachPanel, gbc);

        gbcReachPanel(reachPanel, 0, "W-Tap Dur (ms):", wTapSpinner = new JSpinner(new SpinnerNumberModel(50, 10, 200, 10)), "W-tap duration for reach");
        gbcReachPanel(reachPanel, 1, reachCheck = new JCheckBox("Enable Reach", false), "Enable W-tapping for reach advantage");
        reachCheck.addActionListener(e -> config.reachEnabled = reachCheck.isSelected());

        // Jitter Panel
        JPanel jitterPanel = new JPanel(new GridBagLayout());
        jitterPanel.setBorder(BorderFactory.createTitledBorder("Jitter Settings"));
        gbc.gridy = row++;
        add(jitterPanel, gbc);

        gbcJitterPanel(jitterPanel, 0, "Jitter Amp (px):", jitterAmpSlider = new JSlider(0, 20, 5), "Jitter movement amplitude in pixels");
        jitterAmpSlider.setMajorTickSpacing(1);
        jitterAmpSlider.setPaintTicks(true);
        jitterAmpSlider.setPaintLabels(true);
        gbcJitterPanel(jitterPanel, 1, jitterCheck = new JCheckBox("Enable Jitter", false), "Enable mouse jitter for natural movement");
        jitterCheck.addActionListener(e -> config.jitterEnabled = jitterCheck.isSelected());
        gbcJitterPanel(jitterPanel, 2, "Pattern:", randomJitterRb = new JRadioButton("Random", true), "Random jitter movement");
        gbcJitterPanel(jitterPanel, 3, "", circularJitterRb = new JRadioButton("Circular", false), "Circular jitter pattern");
        gbcJitterPanel(jitterPanel, 4, "", linearJitterRb = new JRadioButton("Linear", false), "Linear jitter pattern");
        gbcJitterPanel(jitterPanel, 5, "", figure8JitterRb = new JRadioButton("Figure-8", false), "Figure-8 jitter pattern");
        ButtonGroup jitterGroup = new ButtonGroup();
        jitterGroup.add(randomJitterRb);
        jitterGroup.add(circularJitterRb);
        jitterGroup.add(linearJitterRb);
        jitterGroup.add(figure8JitterRb);
        randomJitterRb.addActionListener(e -> config.jitterPattern = "Random");
        circularJitterRb.addActionListener(e -> config.jitterPattern = "Circular");
        linearJitterRb.addActionListener(e -> config.jitterPattern = "Linear");
        figure8JitterRb.addActionListener(e -> config.jitterPattern = "Figure8");

        // Breaks Panel
        JPanel breaksPanel = new JPanel(new GridBagLayout());
        breaksPanel.setBorder(BorderFactory.createTitledBorder("Break Settings"));
        gbc.gridy = row++;
        add(breaksPanel, gbc);

        gbcBreaksPanel(breaksPanel, 0, "Break Every:", breakEverySpinner = new JSpinner(new SpinnerNumberModel(50, 0, 200, 10)), "Clicks before a break");
        gbcBreaksPanel(breaksPanel, 1, "Break Min (ms):", breakMinSpinner = new JSpinner(new SpinnerNumberModel(500, 100, 5000, 100)), "Minimum break duration");
        gbcBreaksPanel(breaksPanel, 2, "Break Max (ms):", breakMaxSpinner = new JSpinner(new SpinnerNumberModel(2000, 100, 5000, 100)), "Maximum break duration");
        gbcBreaksPanel(breaksPanel, 3, breaksCheck = new JCheckBox("Enable Breaks", false), "Enable periodic breaks for human-like behavior");
        breaksCheck.addActionListener(e -> config.enableBreaks = breaksCheck.isSelected());

        // Minecraft Focus Panel
        JPanel focusPanel = new JPanel(new GridBagLayout());
        focusPanel.setBorder(BorderFactory.createTitledBorder("Game Focus"));
        gbc.gridy = row++;
        add(focusPanel, gbc);

        gbcFocusPanel(focusPanel, 0, minecraftFocusCheck = new JCheckBox("Only in MC (Win)", true), "Restrict clicks to Minecraft window (Windows only)");
        minecraftFocusCheck.addActionListener(e -> config.onlyInMinecraft = minecraftFocusCheck.isSelected());

        // Control Panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        gbc.gridy = row++;
        add(controlPanel, gbc);

        gbcControlPanel(controlPanel, 0, toggleButton = new JButton("Start"), "Toggle autoclicker (custom hotkey)");
        toggleButton.addActionListener(e -> toggle());
        gbcControlPanel(controlPanel, 1, new JLabel("Custom hotkey to toggle"), "Use configured hotkey to toggle autoclicker");
    }

    private void gbcProfilePanel(JPanel panel, int row, JComponent component, String tooltip) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST;
        panel.add(row == 0 ? new JLabel("Profile:") : new JLabel(), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        component.setToolTipText(tooltip);
        panel.add(component, gbc);
    }

    private void gbcHotkeyPanel(JPanel panel, int row, String label, JComponent component, String tooltip) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        component.setToolTipText(tooltip);
        panel.add(component, gbc);
    }

    private void gbcPremiumPanel(JPanel panel, int row, String label, JComponent component, String tooltip) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST;
        panel.add(label.isEmpty() ? new JLabel() : new JLabel(label), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        component.setToolTipText(tooltip);
        panel.add(component, gbc);
    }

    private void gbcCpsPanel(JPanel panel, int row, String label, JComponent component, String tooltip) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        component.setToolTipText(tooltip);
        panel.add(component, gbc);
    }

    private void gbcBurstPanel(JPanel panel, int row, String label, JComponent component, String tooltip) {
        gbcPremiumPanel(panel, row, label, component, tooltip);
    }

    private void gbcHoldPanel(JPanel panel, int row, String label, JComponent component, String tooltip) {
        gbcCpsPanel(panel, row, label, component, tooltip);
    }

    private void gbcRandPanel(JPanel panel, int row, String label, JComponent component, String tooltip) {
        gbcPremiumPanel(panel, row, label, component, tooltip);
    }

    private void gbcClickPanel(JPanel panel, int row, String label, JComponent component, String tooltip) {
        gbcPremiumPanel(panel, row, label, component, tooltip);
    }

    private void gbcModePanel(JPanel panel, int row, String label, JComponent component, String tooltip) {
        gbcPremiumPanel(panel, row, label, component, tooltip);
    }

    private void gbcButterflyPanel(JPanel panel, int row, JCheckBox checkBox, String tooltip) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        checkBox.setToolTipText(tooltip);
        panel.add(checkBox, gbc);
    }

    private void gbcReachPanel(JPanel panel, int row, String label, JComponent component, String tooltip) {
        gbcCpsPanel(panel, row, label, component, tooltip);
    }

    private void gbcJitterPanel(JPanel panel, int row, String label, JComponent component, String tooltip) {
        gbcPremiumPanel(panel, row, label, component, tooltip);
    }

    private void gbcBreaksPanel(JPanel panel, int row, String label, JComponent component, String tooltip) {
        gbcCpsPanel(panel, row, label, component, tooltip);
    }

    private void gbcFocusPanel(JPanel panel, int row, JCheckBox checkBox, String tooltip) {
        gbcButterflyPanel(panel, row, checkBox, tooltip);
    }

    private void gbcControlPanel(JPanel panel, int row, JComponent component, String tooltip) {
        gbcPremiumPanel(panel, row, "", component, tooltip);
    }

    private void activatePremium() {
        if ("grokpremium".equals(premiumKeyField.getText())) {
            premium = true;
            ((SpinnerNumberModel) minCpsSpinner.getModel()).setMaximum(30);
            ((SpinnerNumberModel) maxCpsSpinner.getModel()).setMaximum(30);
            recordMacroButton.setEnabled(true);
            playMacroButton.setEnabled(true);
            statsTimer = new Timer();
            statsTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    updateStats();
                }
            }, 1000, 1000);
            JOptionPane.showMessageDialog(this, "Premium activated! Enjoy higher CPS, stats, and macros.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid premium key", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStats() {
        if (running.get() && premium) {
            long sessionTime = System.currentTimeMillis() - sessionStartTime;
            double avgCPS = sessionTime > 0 ? (double) totalClicks / (sessionTime / 1000.0) : 0;
            statsLabel.setText(String.format("Stats: %d clicks, %.1f sec, %.2f CPS", totalClicks, sessionTime / 1000.0, avgCPS));
        } else {
            statsLabel.setText("Stats: N/A (Premium only)");
        }
    }

    private void toggleRecording() {
        if (!premium) {
            JOptionPane.showMessageDialog(this, "Macro recording requires premium activation.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean newRecording = !recording.get();
        recording.set(newRecording);
        if (newRecording) {
            macroEvents.clear();
            macroStartTime = System.currentTimeMillis();
            recordMacroButton.setText("Stop Recording");
            playMacroButton.setEnabled(false);
            JOptionPane.showMessageDialog(this, "Recording started. Perform mouse clicks.", "Macro", JOptionPane.INFORMATION_MESSAGE);
        } else {
            recordMacroButton.setText("Record Macro");
            playMacroButton.setEnabled(!macroEvents.isEmpty());
            JOptionPane.showMessageDialog(this, "Recording stopped. " + macroEvents.size() + " events recorded.", "Macro", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void playMacro() {
        if (!premium) {
            JOptionPane.showMessageDialog(this, "Macro playback requires premium activation.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (macroEvents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No macro recorded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!running.get()) {
            toggle();
        }
        executor.submit(() -> {
            try {
                for (MacroEvent event : macroEvents) {
                    if (!running.get() || Thread.currentThread().isInterrupted()) return;
                    if (config.onlyInMinecraft && !isMinecraftFocused()) continue;
                    int buttonMask = event.button == NativeMouseEvent.BUTTON1 ? 
                        java.awt.event.InputEvent.BUTTON1_DOWN_MASK : 
                        java.awt.event.InputEvent.BUTTON3_DOWN_MASK;
                    if (event.isPress) {
                        robot.mousePress(buttonMask);
                    } else {
                        robot.mouseRelease(buttonMask);
                    }
                    totalClicks += event.isPress ? 1 : 0;
                    if (event != macroEvents.get(macroEvents.size() - 1)) {
                        long nextTime = macroEvents.get(macroEvents.indexOf(event) + 1).time;
                        Thread.sleep(nextTime - event.time);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                simulating.set(false);
            }
        });
    }

    private void saveProfile() {
        updateConfigFromGUI();
        String profileName = JOptionPane.showInputDialog(this, "Enter profile name:", "Save Profile", JOptionPane.PLAIN_MESSAGE);
        if (profileName != null && !profileName.trim().isEmpty()) {
            Config profileConfig = config.clone();
            profileConfig.toggleKeyCode = toggleKeyCode;
            profileConfig.holdKeyCode = holdKeyCode;
            profiles.put(profileName, profileConfig);
            profileComboBox.addItem(profileName);
            saveCustomProfiles();
            profileComboBox.setSelectedItem(profileName);
        }
    }

    private void applyProfile() {
        String selectedProfile = (String) profileComboBox.getSelectedItem();
        if (selectedProfile != null) {
            Config selectedConfig = profiles.get(selectedProfile);
            if (selectedConfig != null) {
                config.copyFrom(selectedConfig);
                toggleKeyCode = selectedConfig.toggleKeyCode;
                holdKeyCode = selectedConfig.holdKeyCode;
                toggleKeySpinner.setValue(toggleKeyCode);
                holdKeySpinner.setValue(holdKeyCode);
                ListenerRegistry.updateToggleKey(toggleKeyCode);
                ListenerRegistry.updateHoldKey(holdKeyCode);
                applyConfigToGUI();
            }
        }
    }

    private void applyConfigToGUI() {
        minCpsSpinner.setValue(config.minCPS);
        maxCpsSpinner.setValue(config.maxCPS);
        minBurstSpinner.setValue(config.minBurst);
        maxBurstSpinner.setValue(config.maxBurst);
        fixedBurstRb.setSelected(!config.randomBurst);
        randomBurstRb.setSelected(config.randomBurst);
        minHoldSpinner.setValue(config.minHoldTime);
        maxHoldSpinner.setValue(config.maxHoldTime);
        stdDevSlider.setValue((int) (config.stdDevPercent * 100));
        gaussianRb.setSelected("Gaussian".equals(config.delayDistribution));
        uniformRb.setSelected("Uniform".equals(config.delayDistribution));
        poissonRb.setSelected("Poisson".equals(config.delayDistribution));
        leftButtonRb.setSelected(config.leftClick);
        rightButtonRb.setSelected(!config.leftClick);
        boostModeRb.setSelected(!config.holdMode && !config.mouseHoldMode);
        keyHoldModeRb.setSelected(config.holdMode);
        mouseHoldModeRb.setSelected(config.mouseHoldMode);
        butterflyCheck.setSelected(config.butterflyEnabled);
        reachCheck.setSelected(config.reachEnabled);
        wTapSpinner.setValue(config.wTapDuration);
        jitterCheck.setSelected(config.jitterEnabled);
        jitterAmpSlider.setValue(config.jitterAmplitude);
        randomJitterRb.setSelected("Random".equals(config.jitterPattern));
        circularJitterRb.setSelected("Circular".equals(config.jitterPattern));
        linearJitterRb.setSelected("Linear".equals(config.jitterPattern));
        figure8JitterRb.setSelected("Figure8".equals(config.jitterPattern));
        breaksCheck.setSelected(config.enableBreaks);
        breakEverySpinner.setValue(config.breakEvery);
        breakMinSpinner.setValue(config.breakMin);
        breakMaxSpinner.setValue(config.breakMax);
        minecraftFocusCheck.setSelected(config.onlyInMinecraft);
    }

    private void toggle() {
        boolean newRunning = !running.get();
        running.set(newRunning);
        if (newRunning) {
            updateConfigFromGUI();
            sessionStartTime = System.currentTimeMillis();
            totalClicks = 0;
            SwingUtilities.invokeLater(() -> toggleButton.setText("Stop"));
        } else {
            keyHolding.set(false);
            mouseHolding.set(false);
            SwingUtilities.invokeLater(() -> toggleButton.setText("Start"));
        }
    }

    private void updateConfigFromGUI() {
        config.minCPS = (Integer) minCpsSpinner.getValue();
        config.maxCPS = (Integer) maxCpsSpinner.getValue();
        if (config.minCPS > config.maxCPS) {
            int temp = config.minCPS;
            config.minCPS = config.maxCPS;
            config.maxCPS = temp;
        }
        config.minBurst = (Integer) minBurstSpinner.getValue();
        config.maxBurst = (Integer) maxBurstSpinner.getValue();
        if (config.minBurst > config.maxBurst) {
            int temp = config.minBurst;
            config.minBurst = config.maxBurst;
            config.maxBurst = temp;
        }
        config.randomBurst = randomBurstRb.isSelected();
        config.minHoldTime = (Integer) minHoldSpinner.getValue();
        config.maxHoldTime = (Integer) maxHoldSpinner.getValue();
        if (config.minHoldTime > config.maxHoldTime) {
            int temp = config.minHoldTime;
            config.minHoldTime = config.maxHoldTime;
            config.maxHoldTime = temp;
        }
        config.stdDevPercent = stdDevSlider.getValue() / 100.0;
        config.delayDistribution = gaussianRb.isSelected() ? "Gaussian" : uniformRb.isSelected() ? "Uniform" : "Poisson";
        config.leftClick = leftButtonRb.isSelected();
        config.holdMode = keyHoldModeRb.isSelected();
        config.mouseHoldMode = mouseHoldModeRb.isSelected();
        config.butterflyEnabled = butterflyCheck.isSelected();
        config.reachEnabled = reachCheck.isSelected();
        config.wTapDuration = (Integer) wTapSpinner.getValue();
        config.jitterEnabled = jitterCheck.isSelected();
        config.jitterAmplitude = jitterAmpSlider.getValue();
        config.jitterPattern = randomJitterRb.isSelected() ? "Random" : 
                               circularJitterRb.isSelected() ? "Circular" :
                               linearJitterRb.isSelected() ? "Linear" : "Figure8";
        config.enableBreaks = breaksCheck.isSelected();
        config.breakEvery = (Integer) breakEverySpinner.getValue();
        config.breakMin = (Integer) breakMinSpinner.getValue();
        config.breakMax = (Integer) breakMaxSpinner.getValue();
        if (config.breakMin > config.breakMax) {
            int temp = config.breakMin;
            config.breakMin = config.breakMax;
            config.breakMax = temp;
        }
        config.onlyInMinecraft = minecraftFocusCheck.isSelected();
        config.toggleKeyCode = toggleKeyCode;
        config.holdKeyCode = holdKeyCode;
    }

    void handleKeyPress(NativeKeyEvent e) {
        if (e.getKeyCode() == toggleKeyCode) {
            toggle();
        }
        if (running.get() && config.holdMode && e.getKeyCode() == holdKeyCode) {
            keyHolding.set(true);
            if (simulating.compareAndSet(false, true)) {
                executor.submit(() -> clicker.autoClickWhileHolding(keyHolding::get));
            }
        }
    }

    void handleKeyRelease(NativeKeyEvent e) {
        if (running.get() && config.holdMode && e.getKeyCode() == holdKeyCode) {
            keyHolding.set(false);
        }
    }

    void handleMousePress(NativeMouseEvent e) {
        if (running.get() && config.mouseHoldMode) {
            int button = config.leftClick ? NativeMouseEvent.BUTTON1 : NativeMouseEvent.BUTTON3;
            if (e.getButton() == button) {
                mouseHolding.set(true);
                if (simulating.compareAndSet(false, true)) {
                    executor.submit(() -> clicker.autoClickWhileHolding(mouseHolding::get));
                }
            }
        }
        if (recording.get()) {
            macroEvents.add(new MacroEvent(System.currentTimeMillis() - macroStartTime, true, e.getButton()));
        }
    }

    void handleMouseRelease(NativeMouseEvent e) {
        int button = config.leftClick ? NativeMouseEvent.BUTTON1 : NativeMouseEvent.BUTTON3;
        if (running.get() && e.getButton() == button) {
            if (config.mouseHoldMode) {
                mouseHolding.set(false);
            } else if (!config.holdMode && !config.mouseHoldMode) {
                if (simulating.compareAndSet(false, true)) {
                    executor.submit(clicker::boostClicks);
                }
            }
        }
        if (recording.get()) {
            macroEvents.add(new MacroEvent(System.currentTimeMillis() - macroStartTime, false, e.getButton()));
        }
    }

    @Override
    public void dispose() {
        running.set(false);
        keyHolding.set(false);
        mouseHolding.set(false);
        simulating.set(false);
        recording.set(false);
        if (statsTimer != null) {
            statsTimer.cancel();
        }
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        ListenerRegistry.unregisterListeners();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BedwarsGainer app = new BedwarsGainer();
            app.setVisible(true);
        });
    }

    // Clicker Class
    class Clicker {
        private final Config config;
        private final Random random = new Random();
        private final Robot robot = BedwarsGainer.this.robot;
        private int clickCount = 0;

        Clicker(Config config) {
            this.config = config;
        }

        void boostClicks() {
            try {
                double avgCPS = (config.minCPS + config.maxCPS) / 2.0;
                int burstLength = config.randomBurst ? config.minBurst + random.nextInt(config.maxBurst - config.minBurst + 1) : config.minBurst;
                for (int i = 0; i < burstLength; i++) {
                    if (!BedwarsGainer.this.running.get() || Thread.currentThread().isInterrupted()) return;
                    performClick();
                    if (i < burstLength - 1) {
                        Thread.sleep(getDelay(avgCPS));
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                BedwarsGainer.this.simulating.set(false);
            }
        }

        void autoClickWhileHolding(Supplier<Boolean> isHolding) {
            try {
                double avgCPS = (config.minCPS + config.maxCPS) / 2.0;
                while (BedwarsGainer.this.running.get() && isHolding.get() && !Thread.currentThread().isInterrupted()) {
                    performClick();
                    Thread.sleep(getDelay(avgCPS));
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                BedwarsGainer.this.simulating.set(false);
            }
        }

        private void performClick() throws InterruptedException {
            if (config.onlyInMinecraft && !isMinecraftFocused()) return;

            int buttonMask = config.leftClick ? java.awt.event.InputEvent.BUTTON1_DOWN_MASK : java.awt.event.InputEvent.BUTTON3_DOWN_MASK;
            Point originalPos = MouseInfo.getPointerInfo().getLocation();

            if (config.jitterEnabled) {
                applyJitterPattern(originalPos);
            }

            int clicks = config.butterflyEnabled ? 2 : 1;
            for (int c = 0; c < clicks; c++) {
                robot.mousePress(buttonMask);
                int holdTime = config.minHoldTime + random.nextInt(config.maxHoldTime - config.minHoldTime + 1);
                Thread.sleep(holdTime);
                robot.mouseRelease(buttonMask);
                if (c < clicks - 1) {
                    Thread.sleep(10 + random.nextInt(20));
                }
                totalClicks++;
            }

            if (config.jitterEnabled) {
                robot.mouseMove(originalPos.x, originalPos.y);
            }

            if (config.reachEnabled) {
                robot.keyPress(java.awt.event.KeyEvent.VK_W);
                Thread.sleep(config.wTapDuration);
                robot.keyRelease(java.awt.event.KeyEvent.VK_W);
            }

            if (config.enableBreaks) {
                clickCount++;
                if (clickCount >= config.breakEvery) {
                    int breakDuration = config.breakMin + random.nextInt(config.breakMax - config.breakMin + 1);
                    Thread.sleep(breakDuration);
                    clickCount = 0;
                }
            }
        }

        private void applyJitterPattern(Point originalPos) throws InterruptedException {
            int amp = config.jitterAmplitude;
            switch (config.jitterPattern) {
                case "Random":
                    int dx = random.nextInt(amp * 2) - amp;
                    int dy = random.nextInt(amp * 2) - amp;
                    robot.mouseMove(originalPos.x + dx, originalPos.y + dy);
                    break;
                case "Circular":
                    double angle = random.nextDouble() * 2 * Math.PI;
                    int radius = random.nextInt(amp) + 1;
                    int dxCirc = (int) (radius * Math.cos(angle));
                    int dyCirc = (int) (radius * Math.sin(angle));
                    robot.mouseMove(originalPos.x + dxCirc, originalPos.y + dyCirc);
                    break;
                case "Linear":
                    int direction = random.nextInt(4);
                    switch (direction) {
                        case 0: robot.mouseMove(originalPos.x, originalPos.y - amp); break;
                        case 1: robot.mouseMove(originalPos.x, originalPos.y + amp); break;
                        case 2: robot.mouseMove(originalPos.x - amp, originalPos.y); break;
                        case 3: robot.mouseMove(originalPos.x + amp, originalPos.y); break;
                    }
                    break;
                case "Figure8":
                    int phase = random.nextInt(2);
                    angle = random.nextDouble() * Math.PI;
                    radius = random.nextInt(amp / 2) + 1;
                    int dx = (int) (radius * Math.cos(angle + phase * Math.PI));
                    int dy = (int) (radius * Math.sin(2 * (angle + phase * Math.PI)));
                    robot.mouseMove(originalPos.x + dx, originalPos.y + dy);
                    break;
            }
            Thread.sleep(5 + random.nextInt(10));
        }

        private int getDelay(double cps) {
            double meanDelay = 1000.0 / cps;
            double minDelay = 1000.0 / config.maxCPS;
            double maxDelay = 1000.0 / config.minCPS;
            double delay;
            switch (config.delayDistribution) {
                case "Uniform":
                    delay = minDelay + random.nextDouble() * (maxDelay - minDelay);
                    break;
                case "Poisson":
                    delay = -Math.log(1 - random.nextDouble()) * meanDelay;
                    break;
                case "Gaussian":
                default:
                    double stdDev = meanDelay * config.stdDevPercent;
                    delay = meanDelay + (stdDev * random.nextGaussian());
                    break;
            }
            delay = Math.max(minDelay, Math.min(maxDelay, delay));
            return Math.max(1, (int) delay);
        }
    }

    private boolean isMinecraftFocused() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) return true;

        try {
            User32 user32 = User32.INSTANCE;
            HWND hwnd = user32.GetForegroundWindow();
            if (hwnd == null) return false;
            char[] buffer = new char[1024];
            user32.GetWindowText(hwnd, buffer, 1024);
            String title = Native.toString(buffer).toLowerCase();
            return title.contains("minecraft");
        } catch (Exception e) {
            System.err.println("Error checking window focus: " + e.getMessage());
            return true;
        }
    }

    // Listener Registry
    static class ListenerRegistry {
        private static NativeMouseInputListener mouseListener;
        private static NativeKeyListener keyListener;
        private static int currentToggleKey = NativeKeyEvent.VC_F1;
        private static int currentHoldKey = NativeKeyEvent.VC_F2;

        static void registerListeners(BedwarsGainer app, int toggleKey, int holdKey) {
            currentToggleKey = toggleKey;
            currentHoldKey = holdKey;

            mouseListener = new NativeMouseInputListener() {
                @Override
                public void nativeMousePressed(NativeMouseEvent e) {
                    app.handleMousePress(e);
                }

                @Override
                public void nativeMouseReleased(NativeMouseEvent e) {
                    app.handleMouseRelease(e);
                }

                @Override
                public void nativeMouseClicked(NativeMouseEvent e) {}
                @Override
                public void nativeMouseMoved(NativeMouseEvent e) {}
                @Override
                public void nativeMouseDragged(NativeMouseEvent e) {}
            };

            keyListener = new NativeKeyListener() {
                @Override
                public void nativeKeyPressed(NativeKeyEvent e) {
                    app.handleKeyPress(e);
                }

                @Override
                public void nativeKeyReleased(NativeKeyEvent e) {
                    app.handleKeyRelease(e);
                }

                @Override
                public void nativeKeyTyped(NativeKeyEvent e) {}
            };

            try {
                GlobalScreen.registerNativeHook();
            } catch (NativeHookException ex) {
                System.err.println("Failed to register native hook: " + ex.getMessage());
                JOptionPane.showMessageDialog(null, "Failed to register native hook: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            GlobalScreen.addNativeMouseListener(mouseListener);
            GlobalScreen.addNativeKeyListener(keyListener);
        }

        static void updateToggleKey(int newKey) {
            currentToggleKey = newKey;
        }

        static void updateHoldKey(int newKey) {
            currentHoldKey = newKey;
        }

        static void unregisterListeners() {
            if (GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.removeNativeMouseListener(mouseListener);
                GlobalScreen.removeNativeKeyListener(keyListener);
                try {
                    GlobalScreen.unregisterNativeHook();
                } catch (NativeHookException ex) {
                    System.err.println("Failed to unregister native hook: " + ex.getMessage());
                }
            }
        }
    }
}

// Macro Event Class
class MacroEvent {
    long time;
    boolean isPress;
    int button;

    MacroEvent(long time, boolean isPress, int button) {
        this.time = time;
        this.isPress = isPress;
        this.button = button;
    }
}
