package org.keycloak.url.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLParser {
    private final String query;

    public URLParser(String query) {
        this.query = query;
    }

    public String remove(String name) {
        String regex = "(?:^|\\?|&)" + name + "=(.*?)(?=&|$)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(this.query);

        if (matcher.find()) {
            return matcher.replaceAll("");
        }

        return this.query;
    }

    public String get(String name) {
        String regex = "(?:^|\\?|&)" + name + "=(.*?)(?:&|$)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(this.query);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }
}
