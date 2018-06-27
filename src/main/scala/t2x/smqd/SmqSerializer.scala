package t2x.smqd

import java.io.{ByteArrayInputStream, ObjectInputStream, ObjectOutputStream}
import java.nio.charset.Charset

import akka.serialization.Serializer
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.ByteBuf

/**
  * 2018. 6. 15. - Created by Kwon, Yeong Eon
  */

class SmqSerializer extends Serializer with StrictLogging {
  override val identifier: Int = 0xDEAD + 1

  override val includeManifest: Boolean = false

  override def toBinary(o: AnyRef): Array[Byte] = o match {

    case m: RoutableMessage =>
      val buf = io.netty.buffer.Unpooled.buffer()

      val path: CharSequence = m.topicPath.toString
      val pathLen: Int = path.length

      buf.writeByte('M')
      buf.writeInt(pathLen)
      buf.writeCharSequence(path, Charset.forName("utf-8"))
      buf.writeBoolean(m.isRetain)

      m.msg match {
        case bb: ByteBuf =>
          buf.writeByte('b')
          buf.writeInt(bb.readableBytes)
          bb.markReaderIndex()
          buf.writeBytes(bb)
          bb.resetReaderIndex()

        case str: String =>
          buf.writeByte('s')
          val data = str.getBytes("utf-8")
          buf.writeInt(data.length)
          buf.writeBytes(data)

        case obj: AnyRef =>
          val bos = new ByteOutputStream()
          val out = new ObjectOutputStream(bos)
          out.writeObject(obj)
          val arr = bos.getBytes
          bos.close()

          buf.writeByte('j')
          buf.writeInt(arr.length)
          buf.writeBytes(arr)

        case other =>
          throw new RuntimeException(s"Unsupported payload: ${other.getClass.getCanonicalName}")
      }

      buf.array()

    case m: ByteBuf =>
      val buf = io.netty.buffer.Unpooled.buffer()
      m.markReaderIndex()
      buf.writeByte('B')
      buf.writeInt(m.readableBytes)
      buf.writeBytes(m)
      m.resetReaderIndex()
      buf.array()

    case f: FilterPath =>
      val buf = io.netty.buffer.Unpooled.buffer()
      buf.writeByte('F')
      val ba = f.toByteArray
      buf.writeInt(ba.length)
      buf.writeBytes(ba)
      buf.array()

    case other => throw new IllegalArgumentException(
      s"${getClass.getName} only serializes RoutedMessage, not [${other.getClass.getName}]")
  }


  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {

    val buf = io.netty.buffer.Unpooled.wrappedBuffer(bytes)

    buf.readByte() match {
      case 'M' => // RoutableMessage
        val pathLen = buf.readInt()
        val path = buf.readCharSequence(pathLen, Charset.forName("utf-8")).toString
        val isRetain = buf.readBoolean()
        val payload =
          buf.readByte() match {
            case 'b' => // ByteBuf
              val dataLen = buf.readInt()
              val data = buf.readBytes(dataLen)
              data
            case 's' => // String
              val dataLen = buf.readInt()
              val data = new Array[Byte](dataLen)
              buf.readBytes(data)
              new String(data, "utf-8")
            case 'j' => // POJO
              val dataLen = buf.readInt()
              val data = new Array[Byte](dataLen)
              buf.readBytes(data)
              val oin = new ObjectInputStream(new ByteArrayInputStream(data))
              val obj = oin.readObject()
              obj
          }

        // this message is traveling through network, so set isLcoal false
        RoutableMessage(TopicPath.parse(path).get, payload, isRetain, isLocal = false)
      case 'B' => // ByteBuf
        val len = buf.readInt()
        val data = buf.readBytes(len)
        io.netty.buffer.Unpooled.wrappedBuffer(data)

      case 'F' => // FilterPath
        val len = buf.readInt()
        val str = buf.readCharSequence(len, Charset.forName("utf-8")).toString
        FilterPath(str)
    }
  }
}