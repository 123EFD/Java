import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;


public class GitHubActivity {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java GitHubActivity <username>");
            return;
        }

        String username = args[0];
        String apiUrl = "https://api.github.com/users/" + username + "/events";

        try {
            URI uri = URI.create(apiUrl);
            URL url = uri.toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Java CLI");

            int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                System.out.println("Failed to fetch data. HTTP response code: " + responseCode);
                if (responseCode == 404) {
                    System.out.println("Error: User not found.");
                }
                return;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder responseJson = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                responseJson.append(inputLine);
            }
            in.close();

            String json = responseJson.toString();
            displayEvents(json);
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    private static void displayEvents(String json) {
    if (!json.startsWith("[")) {
        System.out.println("Unexpected JSON format.");
        return;
    }

    System.out.println("Raw JSON:\n" + json);  // 👈 TEMP: See raw data

    String[] events = json.split("\\},\\{");
    int count = 0;

    for (String event : events) {
        System.out.println("DEBUG: Raw event: " + event);  // 👈 TEMP: Check event text

        String type = extractField(event, "\"type\":\"", "\"");
        String repo = extractField(event, "\"repo\":\\{\"id\":\\d+,\"name\":\"", "\"");

        if (type != null && repo != null) {
            System.out.println("Event type: " + type + ", repo: " + repo);  // 👈 TEMP: Log what we got

            String activity = null;

            if (type.equals("PushEvent")) {
                int commits = countCommits(event);
                activity = "Pushed " + commits + " commit" + (commits == 1 ? "" : "s") + " to " + repo;
            } else {
                activity = interpretType(type, repo);
            }

            if (activity != null) {
                System.out.println("- " + activity);
                count++;
            }
        }

        if (count >= 10) break;
    }

    if (count == 0) {
        System.out.println("No public activity found.");
    }
}


    private static int countCommits(String eventJson) {
    try {
        int start = eventJson.indexOf("\"commits\":[");
        if (start == -1) return 0;

        int end = eventJson.indexOf("]", start);
        if (end == -1) return 0;

        String commitsArray = eventJson.substring(start + 11, end); // skip "commits":[
        if (commitsArray.trim().isEmpty()) return 0;

        // Count how many '{' are in the array to determine number of commits
        int count = 0;
        for (int i = 0; i < commitsArray.length(); i++) {
            if (commitsArray.charAt(i) == '{') count++;
        }
        return count;
    } catch (Exception e) {
        return 0;
    }
}

    private static String extractField(String text, String prefix, String suffix) {
        try {
            int start = text.indexOf(prefix);
            if (start == -1) return null;
            start += prefix.length();
            int end = text.indexOf(suffix, start);
            if (end == -1) return null;
            return text.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private static String interpretType(String type, String repo) {
        switch (type) {
            case "PushEvent":
                return "Pushed commits to " + repo;
            case "IssuesEvent":
                return "Opened a new issue in " + repo;
            case "WatchEvent":
                return "Starred " + repo;
            case "CreateEvent":
                return "Created something in " + repo;
            case "ForkEvent":
                return "Forked " + repo;
            case "PullRequestEvent":
                return "Opened a pull request in " + repo;
            default:
                return null; // Skip other events
        }
    }
}
