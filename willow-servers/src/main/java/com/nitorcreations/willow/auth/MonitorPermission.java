package com.nitorcreations.willow.auth;

import org.apache.shiro.authz.permission.DomainPermission;

public class MonitorPermission extends DomainPermission {
  private static final long serialVersionUID = -8383249692495574699L;

  public MonitorPermission() {
    super("monitor");
  }
}
