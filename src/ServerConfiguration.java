import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * A Java class that manages server configurations loaded from a file.
 */
public class ServerConfiguration {
    private static final Path DEFAULT_ROOT = Paths.get(System.getProperty("user.home"),"/www/lab/html/");
    private static final String DEFAULT_CONFIG_FILE = "config.ini";
    private static final String DEFAULT_INDEX_PAGE = "index.html";
    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_MAX_THREADS = 10;

    private int port = DEFAULT_PORT;
    private int maxThreads = DEFAULT_MAX_THREADS;
    private Path root = DEFAULT_ROOT;
    private String defaultPage = DEFAULT_ROOT.resolve(DEFAULT_INDEX_PAGE).toString();

    /**
     * Constructs a ServerConfiguration object with default settings.
     */
    public ServerConfiguration() {
        setDefaultPage(DEFAULT_ROOT.resolve(DEFAULT_INDEX_PAGE).toString());
    }

    /**
     * Constructs a ServerConfiguration object with custom settings.
     *
     * @param root - the root directory path
     * @param port - the server port number
     * @param defaultPage - the default page path
     * @param maxThreads - the maximum number of server threads
     */
    public ServerConfiguration(String root, int port, String defaultPage, int maxThreads) {
        setRoot(root);
        setDefaultPage(defaultPage);
        setPort(port);
        setMaxThreads(maxThreads);
    }

    public Path getRoot() {
        return root;
    }

    public void setRoot(String root) {
        if (!root.isEmpty()) {
            this.root = Paths.get(root);
        } else {
            throw new IllegalArgumentException("Missing root path!");
        }
    }

    public String getDefaultPage() {
        return defaultPage;
    }

    private void setDefaultPage(String defaultPage) {
        if (!defaultPage.isEmpty()) {
            this.defaultPage = defaultPage;
        } else {
            throw new IllegalArgumentException("Missing default page path!");
        }
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    private void setMaxThreads(int maxThreads) {
        if (maxThreads >= 1) {
            this.maxThreads = maxThreads;
        } else {
            throw new IllegalArgumentException("Illegal number of threads (must be at least 1).");
        }
    }

    public int getPort() {
        return port;
    }

    private void setPort(int port) {
        if ( 0 <= port && port <= 65535 ) {
            this.port = port;
        } else {
            throw new IllegalArgumentException("Illegal port number!");
        }
    }

    /**
     * Loads the server configurations from a file if it exists, otherwise creates and saves it.
     *
     * @param configFilePath - the path to the configuration file
     * @return the ServerConfiguration loaded from the configuration file
     */
    public static ServerConfiguration loadConfig(String configFilePath) {
        ServerConfiguration configFromFile = new ServerConfiguration();
        File configFile = new File(configFilePath);

        if (configFile.exists()) {
            configFromFile = updateConfigurationFromFile(configFilePath);
        } else {
            configFromFile.generateConfigFile();
        }

        return configFromFile;
    }

    /**
     * Reads properties from a file and updates the server configuration based on them.
     *
     * @param configFilePath - config.ini path.
     * @return - a ServerConfiguration object updated from the file
     */
    private static ServerConfiguration updateConfigurationFromFile(String configFilePath) {
        ServerConfiguration configFromFile = new ServerConfiguration();
        FileInputStream inputFromIni = null;

        try {
            Properties properties = new Properties();
            inputFromIni = new FileInputStream(configFilePath);
            properties.load(inputFromIni);

            configFromFile.setPort(Integer.parseInt(properties.getProperty("port", String.valueOf(DEFAULT_PORT))));
            configFromFile.setRoot(properties.getProperty("root", DEFAULT_ROOT.toString()).trim());
            configFromFile.setDefaultPage(properties.getProperty("defaultPage", configFromFile.getDefaultPage()));
            configFromFile.setMaxThreads(Integer.parseInt(properties.getProperty("maxThreads", String.valueOf(DEFAULT_MAX_THREADS))));

        } catch (IOException | NumberFormatException e) {
            System.out.println("Failed to generate config file");
        } finally {
            if (inputFromIni != null) {
                try {
                    inputFromIni.close();
                } catch (IOException e) {
                    System.out.println("Failed to generate config file");
                }
            }
        }

        return configFromFile;
    }

    /**
     * Generates a config.ini file in the root directory.
     */
    private void generateConfigFile() {
        Path filePath = Path.of(DEFAULT_CONFIG_FILE);
        Properties properties = new Properties();

        properties.setProperty("port", String.valueOf(this.port));
        properties.setProperty("root", String.valueOf(this.root));
        properties.setProperty("defaultPage", this.defaultPage);
        properties.setProperty("maxThreads", String.valueOf(this.maxThreads));

        try (FileOutputStream output = new FileOutputStream(filePath.toString())) {
            properties.store(output, "Server Configuration");
        } catch (IOException e) {
            System.out.println("Failed to generate config file");
        }
    }
}
