package com.nitorcreations.willow.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.BufferUnpacker;

import com.nitorcreations.willow.messages.event.EventMessage;
import com.nitorcreations.willow.messages.event.MetricThresholdTriggeredEvent;

public class MessageMapping {
  MessagePack msgpack = new MessagePack();
  private static final Logger logger = Logger.getLogger(MessageMapping.class.getCanonicalName());

  public enum MessageType {
    PROC, CPU, MEM, DISK, OUTPUT, LOG, JMX, PROCESSCPU, ACCESS, LONGSTATS,
    HASH, NET, TCPINFO, DISKIO, THREADDUMP, OS, EVENT, HOSTINFO;
    public String lcName() {
      return toString().toLowerCase(Locale.ENGLISH);
    }

    public static String[] lcNames() {
      List<String> ret = new ArrayList<>();
      for (MessageType next : MessageType.values()) {
        ret.add(next.lcName());
      }
      return ret.toArray(new String[ret.size()]);
    }
  }
  private static Map<MessageType, Class<? extends AbstractMessage>> messageTypes = new ConcurrentHashMap<>(new HashMap<MessageMapping.MessageType, Class<? extends AbstractMessage>>());
  private static Map<Class<? extends AbstractMessage>, MessageType> messageClasses = new ConcurrentHashMap<>(new HashMap<Class<? extends AbstractMessage>, MessageType>());
  static {
    messageTypes.put(MessageType.PROC, Processes.class);
    messageTypes.put(MessageType.CPU, CPU.class);
    messageTypes.put(MessageType.MEM, Memory.class);
    messageTypes.put(MessageType.DISK, DiskUsage.class);
    messageTypes.put(MessageType.OUTPUT, OutputMessage.class);
    messageTypes.put(MessageType.LOG, LogMessage.class);
    messageTypes.put(MessageType.JMX, JmxMessage.class);
    messageTypes.put(MessageType.PROCESSCPU, ProcessCPU.class);
    messageTypes.put(MessageType.ACCESS, AccessLogEntry.class);
    messageTypes.put(MessageType.LONGSTATS, LongStatisticsMessage.class);
    messageTypes.put(MessageType.HASH, HashMessage.class);
    messageTypes.put(MessageType.NET, NetInterface.class);
    messageTypes.put(MessageType.DISKIO, DiskIO.class);
    messageTypes.put(MessageType.TCPINFO, TcpInfo.class);
    messageTypes.put(MessageType.THREADDUMP, ThreadDumpMessage.class);
    messageTypes.put(MessageType.OS, OsInfo.class);
    messageTypes.put(MessageType.EVENT, EventMessage.class);
    messageTypes.put(MessageType.HOSTINFO, HostInfoMessage.class);
    for (Entry<MessageType, Class<? extends AbstractMessage>> next : messageTypes.entrySet()) {
      messageClasses.put(next.getValue(), next.getKey());
    }
  }

  public MessageMapping() {
    registerTypes(msgpack);
  }

  public void registerTypes(MessagePack msgpack) {
    msgpack.register(Thread.State.class);
    msgpack.register(ThreadInfoMessage.class);
    msgpack.register(GcInfo.class);
    msgpack.register(StackTraceData.class);
    msgpack.register(LockData.class);
    msgpack.register(MonitorData.class);
    msgpack.register(ThreadData.class);
    msgpack.register(MetricThresholdTriggeredEvent.class);
    for (Class<?> next : messageTypes.values()) {
      msgpack.register(next);
    }
    msgpack.register(DeployerMessage.class);
  }

  public static Class<? extends AbstractMessage> map(int type) {
    return messageTypes.get(MessageType.values()[type]);
  }

  public static MessageType map(Class<?> msgclass) {
    return messageClasses.get(msgclass);
  }

  public ByteBuffer encode(List<AbstractMessage> msgs) throws IOException {
    LZ4Factory factory = LZ4Factory.fastestInstance();
    LZ4Compressor compressor = factory.fastCompressor();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (Packer packer = msgpack.createPacker(out)) {
      for (AbstractMessage msg : msgs) {
        byte[] message = msgpack.write(msg);
        MessageType type = map(msg.getClass());
        packer.write(new DeployerMessage(type.ordinal(), message));
      }
    }
    byte data[] = out.toByteArray();
    int maxCompressedLength = compressor.maxCompressedLength(data.length);
    byte[] compressed = new byte[maxCompressedLength];
    int compressedLength = compressor.compress(data, 0, data.length, compressed, 0, maxCompressedLength);
    logger.finest(String.format("Message data %d bytes, compressed %d bytes", data.length, compressedLength));
    if (compressedLength >= data.length) {
      return (ByteBuffer) ByteBuffer.allocate(4 + data.length).putInt(0).put(data).flip();
    }
    return (ByteBuffer) ByteBuffer.allocate(4 + compressedLength).putInt(data.length).put(compressed, 0, compressedLength).flip();
  }

  public List<AbstractMessage> decode(byte[] data, int offset, int length) throws IOException {
    if (length < 4)
      return new ArrayList<AbstractMessage>();
    ByteBuffer read = ByteBuffer.wrap(data);
    int uclen = read.getInt();
    byte[] restored;
    if (uclen == 0) {
      restored = new byte[length - 4];
      read.get(restored);
    } else {
      restored = new byte[uclen];
      LZ4Factory factory = LZ4Factory.fastestInstance();
      LZ4FastDecompressor decompressor = factory.fastDecompressor();
      try {
        decompressor.decompress(data, offset + 4, restored, 0, uclen);
      } catch (Throwable e) {
        String message = String.format("Failed to parse buffer[%d], %d, %d - uncompressed len %d", data.length, offset, length, uclen);
        throw new IOException(message, e);
      }
    }
    BufferUnpacker unpacker = msgpack.createBufferUnpacker(restored);
    ArrayList<AbstractMessage> ret = new ArrayList<AbstractMessage>();
    while (unpacker.getReadByteCount() < restored.length) {
      DeployerMessage next = unpacker.read(DeployerMessage.class);
      ret.add(msgpack.read(next.message, map(next.type)));
    }
    return ret;
  }
}
