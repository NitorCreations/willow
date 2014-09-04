package com.nitorcreations.willow.ssh;


public class SessionOutput {
    private final String output;
    private final Long hostSystemId=Long.valueOf(1);

    public SessionOutput(String output) {
    	this.output = output;
    }
    public String getOutput() {
        return output;
    }
	public Long getHostSystemId() {
		return hostSystemId;
	}
}
