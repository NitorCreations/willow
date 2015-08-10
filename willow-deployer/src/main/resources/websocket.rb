# encoding: utf-8
require "logstash/namespace"
require "logstash/outputs/base"

class LogStash::Outputs::WebSocket < LogStash::Outputs::Base

  config_name "websocket"
  milestone 1

  config :flush_interval, :validate => :number, :default => "1000"

  config :uri, :validate => :string

  public
  def register
    jarpath = File.join(File.dirname(__FILE__), "../../../vendor/websocket-output/*.jar")
    Dir[jarpath].each do |jar|
        require jar
    end

    options = {
      :flush_interval => @flush_interval,
      :uri => @uri
    }
    options[:type] = :node

    @transmitter = com.nitorcreations.willow.messages.WebSocketTransmitter.getSingleton(@flush_interval, @uri)
    @adapter = com.nitorcreations.willow.logstash.Adapter.new()
    @transmitter.start()
  end # def register

  public
  def receive(event)
    return unless output?(event)
    event_hash = event.to_hash
    msgtype = event_hash["wsmsgtype"]
    unless msgtype then
      msgtype = "logmessage"
    end
    @transmitter.queue(@adapter.fromHash(msgtype, event_hash))
  end # def receive
end # class LogStash::Outputs::WebSocket
