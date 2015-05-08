package com.nitorcreations.willow.servers;

import mx.com.inftel.shiro.oauth2.AbstractOAuth2AuthenticatingFilter;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GitHubOAuthAuthenticatingFilter extends AbstractOAuth2AuthenticatingFilter {

    private static final String SCOPE = "user:email,read:org";

    public GitHubOAuthAuthenticatingFilter() throws IOException {
        Properties config = new Properties();
        config.load(getClass().getResourceAsStream("/github-oauth.properties"));
        setRedirectUri(config.getProperty("redirect_uri"));
        setClientId(config.getProperty("client_id"));
        setClientSecret(config.getProperty("client_secret"));
        setLoginUrl("/");
    }

    @Override
    protected String getAuthorizeURL(ServletRequest request, ServletResponse response) throws Exception {
        return makeStandardAuthorizeURL(request, response, "https://github.com/login/oauth/authorize", SCOPE);
    }

    @Override
    protected JSONObject getOAuth2Principal(ServletRequest request, ServletResponse response) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL("https://github.com/login/oauth/access_token").openConnection();
        conn.setDoOutput(true);
        try(OutputStream out = conn.getOutputStream()) {
            String body = "client_id=" + getClientId()
                    + "&client_secret=" + getClientSecret()
                    + "&redirect_uri=" + encodeURL(getRedirectUri())
                    + "&code=" + encodeURL(request.getParameter("code"));
            out.write(body.getBytes(UTF_8));
        }
        String accessToken = getAccessToken(readResponseBody(conn));
        JSONObject user = new JSONObject(gitHubHttpGet("https://api.github.com/user", accessToken));
        JSONArray memberOf = JSONTool.toArray(getOrganizations(user, accessToken), getTeams(accessToken));

        return new JSONObject()
                .append("login", user.getString("login"))
                .append("member_of", memberOf);
    }

    @Override
    protected String getOAuth2Credentials(JSONObject principal) throws Exception {
        return principal.getString("login");
    }

    private String getAccessToken(String response) throws Exception {
        for(String param : response.split("&")) {
            if(param.startsWith("access_token=")) {
                return decodeURL(param.substring(param.indexOf('=') + 1));
            }
        }
        throw new IllegalStateException("access_token param not sent by idp");
    }

    private List<String> getOrganizations(JSONObject user, String accessToken) throws Exception {
        JSONArray organizations = new JSONArray(gitHubHttpGet(user.getString("organizations_url"), accessToken));

        List<String> names = new ArrayList<>();
        for(int i = 0; i < organizations.length(); i++) {
            names.add(organizations.getJSONObject(i).getString("login"));
        }
        return Collections.unmodifiableList(names);
    }

    private List<String> getTeams(String accessToken) throws Exception {
        JSONArray teams = new JSONArray(gitHubHttpGet("https://api.github.com/user/teams", accessToken));

        List<String> names = new ArrayList<>();
        for(int i = 0; i < teams.length(); i++) {
            JSONObject team = teams.getJSONObject(i);
            names.add(team.getJSONObject("organization").getString("login")
                    + "."
                    + team.getString("name"));

        }
        return Collections.unmodifiableList(names);
    }

    private String gitHubHttpGet(String url, String accessToken) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "token " + accessToken);
        return readResponseBody(conn);
    }

    private String readResponseBody(HttpURLConnection conn) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buf = new byte[4096];
        try(InputStream in = conn.getResponseCode() == 200 ? conn.getInputStream() : conn.getErrorStream()) {
            for(int i = in.read(buf); i != -1; i = in.read(buf)) {
                baos.write(buf, 0, i);
            }
        }

        String body = new String(baos.toByteArray(), UTF_8);
        if(conn.getResponseCode() != 200) {
            throw new IllegalStateException(String.format("idp responded with HTTP %s %s: %s",
                    conn.getResponseCode(), conn.getResponseMessage(), body));
        }
        return body;
    }
}
