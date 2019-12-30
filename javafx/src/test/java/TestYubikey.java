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
    //static String CMD = "/usr/local/bin/yubico-piv-tool";
    static String CMD = "/Users/starksm/bin/yubico-piv-tool-1.7.0/bin/yubico-piv-tool";

    @Test
    public void testReadKey() {
        ProcessBuilder pb = new ProcessBuilder();
        pb.environment().put("LD_LIBRARY_PATH", "/usr/local/lib");
        ArrayList<String> command = new ArrayList<>();

        command.add(CMD);
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
                System.out.println(data);
                // Convert from hex to decimal string
                StringBuilder tmp = new StringBuilder();
                for (int n = 0; n < data.length(); n += 2) {
                    String digits = data.substring(n, n + 2);
                    int value = Integer.parseInt(digits, 16);
                    tmp.append((char) value);
                }
                tmp.append(tmp.toString());
                System.out.println(tmp.toString());
            }
        } catch (IOException |InterruptedException e) {
            System.out.printf("Yubikey Error, %s", e);
        }

    }
}
