package tcp

import cats.effect._
import fs2._
import java.net._
import java.nio.channels._

trait TCPServer[F[_]] {
  def stream: Stream[F, SocketChannel]
}

object TCPServer {
  def impl[F[_]: Sync](hostname: String, port: Int): TCPServer[F] = {
    val serverChannelResource: Resource[F, ServerSocketChannel] = {
      Resource.make(
        Sync[F].blocking(
          ServerSocketChannel
            .open()
            .bind(new InetSocketAddress(hostname, port))
        )
      )(s => Sync[F].blocking(s.close()))
    }
    fromServerSocketChannelResource(serverChannelResource)
  }

  def unsafeCreate[F[_]: Sync](channel: ServerSocketChannel): TCPServer[F] = {
    fromServerSocketChannelResource(Resource.pure[F, ServerSocketChannel](channel))
  }

  private def fromServerSocketChannelResource[F[_]: Sync](
      serverSocketChannel: Resource[F, ServerSocketChannel]
  ): TCPServer[F] = new TCPServer[F] {
    def clientChannelResource(
        serverSocketChannel: ServerSocketChannel
    ): Resource[F, SocketChannel] = {
      Resource.make(Sync[F].blocking(serverSocketChannel.accept()))(s =>
        Sync[F].blocking(s.close())
      )
    }

    override def stream: Stream[F, SocketChannel] = {
      Stream
        .resource(serverSocketChannel)
        .flatMap { serverSocketChannel =>
          Stream.resource(clientChannelResource(serverSocketChannel)).repeat
        }
    }
  }
}
