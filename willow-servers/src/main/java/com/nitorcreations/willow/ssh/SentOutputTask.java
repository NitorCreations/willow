/**
 * Copyright 2013 Sean Kavanagh - sean.p.kavanagh6@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nitorcreations.willow.ssh;

import static com.nitorcreations.willow.ssh.SecureShellWS.BUFFER_LEN;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.eclipse.jetty.websocket.api.Session;

import com.google.gson.Gson;

/**
 * class to send output to web socket client
 */
public class SentOutputTask implements Runnable {


	private final Gson gson = new Gson();
	Session session;
	InputStream output;

	public SentOutputTask(Session session, InputStream output) {
		this.session = session;
		this.output = output;
	}

	public void run() {
		byte[] buf = new byte[BUFFER_LEN];

		while (session.isOpen()) {
			int read;
			try {
				while ((read = output.read(buf)) != -1) {
					try {
						if (read > 0) {
							SessionOutput out = new SessionOutput(new String(buf, 0, read, Charset.forName("UTF-8")));
							String json = gson.toJson(out);
							this.session.getRemote().sendString(json);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
