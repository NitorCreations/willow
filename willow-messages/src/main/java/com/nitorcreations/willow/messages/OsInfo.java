package com.nitorcreations.willow.messages;

public class OsInfo extends AbstractMessage {
  public String name;
  public String version;
  public String arch;
  public String machine;
  public String description;
  public String patchLevel;
  public String vendor;
  public String vendorVersion;
  public String vendorName;
  public String vendorCodeName;
  public String getName() {
    return name;
  }
  public String getVersion() {
    return version;
  }
  public String getArch() {
    return arch;
  }
  public String getMachine() {
    return machine;
  }
  public String getDescription() {
    return description;
  }
  public String getPatchLevel() {
    return patchLevel;
  }
  public String getVendor() {
    return vendor;
  }
  public String getVendorVersion() {
    return vendorVersion;
  }
  public String getVendorName() {
    return vendorName;
  }
  public String getVendorCodeName() {
    return vendorCodeName;
  }
  public void setName(String name) {
    this.name = name;
  }
  public void setVersion(String version) {
    this.version = version;
  }
  public void setArch(String arch) {
    this.arch = arch;
  }
  public void setMachine(String machine) {
    this.machine = machine;
  }
  public void setDescription(String description) {
    this.description = description;
  }
  public void setPatchLevel(String patchLevel) {
    this.patchLevel = patchLevel;
  }
  public void setVendor(String vendor) {
    this.vendor = vendor;
  }
  public void setVendorVersion(String vendorVersion) {
    this.vendorVersion = vendorVersion;
  }
  public void setVendorName(String vendorName) {
    this.vendorName = vendorName;
  }
  public void setVendorCodeName(String vendorCodeName) {
    this.vendorCodeName = vendorCodeName;
  }
}
