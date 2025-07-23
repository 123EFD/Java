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
        String[] events = json.split("\\},\\{");
        int count = 0;

        for (String event : events) {
    String type = extractField(event, "\"type\":\"", "\"");
    String repo = extractField(event, "\"repo\":\\{\"id\":\\d+,\"name\":\"", "\"");

    if (type != null && repo != null) {
        String activity = null;

        if (type.equals("PushEvent")) {
            String commitCountStr = extractField(event, "\"commits\":\\[", "]");
            int commitCount = commitCountStr == null ? 0 : commitCountStr.split("\\},\\{").length;
            activity = "Pushed " + commitCount + " commit" + (commitCount > 1 ? "s" : "") + " to " + repo;
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
