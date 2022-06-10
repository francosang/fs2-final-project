package tcp

import fs2._

trait FakeTcpServers {
  def multipleChannels[F[_]](tcpChannels: List[TCPChannel[F]]): TCPServer[F] = new TCPServer[F] {
    override def stream: Stream[F, TCPChannel[F]] = {
      Stream.emits(tcpChannels)
    }
  }
}