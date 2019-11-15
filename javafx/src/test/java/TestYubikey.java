import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Test code used to load pw from yubikey
 * On ubuntu I needed:
 *   sudo apt install pcsc-tools
 *   sudo apt install opensc
 */
public class TestYubikey {
    @Test
    public void testReadKey() {
        ProcessBuilder pb = new ProcessBuilder();
        pb.environment().put("LD_LIBRARY_PATH", "/usr/local/lib");
        ArrayList<String> command = new ArrayList<>();

        command.add("/usr/local/bin/yubico-piv-tool");
        command.add("-a");
        command.add("read-object");
        command.add("--id");
        command.add("0x5fc10d");
        System.out.printf("Running %s\n", command);
        pb.command(command);
        String data = null;
        int ok = 0;
        try {
            System.out.printf("Starting %s\n", pb);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            data = reader.readLine();
            ok = process.waitFor();
            System.out.printf("yubico-piv-tool exit status: %d\n", ok);
            if(ok == 0) {
                System.out.printf("Read %d bytes of data\n", data.length());
            }
        } catch (IOException |InterruptedException e) {
            System.out.printf("Yubikey Error, %s", e);
        }

    }
}
