import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class CrackMe {

    public static void main(String[] args) throws Exception {
        long sleepy = 1;
        while (true) {
            // ANTI-BRUTE FORCE: Increases wait time every loop. 
            // This makes running the script as-is nearly impossible to finish.
            // Thread.sleep(sleepy++);

            // SEED GENERATION: Picks a random starting point for the PRNG.
            long seed = new Random().nextLong();
            Random rnd = new Random(seed);
            final int range = 26;
            // String prefix = "";

            // // STAGE 1: Generate an 80-character string based on the seed.
            // for (int i = 0; i < 80; i++) {
            //     int v = rnd.nextInt(range) + 1; // Generates 1-26
            //     char c = (char) ('`' + v);      // Maps 1-26 to 'a'-'z'
            //     prefix += c;
            // }

            // STAGE 2: The "s" characters are replaced with delimiters.
            // The resulting string MUST be split into exactly 3 parts by the letter 's'.
            // prefix = prefix.replace("s", "-");
            // String[] preconditions = prefix.split("-");

            // if (preconditions.length != 3) {
            //     continue;
            // }

            // // STAGE 3: Hardcoded checks. 
            // // These strings are the "lock." The seed must generate these exact sequences.
            // if (!preconditions[0].equals("xgjv")) continue;
            // if (!preconditions[1].equals("xceg")) continue;
            // if (!preconditions[2].equals("rtakdmavlctjtnfdwicmliizylrfqynpbolgexazdutylgzrbyrcybzgdmjydylzhrbiml")) continue;

            // STAGE 4: If the above matches, use the SAME random object to generate a suffix.
            String suffix = "";
            for (int i = 0; i < 20; i++) {
                int v = rnd.nextInt(range) + 1;
                char c = (char) ('`' + v);
                suffix += c;
            }

            // STAGE 5: Cryptographic check.
            // The suffix (with 's' replaced by '-') must have a SHA-256 hash starting with "e9".
            suffix = suffix.replace("s", "-");
            if (SHA256(suffix).startsWith("e9")) {
                System.out.println("You unlocked the challenge - This is your key [" + suffix.substring(0, 15) + "]");
                break;
            }
        }
    }

    // Standard SHA-256 Hashing helper
    private static String SHA256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}