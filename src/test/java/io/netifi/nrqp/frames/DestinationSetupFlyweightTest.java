package io.netifi.nrqp.frames;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/** */
public class DestinationSetupFlyweightTest {
  @Test
  public void testComputeLengthEncrypted() {
    int expected = 104;
    int length = DestinationSetupFlyweight.computeLength(true, 3);
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testComputeLength() {
    int expected = 72;
    int length = DestinationSetupFlyweight.computeLength(false, 3);
    Assert.assertEquals(expected, length);
  }
  
  @Test
  public void testComputeLengthNoGroups() {
    int expected = 48;
    int length = DestinationSetupFlyweight.computeLength(false, 0);
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testEncodeWithEncryption() {
    Random rnd = ThreadLocalRandom.current();
    byte[] pk = new byte[32];
    rnd.nextBytes(pk);
    byte[] accessToken = new byte[20];
    rnd.nextBytes(accessToken);
    long accessKey = rnd.nextLong();
    long destinationId = rnd.nextLong();
    long groupId1 = rnd.nextLong();
    long groupId2 = rnd.nextLong();
    long groupId3 = rnd.nextLong();

    int length = DestinationSetupFlyweight.computeLength(true, 3);
    ByteBuf byteBuf = Unpooled.buffer(length);

    int encodedLength =
        DestinationSetupFlyweight.encode(
            byteBuf,
            Unpooled.wrappedBuffer(pk),
            Unpooled.wrappedBuffer(accessToken),
            accessKey,
            destinationId,
            0,
            groupId1,
            groupId2,
            groupId3);

    Assert.assertEquals(length, encodedLength);

    byte[] pk1 = new byte[pk.length];
    DestinationSetupFlyweight.publicKey(byteBuf).getBytes(0, pk1);
    Assert.assertArrayEquals(pk, pk1);

    byte[] accessToken1 = new byte[accessToken.length];
    DestinationSetupFlyweight.accessToken(byteBuf).getBytes(0, accessToken1);
    Assert.assertArrayEquals(accessToken, accessToken1);

    Assert.assertEquals(accessKey, DestinationSetupFlyweight.accessKey(byteBuf));
    Assert.assertEquals(destinationId, DestinationSetupFlyweight.destinationId(byteBuf));
    Assert.assertArrayEquals(
        new long[] {groupId1, groupId2, groupId3}, DestinationSetupFlyweight.groupIds(byteBuf));
  }
  
}
