package com.nitorcreations.willow.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.BufferUnpacker;

import com.nitorcreations.willow.messages.event.EventMessage;
import com.nitorcreations.willow.messages.event.MetricThresholdTriggeredEvent;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

public class MessageMapping {
  MessagePack msgpack = new MessagePack();
  private static final Logger logger = Logger.getLogger(MessageMapping.class.getCanonicalName());

  public enum MessageType {
    PROC(Processes.class), CPU(CPU.class), MEM(Memory.class), DISK(DiskUsage.class), 
    LOG(LogMessage.class), JMX(JmxMessage.class), PROCESSCPU(ProcessCPU.class),
    ACCESS(AccessLogEntry.class), LONGSTATS(LongStatisticsMessage.class),
    HASH(HashMessage.class), NET(NetInterface.class), TCPINFO(TcpInfo.class),
    DISKIO(DiskIO.class), THREADDUMP(ThreadDumpMessage.class), OS(OsInfo.class),
    EVENT(EventMessage.class),  HOSTINFO(HostInfoMessage.class);
    private final Class<? extends AbstractMessage> implClass;

    MessageType(Class<? extends AbstractMessage> implClass) {
      this.implClass = implClass;
    }
    public String lcName() {
      return toString().toLowerCase(Locale.ENGLISH);
    }
    public Class<? extends AbstractMessage> getMessageClass() {
      return implClass;
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
  private static Map<String, Class<? extends AbstractMessage>> messageNames = new ConcurrentHashMap<>(new HashMap<String, Class<? extends AbstractMessage>>());
  private static Map<Class<? extends AbstractMessage>, MessageType> messageClasses = new ConcurrentHashMap<>(new HashMap<Class<? extends AbstractMessage>, MessageType>());
  static {
    for (MessageType next : MessageType.values()) {
      messageTypes.put(next, next.getMessageClass());
      messageNames.put(next.lcName(), next.getMessageClass());
      messageClasses.put(next.getMessageClass(), next);
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

  public static Class<? extends AbstractMessage> map(String name) {
    return messageNames.get(name);
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
      } catch (Exception e) {
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
