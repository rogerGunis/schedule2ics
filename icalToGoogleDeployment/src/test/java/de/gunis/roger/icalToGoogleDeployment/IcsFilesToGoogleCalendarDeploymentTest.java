package de.gunis.roger.icalToGoogleDeployment;

import org.junit.Ignore;
import org.junit.Test;

import java.net.URISyntaxException;

public class IcsFilesToGoogleCalendarDeploymentTest {

    @Ignore
    @Test
    public void runDeployment() {
        System.setProperty("ACCOUNT_USER", "");
        IcsFilesToGoogleCalendarDeployment icsTest = new IcsFilesToGoogleCalendarDeployment();
        icsTest.setInputDirectoryIcsFiles(System.setProperty("INPUT_DIR_ICS", ""));
        icsTest.setLoggingLevel("DEBUG");
        try {
            icsTest.setApiKeyFile(IcsFilesToGoogleCalendarDeployment.class.getResource("/client_secrets_apiKey.json").toURI().getPath());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        icsTest.runDeployment();

    }
}