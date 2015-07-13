# Willow event handler #

Willow event handler is a stand-alone executable that receives events from the metrics server
and processes them with the configured handlers.

## Websocket authentication ##

The event handler uses a websocket connection to the Willow metrics server. This connection is authenticated
using the Willow internal authentication mechanism. The authentication is based on SSH keys.

You can set it up e.g. as follows:
1. On the event handler server, put your public and private keys in your home directory in `.ssh/id_rsa.pub` and `.ssh/id_rsa`. Alternatively you can setup an SSH agent on the eventhandler machine to provide this information.
2. Add the line from the `id_rsa.pub` to the file `authorized_keys` in the metrics server. This file must go to the root of the metrics server classpath.
3. Build willow-servers so that `authorized_keys` ends up in the classpath (e.g. in the über-jar)
4. Set the username (Java system property `user.name`) to match whatever is specified in you public key on the eventhandler server. In Windows you can do this in the command prompt just with e.g. `SET USERNAME=myname@hostname.com`
5. With the username appropriately set up, start Willow eventhandler (and any components based on Willow deployer)

## Configuring handlers ##

The handlers are configured using either `.properties` files or YML files. These files are provided as command-line arguments to the eventhandler executable (as file URLs or in fact any URLs).

The configuration must contain the websocket URL to the metrics server.

A handler would be configured using YML e.g. as follows:

	willow-event-handler:
	  eventsUri: ws://localhost:5120/poll-internal/
	  handlers:
	    - event: com.nitorcreations.willow.messages.event.ChildDiedEvent
	      handler: com.nitorcreations.willow.eventhandler.SendMailEventHandler
	      properties: &mail
	        host: mail.hostname.com
	        port: 25
	        from: nobody@hostname.com
	        to: somebody@hostname.com, somebodyelse@hostname.com
	    - event: com.nitorcreations.willow.messages.event.ChildRestartedEvent
	      handler: com.nitorcreations.willow.eventhandler.SendMailEventHandler
	      properties: *mail

The configuration simply defines the event class and the handler class. Only the exact event classes are handled, not any subclasses of them. You can define any number of handlers for the same event class. Arbitrary properties can be specified for the handler class, these are injected to the handler instance as standard JavaBean properties.

## Extending with custom handlers ##

You can easily implement any custom handler just by implementing the simple `com.nitorcreations.willow.eventhandler.EventHandler` interface. It can be dynamically configured using the configuration mechanism described above (e.g. any custom properties). The class only needs to be in the classpath when the eventhandler is run, so you must build the eventhandler appropriately.
