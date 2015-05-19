package com.nitorcreations.willow.protocols.ssh;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value={"NM_SAME_SIMPLE_NAME_AS_SUPERCLASS"}, 
  justification="Name mandated by how java standard protocol handlers work" )
public class Handler extends com.nitorcreations.willow.protocols.sftp.Handler {  
}
