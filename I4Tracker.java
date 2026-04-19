// Update regex pattern to match specific player achievements

// Original regex pattern
String regexPattern = "(.*) completed a device! \((\d+/\d+)\)";

// Updated regex pattern to include Berserker players
String updatedRegexPattern = "(Berserker \d+) completed a device! \((\d+/\d+)\)";

// Add logic to extract PlayerName and achievement
Pattern pattern = Pattern.compile(updatedRegexPattern);
Matcher matcher = pattern.matcher(input);

if (matcher.find()) {
    String playerName = matcher.group(1);
    String achievement = matcher.group(2);
    // Handle the achievement logic here
}