package io.alda.player

import com.illposed.osc.OSCBadDataEvent
import com.illposed.osc.OSCBundle
import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCPacket
import com.illposed.osc.OSCPacketEvent
import com.illposed.osc.OSCPacketListener
import com.illposed.osc.transport.NetworkProtocol
import com.illposed.osc.transport.OSCPortIn
import com.illposed.osc.transport.OSCPortInBuilder
import mu.KotlinLogging
import java.net.InetAddress
import java.net.InetSocketAddress

private val log = KotlinLogging.logger {}

private fun instructions(packet : OSCPacket) : List<OSCMessage> {
  if (packet is OSCMessage) {
    return listOf(packet)
  }

  return (packet as OSCBundle).getPackets().flatMap { instructions(it) }
}

fun receiver(port : Int) : OSCPortIn {
  val loopbackSocketAddress = InetSocketAddress(InetAddress.getByName(null), port)
  return OSCPortInBuilder()
    .setNetworkProtocol(NetworkProtocol.TCP)
    .setSocketAddress(loopbackSocketAddress)
    .setPacketListener(object : OSCPacketListener {
    override fun handlePacket(event : OSCPacketEvent) {
      stateManager!!.delayExpiration()
      playerQueue.put(instructions(event.getPacket()))
    }

    override fun handleBadData(event : OSCBadDataEvent) {
      log.error { "bad data: $event" }
    }
  }).build()
}

