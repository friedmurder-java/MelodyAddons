import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class I4Tracker {

    private static final Pattern COMPLETION_PATTERN = Pattern.compile("(\\w+) completed a device! \((\\d+)/(\\d+)\)");

    public static void trackCompletion(String message) {
        Matcher matcher = COMPLETION_PATTERN.matcher(message);
        if (matcher.find()) {
            String playerName = matcher.group(1);
            String completed = matcher.group(2);
            String total = matcher.group(3);
            // Handle player detection and completion tracking
            System.out.println(playerName + " has completed " + completed + " out of " + total + ".");
        }
    }

    public static boolean isBerserker(String playerTag) {
        return playerTag.contains("[B]");
    }
}