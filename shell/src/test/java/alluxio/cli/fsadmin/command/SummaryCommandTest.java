/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.cli.fsadmin.command;

import alluxio.cli.fsadmin.report.SummaryCommand;
import alluxio.client.MetaMasterClient;
import alluxio.client.block.BlockMasterClient;
import alluxio.util.CommonUtils;
import alluxio.wire.BlockMasterInfo;
import alluxio.wire.MasterInfo;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SummaryCommandTest {
  private MetaMasterClient mMetaMasterClient;
  private BlockMasterClient mBlockMasterClient;
  private ByteArrayOutputStream mOutputStream;
  private PrintStream mPrintStream;

  @Before
  public void prepareDependencies() throws IOException {
    // Generate random values for MasterInfo and BlockMasterInfo
    // Prepare mock meta master client
    mMetaMasterClient = Mockito.mock(MetaMasterClient.class);
    MasterInfo masterInfo = new MasterInfo()
        .setMasterAddress("testAddress")
        .setWebPort(1231)
        .setRpcPort(8462)
        .setStartTimeMs(1131242343122L)
        .setUpTimeMs(12412412312L)
        .setVersion("testVersion")
        .setSafeMode(false);
    Mockito.when(mMetaMasterClient.getMasterInfo(Mockito.any())).thenReturn(masterInfo);

    // Prepare mock block master client
    mBlockMasterClient = Mockito.mock(BlockMasterClient.class);
    Map<String, Long> capacityBytesOnTiers = new HashMap<>();
    Map<String, Long> usedBytesOnTiers = new HashMap<>();
    capacityBytesOnTiers.put("MEM", 1341353L);
    usedBytesOnTiers.put("MEM", 62434L);
    BlockMasterInfo blockMasterInfo = new BlockMasterInfo()
        .setLiveWorkerNum(12)
        .setLostWorkerNum(4)
        .setCapacityBytes(1341353L)
        .setCapacityBytesOnTiers(capacityBytesOnTiers)
        .setUsedBytes(62434L)
        .setUsedBytesOnTiers(usedBytesOnTiers)
        .setFreeBytes(1278919L);
    Mockito.when(mBlockMasterClient.getBlockMasterInfo(Mockito.any()))
        .thenReturn(blockMasterInfo);

    // Prepare print stream
    mOutputStream = new ByteArrayOutputStream();
    mPrintStream = new PrintStream(mOutputStream, true, "utf-8");
  }

  @After
  public void after() {
    mPrintStream.close();
  }

  @Test
  public void summary() throws IOException {
    SummaryCommand summaryCommand = new SummaryCommand(mMetaMasterClient,
        mBlockMasterClient, mPrintStream);
    summaryCommand.run();
    checkIfOutputValid();
  }

  /**
   * Checks if the output is expected.
   */
  private void checkIfOutputValid() {
    String output = new String(mOutputStream.toByteArray(), StandardCharsets.UTF_8);
    // Skip checking startTime which relies on system time zone
    String startTime =  CommonUtils.convertMsToDate(1131242343122L);
    List<String> expectedOutput = Arrays.asList("Alluxio cluster summary: ",
        "    Master Address: testAddress",
        "    Web Port: 1231",
        "    Rpc Port: 8462",
        "    Started: " + startTime,
        "    Uptime: 143 day(s), 15 hour(s), 53 minute(s), and 32 second(s)",
        "    Version: testVersion",
        "    Safe Mode: false",
        "    Live Workers: 12",
        "    Lost Workers: 4",
        "    Total Capacity: 1309.92KB",
        "        Tier: MEM  Size: 1309.92KB",
        "    Used Capacity: 60.97KB",
        "        Tier: MEM  Size: 60.97KB",
        "    Free Capacity: 1248.94KB");
    List<String> testOutput = Arrays.asList(output.split("\n"));
    Assert.assertThat(testOutput,
        IsIterableContainingInOrder.contains(expectedOutput.toArray()));
  }
}
