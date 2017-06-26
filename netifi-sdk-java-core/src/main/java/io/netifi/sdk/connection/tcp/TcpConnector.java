package io.netifi.sdk.connection.tcp;

import com.google.flatbuffers.FlatBufferBuilder;
import io.netifi.edge.router.ConnectionSetupInfo;
import io.netifi.edge.router.ConnectionSetupInfoUnion;
import io.netifi.edge.router.DestinationConnectionSetupInfo;
import io.netifi.edge.router.Version;
import io.netifi.sdk.NetifiConfig;
import io.netifi.sdk.connection.Connector;
import io.netifi.sdk.connection.Handler;
import io.rsocket.Frame;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.PayloadImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessage;
import reactor.core.publisher.Mono;

/** An implementation of {@link io.netifi.sdk.connection.Connector} that return */
public class TcpConnector implements Connector<TcpConnection> {
  private static final Logger logger = LogManager.getLogger(TcpConnector.class);

  @Override
  public Mono<TcpConnection> apply(NetifiConfig netifiConfig) {
    try {
      logger.info(() -> new FormattedMessage("creating connection to %s", netifiConfig.toString()));

      // Setup Sender
      FlatBufferBuilder builder = new FlatBufferBuilder();
      Version.startVersion(builder);
      Version.addMajor(builder, netifiConfig.getMajor());
      Version.addMinor(builder, netifiConfig.getMinor());
      Version.addPatch(builder, netifiConfig.getPatch());
      int version = Version.endVersion(builder);

      DestinationConnectionSetupInfo.startDestinationConnectionSetupInfo(builder);
      DestinationConnectionSetupInfo.addId(builder, netifiConfig.getId());
      DestinationConnectionSetupInfo.addEnvironment(builder, netifiConfig.getEnvironment());
      DestinationConnectionSetupInfo.addRegion(builder, netifiConfig.getRegion());
      DestinationConnectionSetupInfo.addGroup(builder, netifiConfig.getGroup());
      DestinationConnectionSetupInfo.addSubGroup(builder, netifiConfig.getSubGroup());
      DestinationConnectionSetupInfo.addVersion(builder, version);

      int offset = DestinationConnectionSetupInfo.endDestinationConnectionSetupInfo(builder);

      ConnectionSetupInfo.startConnectionSetupInfo(builder);
      ConnectionSetupInfo.addInfo(builder, offset);
      ConnectionSetupInfo.addInfoType(
          builder, ConnectionSetupInfoUnion.DestinationConnectionSetupInfo);
      int info = ConnectionSetupInfo.endConnectionSetupInfo(builder);
      builder.finish(info);

      return RSocketFactory.connect()
          .keepAlive()
          .setupPayload(new PayloadImpl(Frame.NULL_BYTEBUFFER, builder.dataBuffer()))
          .acceptor(
              () ->
                  rSocket -> {
                    Handler wrappedSocket = WrapperUtil.rsocketToHandler(rSocket);
                    Handler handler = netifiConfig.getHandlerAcceptor().apply(wrappedSocket);
                    return WrapperUtil.handlerToRSocket(handler);
                  })
          .transport(TcpClientTransport.create(netifiConfig.getHost(), netifiConfig.getPort()))
          .start()
          .doOnNext(
              r ->
                  logger.info(
                      () ->
                          new FormattedMessage(
                              "successfully created connection to  %s and %d",
                              netifiConfig.getHost(), netifiConfig.getPort())))
          .map(TcpConnection::new);

    } catch (Throwable t) {
      return Mono.error(t);
    }
  }
}
