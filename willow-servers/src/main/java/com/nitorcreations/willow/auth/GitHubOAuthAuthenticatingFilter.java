package com.nitorcreations.willow.auth;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authz.Permission;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.ImmutableMap;

import mx.com.inftel.shiro.oauth2.AbstractOAuth2AuthenticatingFilter;

public class GitHubOAuthAuthenticatingFilter extends AbstractOAuth2AuthenticatingFilter {

  private static final String SCOPE = "user:email,read:org";
  private final Map<String,? extends Permission> PERMISSIONS;
  private final GitHubOAuthAccounts accounts;
  
  @Inject
  public GitHubOAuthAuthenticatingFilter(GitHubOAuthConfig config, GitHubOAuthAccounts accounts) throws IOException {
    setRedirectUri(config.getRedirectUri());
    setClientId(config.getClientId());
    setClientSecret(config.getClientSecret());
    setLoginUrl("/");
    this.accounts = accounts;
    PERMISSIONS = ImmutableMap.of(
        config.getOrganization()  + "." + config.getAdminTeam(), Permissions.ADMIN,
        config.getOrganization(),       Permissions.MONITOR);

  }
  @Override
  public String getName() {
    return getClass().getName();
  }
  @Override
  protected String getAuthorizeURL(ServletRequest request, ServletResponse response) throws Exception {
    return makeStandardAuthorizeURL(request, response, "https://github.com/login/oauth/authorize", SCOPE);
  }
  @Override
  protected JSONObject getOAuth2Principal(ServletRequest request, ServletResponse response) throws Exception {
    String tokenResponse = httpPost("https://github.com/login/oauth/access_token",
        "client_id=" + getClientId() +
        "&client_secret=" + getClientSecret() +
        "&redirect_uri=" + encodeURL(getRedirectUri()) +
        "&code=" + encodeURL(request.getParameter("code")));
    String token = getAccessToken(tokenResponse);
    Map<String,String> headers = singletonMap("Authorization", "token " + token);
    JSONArray memberOf = JSONTool.toArray(getOrganizations(headers), getTeams(headers));
    String loginId = new JSONObject(httpGet("https://api.github.com/user", headers)).getString("login");
    JSONObject ret = new JSONObject()
        .put("login", loginId)
        .put("member_of", memberOf)
        .put("token", token);
    SimpleAccount acco = doGetAuthenticationInfo(ret);
    accounts.add(acco);
    return ret;
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

  private List<String> getOrganizations(Map<String,String> headers) throws Exception {
    JSONArray organizations = new JSONArray(httpGet("https://api.github.com/user/orgs", headers));

    List<String> names = new ArrayList<>();
    for(int i = 0; i < organizations.length(); i++) {
      names.add(organizations.getJSONObject(i).getString("login"));
    }
    return Collections.unmodifiableList(names);
  }

  private List<String> getTeams(Map<String,String> headers) throws Exception {
    JSONArray teams = new JSONArray(httpGet("https://api.github.com/user/teams", headers));

    List<String> names = new ArrayList<>();
    for(int i = 0; i < teams.length(); i++) {
      JSONObject team = teams.getJSONObject(i);
      names.add(team.getJSONObject("organization").getString("login")
          + "."
          + team.getString("name"));

    }
    return Collections.unmodifiableList(names);
  }

  private String httpGet(String url, Map<String, String> headers) throws Exception {
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
    conn.setDoOutput(true);
    for(Map.Entry<String,String> header : headers.entrySet()) {
      conn.setRequestProperty(header.getKey(), header.getValue());
    }
    return readResponseBody(conn);
  }

  private String httpPost(String url, String data) throws Exception {
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
    conn.setDoOutput(true);
    try(OutputStream out = conn.getOutputStream()) {
      out.write(data.getBytes(UTF_8));
    }
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
  public SimpleAccount doGetAuthenticationInfo(JSONObject user) throws AuthenticationException {
    try {
      String userId = user.getString("login");
      Set<String> memberOf = JSONTool.toStringSet(user.getJSONArray("member_of"));
      return new SimpleAccount(userId, userId, getName(), memberOf, memberShipsToPermissions(memberOf));
    } catch (JSONException e) {
      throw new AuthenticationException(e);
    }
  }
  

  protected Set<Permission> memberShipsToPermissions(Set<String> organizations) {
    Set<Permission> permissions = new HashSet<>();
    for(String team : organizations) {
      Permission permission = PERMISSIONS.get(team);
      if(permission != null) {
        permissions.add(permission);
      }
    }
    return unmodifiableSet(permissions);
  }


}
