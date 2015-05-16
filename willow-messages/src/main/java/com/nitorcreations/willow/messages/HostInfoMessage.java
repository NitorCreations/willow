package com.nitorcreations.willow.messages;

import org.msgpack.annotation.Message;

@Message
public class HostInfoMessage extends AbstractMessage {

  public String publicIpAddress, publicHostname, privateIpAddress, privateHostname;

}
