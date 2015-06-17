function FindProxyForURL(url, host) {
  if (url == "http://test1") {
    // too short
    return "FOO";
  } else if (url == "http://test2") {
    //Not valid
    return "FOOBAR ";
  } else {
    //Empty
    return "           "; 
  }
  
}