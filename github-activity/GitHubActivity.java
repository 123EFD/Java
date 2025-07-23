import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

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
            StringBuilder responseJson = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                responseJson.append(inputLine);
            }
            in.close();

            displayEvents(responseJson.toString());
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    private static void displayEvents(String json) {
        int eventStart = 0;
        int count = 0;

        while ((eventStart = json.indexOf("{", eventStart)) != -1 && count < 10) {
            int eventEnd = findMatchingBrace(json, eventStart);
            if (eventEnd == -1) break;

            String eventJson = json.substring(eventStart, eventEnd + 1);
            String type = extractValue(eventJson, "\"type\":\"", "\"");
            String repo = extractValue(eventJson, "\"repo\":\\{[^}]*\"name\":\"", "\"");

            if (type != null && repo != null) {
                String activity = null;

                switch (type) {
                    case "PushEvent":
                        int commitCount = countOccurrences(eventJson, "\"sha\":\"");
                        activity = "Pushed " + commitCount + " commit" + (commitCount == 1 ? "" : "s") + " to " + repo;
                        break;
                    case "IssuesEvent":
                        activity = "Opened a new issue in " + repo;
                        break;
                    case "WatchEvent":
                        activity = "Starred " + repo;
                        break;
                    case "ForkEvent":
                        activity = "Forked " + repo;
                        break;
                    case "CreateEvent":
                        activity = "Created something in " + repo;
                        break;
                    case "PullRequestEvent":
                        activity = "Opened a pull request in " + repo;
                        break;
                }

                if (activity != null) {
                    System.out.println("- " + activity);
                    count++;
                }
            }

            eventStart = eventEnd + 1;
        }

        if (count == 0) {
            System.out.println("No public activity found.");
        }
    }

    private static int findMatchingBrace(String json, int start) {
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '{') depth++;
            else if (json.charAt(i) == '}') depth--;

            if (depth == 0) return i;
        }
        return -1; // No matching brace found
    }

    private static String extractValue(String text, String prefixRegex, String endChar) {
        try {
            int start = text.indexOf(prefixRegex);
            if (start == -1) return null;
            start = text.indexOf(":", start) + 1;
            int quoteStart = text.indexOf("\"", start) + 1;
            int quoteEnd = text.indexOf(endChar, quoteStart);
            if (quoteStart == -1 || quoteEnd == -1) return null;
            return text.substring(quoteStart, quoteEnd);
        } catch (Exception e) {
            return null;
        }
    }

    private static int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }
}
