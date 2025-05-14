package Main;

import config.Environment;
import config.EnvironmentManager;
import config.RemoteConfig;
import config.ConfigurationManager;
import config.ConfigurationManagerRemote;
import java.net.MalformedURLException;
import ui.ArabicNotepadUI;
import ui.RemoteArabicNotepadUI;

import javax.swing.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        System.setProperty("java.security.manager", "com.sun.security.policy.PolicyFile");
        System.setProperty("java.security.policy", "src/main/resources/remote/rmi.policy");

        Environment env = EnvironmentManager.getCurrentEnvironment();
        logger.info("Detected environment: {}", env);
        try {
            ConfigurationManager.getInstance(env);
        } catch (RemoteException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            if (env == Environment.REMOTE) {
                startRemoteClient();
            } else {
                startLocalClient(env);
            }
        } catch (Exception e) {
            logger.error("Application failed to start", e);
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage(), "Application Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void startLocalClient(Environment env) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            logger.info("Set Look and Feel to system default.");
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            logger.error("Failed to set Look and Feel", e);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                ArabicNotepadUI ui = new ArabicNotepadUI();
                ui.pack();
                ui.setLocationRelativeTo(null);
                ui.setVisible(true);
                logger.info("ArabicNotepadUI launched.");
            } catch (Exception e) {
                logger.error("Failed to launch ArabicNotepadUI.", e);
                JOptionPane.showMessageDialog(null, "Failed to launch the application.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void startRemoteClient() {
        try {
            RemoteConfig remoteConfig = new RemoteConfig();
            String rmiHost = remoteConfig.getRmiHost();
            int rmiPort = remoteConfig.getRmiPort();
            String rmiUrl = String.format("rmi://%s:%d/ConfigurationManager", rmiHost, rmiPort);

            ConfigurationManagerRemote remoteConfigManager = (ConfigurationManagerRemote) Naming.lookup(rmiUrl);
            logger.info("Successfully connected to remote ConfigurationManager at {}", rmiUrl);

            Environment env = EnvironmentManager.getCurrentEnvironment();
            ConfigurationManager.getInstance(env);
            
            RemoteArabicNotepadUI client = new RemoteArabicNotepadUI(remoteConfig);
            //client.start();
            logger.info("RemoteArabicNotepadUI initialized successfully.");

        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            logger.error("Failed to connect to remote ConfigurationManager", e);
            JOptionPane.showMessageDialog(null, "Error connecting to remote server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
