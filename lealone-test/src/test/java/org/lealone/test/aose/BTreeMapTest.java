/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lealone.test.aose;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Test;
import org.lealone.storage.PageKey;
import org.lealone.storage.StorageMapCursor;
import org.lealone.storage.aose.AOStorage;
import org.lealone.storage.aose.AOStorageBuilder;
import org.lealone.storage.aose.btree.BTreeMap;
import org.lealone.storage.aose.btree.BTreePage;
import org.lealone.storage.aose.btree.BTreePage.PageReference;
import org.lealone.test.TestBase;

public class BTreeMapTest extends TestBase {
    private AOStorage storage;
    private String storageName;
    private BTreeMap<Integer, String> map;

    @Test
    public void run() {
        init();
        // testMapOperations();
        testGetEndpointToKeyMap();
        // testCompact();
        // testTransfer();

        // testSplit();
    }

    private void init() {
        AOStorageBuilder builder = new AOStorageBuilder();
        storageName = joinDirs("aose");
        int pageSplitSize = 16 * 1024;
        pageSplitSize = 4 * 1024;
        pageSplitSize = 1 * 1024;
        // pageSplitSize = 32 * 1024;
        builder.storageName(storageName).compress().reuseSpace().pageSplitSize(pageSplitSize).minFillRate(30);
        storage = builder.openStorage();

        map = storage.openBTreeMap("BTreeMapTest");
    }

    void testSplit() {
        // map.clear();
        // for (int i = 1; i <= 50; i += 2) {
        // map.put(i, "value" + i);
        // }
        //
        // map.save();

        // for (int i = 29; i <= 48; i += 2) {
        // map.remove(i);
        // }

        for (int i = 1; i <= 50; i += 2) {
            map.remove(i);
        }

        map.remove(49);
        map.printPage();
    }

    void testGetEndpointToKeyMap() {
        map.clear();
        testGetEndpointToKeyMap(map); // 测试空map

        map.clear();
        int count = 10;
        for (int i = 1; i <= count; i++) {
            map.put(i, "value" + i);
        }
        testGetEndpointToKeyMap(map); // 测试只有一个root leaf page的map

        map.clear();
        count = 6000;
        for (int i = 1; i <= count; i++) {
            map.put(i, "value" + i);
        }
        testGetEndpointToKeyMap(map); // 测试有root node page的map

        map.close();
    }

    void testGetEndpointToKeyMap(BTreeMap<Integer, String> map) {
        BTreePage root = map.getRootPage();
        Random random = new Random();
        String[] ids = { "a", "b", "c", "d", "e", "f" };
        injectReplicationHostIds(root, random, ids);
        Integer from = 3; // 5900;
        Integer to = 5999;
        HashSet<PageKey> pageKeySet = new HashSet<>();
        Map<String, List<PageKey>> endpointToPageKeyMap = map.getEndpointToPageKeyMap(null, from, to);
        // System.out.println(endpointToPageKeyMap);
        for (List<PageKey> pageKeys : endpointToPageKeyMap.values()) {
            for (PageKey pk : pageKeys) {
                if (!pageKeySet.add(pk)) {
                    // System.out.println("PageKey: " + pk);
                    fail("PageKey: " + pk);
                }
            }
        }

        int count = 0;
        for (List<PageKey> pageKeys : endpointToPageKeyMap.values()) {
            // System.out.println("pageKeys: " + pageKeys);
            StorageMapCursor<Integer, String> cursor = map.cursor(pageKeys, from);
            while (cursor.hasNext()) {
                count++;
                cursor.next();
                // System.out.println(cursor.getKey());
            }
        }
        System.out.println("count: " + count + ", to-from: " + (to - from + 1));
    }

    void injectReplicationHostIds(BTreePage page, Random random, String[] ids) {
        if (page.isLeaf()) {
            injectReplicationHostIds(null, page, random, ids);
            return;
        }
        for (PageReference pf : page.getChildren()) {
            if (pf.getPage().isNode())
                injectReplicationHostIds(pf.getPage(), random, ids);
            else {
                injectReplicationHostIds(pf, page, random, ids);
            }
        }
    }

    void injectReplicationHostIds(PageReference pf, BTreePage page, Random random, String[] ids) {
        int needNodes = 3;
        ArrayList<String> replicationHostIds = new ArrayList<>(needNodes);
        int totalNodes = ids.length;
        Set<Integer> indexSet = new HashSet<>(needNodes);
        while (true) {
            int i = random.nextInt(totalNodes);
            indexSet.add(i);
            if (indexSet.size() == needNodes)
                break;
        }
        for (int i : indexSet) {
            replicationHostIds.add(ids[i]);
        }
        page.setReplicationHostIds(replicationHostIds);
        if (pf != null)
            pf.setReplicationHostIds(replicationHostIds);
    }

    void testMapOperations() {
        Object v = null;
        map.clear();

        v = map.put(10, "a");
        assertNull(v);
        v = map.get(10);
        assertEquals("a", v);
        assertTrue(map.containsKey(10));

        v = map.putIfAbsent(10, "a1");
        assertNotNull(v);
        assertEquals("a", v);

        v = map.putIfAbsent(20, "b");
        assertNull(v);

        v = map.get(20);
        assertEquals("b", v);

        map.clear();
        assertEquals(0, map.size());

        for (int i = 1; i <= 200; i++) {
            map.put(i, "value" + i);
        }

        assertEquals(200, map.size());

        v = map.firstKey();
        assertEquals(1, v);
        v = map.lastKey();
        assertEquals(200, v);

        v = map.higherKey(101); // >"101"的最小key
        assertEquals(102, v);
        v = map.ceilingKey(101); // >="101"的最小key
        assertEquals(101, v);

        v = map.lowerKey(101); // <"101"的最大key
        assertEquals(100, v);
        v = map.floorKey(101); // <="101"的最大key
        assertEquals(101, v);

        v = map.replace(100, "value100a", "value100");
        assertFalse((boolean) v);
        v = map.replace(100, "value100", "value100a");
        assertTrue((boolean) v);
        v = map.get(100);
        assertEquals("value100a", v);
        v = map.replace(100, "value100a", "value100");
        assertTrue((boolean) v);

        StorageMapCursor<?, ?> cursor = map.cursor(null);
        int count = 0;
        while (cursor.hasNext()) {
            cursor.next();
            count++;
        }
        assertEquals(200, count);

        cursor = map.cursor(151);
        count = 0;
        while (cursor.hasNext()) {
            cursor.next();
            count++;
        }
        assertEquals(50, count);

        v = map.remove(150);
        assertNotNull(v);
        assertEquals(199, map.size());

        // map.printPage();
        // map.remove();

        map.close();

        assertTrue(map.isClosed());

        try {
            map.put(10, "a");
            fail();
        } catch (IllegalStateException e) {
            // e.printStackTrace();
        }
    }

    void testCompact() {
        map = storage.openBTreeMap("BTreeMapTest");

        map.clear();

        map.put(1, "v1");
        map.put(50, "v50");
        map.put(100, "v100");

        map.save();

        for (int i = 1; i <= 200; i++)
            map.put(i, "value" + i);
        map.save();

        // map.printPage();

        for (int i = 50; i <= 200; i++)
            map.put(i, "value" + i);

        map.save();
    }

    void testTransfer() {
        String file = storageName + File.separator + map.getName() + "TransferTo" + AOStorage.SUFFIX_AO_FILE;

        // put();
        // testTransferTo(file);
        testTransferFrom(file);

        // map.get(3000);
    }

    void testTransferTo(String file) {
        // put();
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel channel = raf.getChannel();
            int firstKey = 2000;
            int lastKey = 3000;
            raf.seek(raf.length());
            map.transferTo(channel, firstKey, lastKey);
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // map.get(3000);
    }

    void testTransferFrom(String file) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel channel = raf.getChannel();
            map.transferFrom(channel, 0, raf.length());
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void put() {
        for (int i = 1000; i < 4000; i++) {
            map.put(i, "value" + i);
        }
        map.save();
    }
}
