package com.nitorcreations.willow.ssh;

import com.google.gson.Gson;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;

import org.apache.lucene.search.suggest.BufferedInputIterator;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.Session;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@WebSocket
public class SecureShellWS {
	private Session session;
	private Channel shell;
	private  JSch jsch = new JSch();
	private com.jcraft.jsch.Session jschSession;
	private CountDownLatch closeLatch;
	private PrintStream inputToShell;
	public static final int BUFFER_LEN = 4 * 1024;
	
	public SecureShellWS() {
		this.closeLatch = new CountDownLatch(1);
	}

	public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
		return this.closeLatch.await(duration, unit);
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		System.out.printf("Got connect: %s%n", session);
		session.setIdleTimeout(TimeUnit.MINUTES.toMillis(1));
		this.session = session;
		Connector con = null;
		try {
			ConnectorFactory cf = ConnectorFactory.getDefault();
			con = cf.createConnector();
		}
		catch(AgentProxyException e){
			System.out.println(e);
		}
		if(con != null ){
			IdentityRepository irepo = new RemoteIdentityRepository(con);
			jsch.setIdentityRepository(irepo);
		}

		String host = session.getUpgradeRequest().getParameterMap().get("host").get(0);
		String user = session.getUpgradeRequest().getParameterMap().get("user").get(0);
		try {
			java.util.Properties config = new java.util.Properties(); 
			config.put("StrictHostKeyChecking", "no");
			jschSession = jsch.getSession(user, host, 22);
			jschSession.setConfig(config);
			jschSession.connect(60000);
			shell = jschSession.openChannel("shell");
			((ChannelShell) shell).setPtyType("vt102");
			shell.connect();
		} catch (JSchException e) {
			e.printStackTrace();
		}
		Runnable run;
		try {
			run = new SentOutputTask(session, new BufferedInputStream(shell.getInputStream(), BUFFER_LEN));
			Thread thread = new Thread(run);
			thread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			inputToShell = new PrintStream(shell.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@OnWebSocketMessage
	public void onMessage(String message) {
		if (session.isOpen()) {
			if (message != null && !message.isEmpty()) {
				@SuppressWarnings("rawtypes")
				Map jsonRoot = new Gson().fromJson(message, Map.class);
				String command = (String) jsonRoot.get("command");
				Integer keyCode = null;
				Double keyCodeDbl = (Double) jsonRoot.get("keyCode");
				if (keyCodeDbl != null) {
					keyCode = keyCodeDbl.intValue();
				}
				if (keyCode != null) {
					if (keyMap.containsKey(keyCode)) {
						try {
							inputToShell.write(keyMap.get(keyCode));
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				} else {
					try {
						inputToShell.write(command.getBytes(Charset.forName("UTF-8")));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
		if (shell != null) {
			shell.disconnect();
		}
		if (jschSession != null) {
			jschSession.disconnect();
		}
		this.session = null;
        this.closeLatch.countDown();
	}


	/**
	 * Maps key press events to the ascii values
	 */
	static Map<Integer, byte[]> keyMap = new HashMap<Integer, byte[]>();

	static {
		//ESC
		keyMap.put(27, new byte[]{(byte) 0x1b});
		//ENTER
		keyMap.put(13, new byte[]{(byte) 0x0d});
		//LEFT
		keyMap.put(37, new byte[]{(byte) 0x1b, (byte) 0x4f, (byte) 0x44});
		//UP
		keyMap.put(38, new byte[]{(byte) 0x1b, (byte) 0x4f, (byte) 0x41});
		//RIGHT
		keyMap.put(39, new byte[]{(byte) 0x1b, (byte) 0x4f, (byte) 0x43});
		//DOWN
		keyMap.put(40, new byte[]{(byte) 0x1b, (byte) 0x4f, (byte) 0x42});
		//BS
		keyMap.put(8, new byte[]{(byte) 0x7f});
		//TAB
		keyMap.put(9, new byte[]{(byte) 0x09});
		//CTR
		keyMap.put(17, new byte[]{});
		//DEL
		keyMap.put(46, "\033[3~".getBytes());
		//CTR-A
		keyMap.put(65, new byte[]{(byte) 0x01});
		//CTR-B
		keyMap.put(66, new byte[]{(byte) 0x02});
		//CTR-C
		keyMap.put(67, new byte[]{(byte) 0x03});
		//CTR-D
		keyMap.put(68, new byte[]{(byte) 0x04});
		//CTR-E
		keyMap.put(69, new byte[]{(byte) 0x05});
		//CTR-F
		keyMap.put(70, new byte[]{(byte) 0x06});
		//CTR-G
		keyMap.put(71, new byte[]{(byte) 0x07});
		//CTR-H
		keyMap.put(72, new byte[]{(byte) 0x08});
		//CTR-I
		keyMap.put(73, new byte[]{(byte) 0x09});
		//CTR-J
		keyMap.put(74, new byte[]{(byte) 0x0A});
		//CTR-K
		keyMap.put(75, new byte[]{(byte) 0x0B});
		//CTR-L
		keyMap.put(76, new byte[]{(byte) 0x0C});
		//CTR-M
		keyMap.put(77, new byte[]{(byte) 0x0D});
		//CTR-N
		keyMap.put(78, new byte[]{(byte) 0x0E});
		//CTR-O
		keyMap.put(79, new byte[]{(byte) 0x0F});
		//CTR-P
		keyMap.put(80, new byte[]{(byte) 0x10});
		//CTR-Q
		keyMap.put(81, new byte[]{(byte) 0x11});
		//CTR-R
		keyMap.put(82, new byte[]{(byte) 0x12});
		//CTR-S
		keyMap.put(83, new byte[]{(byte) 0x13});
		//CTR-T
		keyMap.put(84, new byte[]{(byte) 0x14});
		//CTR-U
		keyMap.put(85, new byte[]{(byte) 0x15});
		//CTR-V
		//keyMap.put(86, new byte[]{(byte) 0x16});
		//CTR-W
		keyMap.put(87, new byte[]{(byte) 0x17});
		//CTR-X
		keyMap.put(88, new byte[]{(byte) 0x18});
		//CTR-Y
		keyMap.put(89, new byte[]{(byte) 0x19});
		//CTR-Z
		keyMap.put(90, new byte[]{(byte) 0x1A});
		//CTR-[
		keyMap.put(219, new byte[]{(byte) 0x1B});
		//CTR-]
		keyMap.put(221, new byte[]{(byte) 0x1D});

	}

}
