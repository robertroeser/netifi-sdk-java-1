package io.netifi.nrqp.frames;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

/** Created by robertroeser on 8/12/17. */
public class DestinationAvailResultTest {
  @Test
  public void testComputeLengthWithToken() {
    int expected = 17;
    int length = DestinationAvailResult.computeLength(true);
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testComputeLengthWithoutToken() {
    int expected = 13;
    int length = DestinationAvailResult.computeLength(false);
    Assert.assertEquals(expected, length);
  }

  @Test
  public void encodeWithToken() {
    int length = DestinationAvailResult.computeLength(true);
    int token = 1;
    boolean found = false;
    ByteBuf byteBuf = Unpooled.buffer(length);
    DestinationAvailResult.encode(byteBuf, true, token, found, 0);
    Assert.assertEquals(found, DestinationAvailResult.found(byteBuf));
  }

  @Test
  public void encodeWithoutToken() {
    int length = DestinationAvailResult.computeLength(false);
    int token = 1;
    boolean found = true;
    ByteBuf byteBuf = Unpooled.buffer(length);
    DestinationAvailResult.encode(byteBuf, false, token, found, 0);
    Assert.assertEquals(found, DestinationAvailResult.found(byteBuf));
  }
}
