import exceptions.InsufficientLinksException;
import exceptions.InvalidURLException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author dboyer
 * @version 1.0
 *
 * This class is a basic Web Crawler class. The constructor takes two arguments. The first argument is the number of sites
 * we want to visit, and the second is the site where we will start the crawl. This class provides sufficient validation for
 * its data members. Provided number of hops must be greater than zero and the URL must follow standard, well-formed URL
 * patterns.
 *
 *           /      \
 *        \  \  ,,  /  /
 *         '-.`\()/`.-'
 *        .--_'(  )'_--.
 *       / /` /`""`\ `\ \
 *        |  |  ><  |  |
 *        \  \      /  /
 *            '.__.'
 *
 */
public class Spider {

    public static void main(String[] args) throws IOException, InsufficientLinksException {
        // does the user need help?
        if (Spider.userNeedsHelp(args[0])) {
            Spider.printHelp();
            return;
        }

        Spider spider = new Spider(Integer.parseInt(args[0]), args[1]);
        spider.crawl();
    }

    private final int numHops;      // how many sites we want to visit
    private Set<String> visited;    // sites that we have visited
    private List<String> toVisit;   // sites to visit. note that this is maintained like a set with no duplicate entries
    private Logger logger;          // for logging

    /**
     * Constructor. Sets data members and calls validate to validate the fields.
     * @param  numHops the number of sites we want to visit
     * @param url url where we will begin our crawl
     */
    private Spider(int numHops, String url) {
        this.logger = Logger.getLogger(Spider.class.getName());
        this.numHops = numHops;
        this.toVisit = new ArrayList<>();
        this.toVisit.add(url);
        this.visited = new HashSet<>();
        validate();
    }

    /**
     * Validates that:
     *  - the number of hops (sites to visit) is greater than 0
     *  - the provided url is not null
     *  - the provided url is not an empty string
     *  - the url follows a valid pattern
     * If the string passed in for the url field equals "--help" or "-h" help
     * instructions will be printed to the console.
     */
    private void validate() {
        // regular validation
        Validator validate = new Validator();
        validate.notNull("url", this.toVisit.get(0));
        validate.notEmpty("url", this.toVisit.get(0));
        validate.greaterThan("numHops", this.numHops, 0);

        // is the URL following a valid pattern?
        validateUrl();
    }

    /**
     * Prints help instructions to the console.
     */
    private static void printHelp() {
        System.out.println("java Spider <numHops> <url>");
        System.out.println("    numHops   number of sites to visit");
        System.out.println("    url       url to start at");

    }

    /**
     * Returns a boolean indicating whether the user entered "--help" or "-h".
     */
    private static boolean userNeedsHelp(String value) {
        return (value.equals("--help") || value.equals("-h"));
    }

    /**
     * Returns a boolean indicating whether the url provided by the user follows a valid pattern.
     * @throws InvalidURLException if the URL does not follow standard patterns
     */
    private void validateUrl() throws InvalidURLException {
        // Pattern to check if this is a valid URL address
        Pattern pattern = Pattern.compile("^(http://|https://)?(www.)?([a-zA-Z0-9]+).[a-zA-Z0-9]*.[a-z]{3}.?([a-z]+)?$");
        Matcher matcher;
        matcher = pattern.matcher(this.toVisit.get(0));

        // if we didn't find a match throw an error notifying the user the url is not valid
        if (!matcher.matches()) {
            throw new InvalidURLException();
        }
    }

    /**
     * Visits the URL provided in the constructor, requests all of the HTMl from that site, and then filters the HTML
     * for valid URL links. These links are then put into a queue to be visited. Repeats this process for numHops.
     */
    private void crawl() throws IOException, InsufficientLinksException {

        for (int hops = 0; hops < this.numHops; hops++) {
            String html = this.getHtml();

            // means that we couldn't connect
            if (html == null) {
                // then let's remove that bad link, and decrement hops
                this.toVisit.remove(0);
                hops--;
                continue;

            }

            // no links found on that site
            if (!this.getLink(html)) {
                logger.log(Level.INFO, "No links found on " + this.toVisit.get(0));
            }

            // add the current URL to the visited set, remove it from toVisit
            this.visited.add(this.toVisit.get(0));
            logger.log(Level.INFO, "just visited: " + this.toVisit.get(0));
            this.toVisit.remove(0);

            // we have no more sites available to visit
            if (this.toVisit == null) {
                throw new InsufficientLinksException((numHops - hops));
            }
        }
    }

    /**
     * Connects to a url and requests all of the HTML from that site. Processes all of the HTML and returns it
     * as a single String object.
     * @throws IOException if connection cannot be established
     */
    private String getHtml() throws IOException {

        if (this.toVisit.isEmpty()) {
            throw new InsufficientLinksException();
        }

        /// / establish a connection with the url
        HttpURLConnection connection = (HttpURLConnection) (new URL(this.toVisit.get(0))).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        // try to connect
        try {
            connection.connect();

        } catch (IOException e) {

            // whoops, looks like that site didn't want to make a connection, so let's get rid of it
            // and log an info message to the user
            logger.log(Level.WARNING, "Could not connect to " + this.toVisit.get(0));
            return null;

        }
            // initialize input stream and string builder to build plain text of all the html
            // received from the GET request
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder html = new StringBuilder();

            // stick that nonsense together, line by line
            String line;
            while ((line = reader.readLine()) != null) {
                html.append(line);
            }

            // close our resources and return the html string
            inputStream.close();
            connection.disconnect();
            return html.toString();

    }

    /**
     * Finds the next link in the html string that has not yet been visited, and is not already in the toVisit set.
     * Adds the first url that meets these conditions to the end of the toVisit set.
     * @param html html that is being searched for links
     * @return whether a valid, unvisited link was found in the html
     */
    private boolean getLink(String html) {
        // because we have not yet found a valid link
        boolean validLinkFound = false;

        // regex pattern to match for <a href> nodes
        Pattern linkPattern = Pattern.compile("<a href=\"http*([^\"]*\"|[^\\s>]*)\\s*>",  Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
        Matcher matcher = linkPattern.matcher(html);

        // cycle through to find a match
        while (matcher.find()) {

            // current match for the pattern
            String url = matcher.group();

            // trimmed down to remove html tag and quotations
            url = url.substring(9, url.length() - 2);

            // if we haven't already visited the site...
            if (shouldVisit(url)) {
                this.toVisit.add(url);      // add it to the list of places to visit
                validLinkFound = true;      // record that a valid link was found
                break;
            }
        }

        return validLinkFound;
    }

    /**
     * Returns true if the url has:
     *  - not already been visited (in the visited set)
     *  - already in the toVisit list
     */
    private boolean shouldVisit(String url) {
        return (!this.visited.contains(url) && !this.toVisit.contains(url));
    }

    /**
     * Validator class to validate the url and the number of hops that we are provided with. Will throw
     * an exception if the requirements are not met. A meaningful message will be provided to the user.
     */
    private class Validator {

        /**
         * Checks that the string is not null.
         * @param name description of the object being evaluated. Example: url
         * @param value string to check
         * @throws RuntimeException if conditions are not met
         */
        private void notNull(String name, Object value) throws RuntimeException {
            if (value == null) {
                throw new RuntimeException("Validation failed. " + name + " cannot be null.");
            }
        }

        /**
         * Checks that the string provided is not empty.
         * @param name description of the object being evaluated. Example: url
         * @param value string to check
         * @throws RuntimeException if conditions are not met
         */
        private void notEmpty(String name, String value) throws RuntimeException {
            if (value.isEmpty()) {
                throw new RuntimeException("Validation failed. " + name + " cannot be an empty string.");
            }
        }

        /**
         * Checks that the value is greater than the minimum.
         * @param name description of the object being evaluated. Example: numHops
         * @param value value to check
         * @throws RuntimeException if conditions are not met
         */
        private void greaterThan(String name, int value, int minimum) throws RuntimeException {
            if (value < minimum) {
                throw new RuntimeException("Validation failed. " + name + " must be greater than " + String.valueOf(minimum) + ".");
            }
        }
    }
}
