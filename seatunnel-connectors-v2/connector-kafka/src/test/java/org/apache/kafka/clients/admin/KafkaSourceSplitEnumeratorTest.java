/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.kafka.clients.admin;

import org.apache.seatunnel.connectors.seatunnel.kafka.source.KafkaSourceSplit;
import org.apache.seatunnel.connectors.seatunnel.kafka.source.KafkaSourceSplitEnumerator;

import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

class KafkaSourceSplitEnumeratorTest {

    AdminClient adminClient = Mockito.mock(KafkaAdminClient.class);
    // prepare
    TopicPartition partition = new TopicPartition("test", 0);

    @BeforeEach
    void init() {

        Mockito.when(adminClient.listOffsets(Mockito.any(java.util.Map.class)))
                .thenReturn(
                        new ListOffsetsResult(
                                new HashMap<
                                        TopicPartition,
                                        KafkaFuture<ListOffsetsResult.ListOffsetsResultInfo>>() {
                                    {
                                        put(
                                                partition,
                                                KafkaFuture.completedFuture(
                                                        new ListOffsetsResult.ListOffsetsResultInfo(
                                                                0, 0, Optional.of(0))));
                                    }
                                }));
        Mockito.when(adminClient.describeTopics(Mockito.any(java.util.Collection.class)))
                .thenReturn(
                        DescribeTopicsResult.ofTopicNames(
                                new HashMap<String, KafkaFuture<TopicDescription>>() {
                                    {
                                        put(
                                                partition.topic(),
                                                KafkaFuture.completedFuture(
                                                        new TopicDescription(
                                                                partition.topic(),
                                                                false,
                                                                Collections.singletonList(
                                                                        new TopicPartitionInfo(
                                                                                0,
                                                                                null,
                                                                                Collections
                                                                                        .emptyList(),
                                                                                Collections
                                                                                        .emptyList())))));
                                    }
                                }));
    }

    @Test
    void addSplitsBack() {
        // test
        Map<TopicPartition, KafkaSourceSplit> assignedSplit =
                new HashMap<TopicPartition, KafkaSourceSplit>() {
                    {
                        put(partition, new KafkaSourceSplit(null, partition));
                    }
                };
        Map<TopicPartition, KafkaSourceSplit> pendingSplit = new HashMap<>();
        List<KafkaSourceSplit> splits = Arrays.asList(new KafkaSourceSplit(null, partition));
        KafkaSourceSplitEnumerator enumerator =
                new KafkaSourceSplitEnumerator(adminClient, pendingSplit, assignedSplit);
        enumerator.addSplitsBack(splits, 1);
        Assertions.assertTrue(pendingSplit.size() == splits.size());
        Assertions.assertNull(assignedSplit.get(partition));
        Assertions.assertTrue(pendingSplit.get(partition).getEndOffset() == 0);
    }

    @Test
    void addStreamingSplitsBack() {
        // test
        Map<TopicPartition, KafkaSourceSplit> assignedSplit =
                new HashMap<TopicPartition, KafkaSourceSplit>() {
                    {
                        put(partition, new KafkaSourceSplit(null, partition));
                    }
                };
        Map<TopicPartition, KafkaSourceSplit> pendingSplit = new HashMap<>();
        List<KafkaSourceSplit> splits =
                Collections.singletonList(new KafkaSourceSplit(null, partition));
        KafkaSourceSplitEnumerator enumerator =
                new KafkaSourceSplitEnumerator(adminClient, pendingSplit, assignedSplit, true);
        enumerator.addSplitsBack(splits, 1);
        Assertions.assertEquals(pendingSplit.size(), splits.size());
        Assertions.assertNull(assignedSplit.get(partition));
        Assertions.assertTrue(pendingSplit.get(partition).getEndOffset() == Long.MAX_VALUE);
    }

    @Test
    void addStreamingSplits() throws ExecutionException, InterruptedException {
        // test
        Map<TopicPartition, KafkaSourceSplit> assignedSplit =
                new HashMap<TopicPartition, KafkaSourceSplit>();
        Map<TopicPartition, KafkaSourceSplit> pendingSplit = new HashMap<>();
        List<KafkaSourceSplit> splits =
                Collections.singletonList(new KafkaSourceSplit(null, partition));
        KafkaSourceSplitEnumerator enumerator =
                new KafkaSourceSplitEnumerator(adminClient, pendingSplit, assignedSplit, true);
        enumerator.fetchPendingPartitionSplit();
        Assertions.assertEquals(pendingSplit.size(), splits.size());
        Assertions.assertNotNull(pendingSplit.get(partition));
        Assertions.assertTrue(pendingSplit.get(partition).getEndOffset() == Long.MAX_VALUE);
    }

    @Test
    void addplits() throws ExecutionException, InterruptedException {
        // test
        Map<TopicPartition, KafkaSourceSplit> assignedSplit =
                new HashMap<TopicPartition, KafkaSourceSplit>();
        Map<TopicPartition, KafkaSourceSplit> pendingSplit = new HashMap<>();
        List<KafkaSourceSplit> splits =
                Collections.singletonList(new KafkaSourceSplit(null, partition));
        KafkaSourceSplitEnumerator enumerator =
                new KafkaSourceSplitEnumerator(adminClient, pendingSplit, assignedSplit, false);
        enumerator.fetchPendingPartitionSplit();
        Assertions.assertEquals(pendingSplit.size(), splits.size());
        Assertions.assertNotNull(pendingSplit.get(partition));
        Assertions.assertTrue(pendingSplit.get(partition).getEndOffset() == 0);
    }
}
