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
package org.lealone.storage;

import org.lealone.aostore.AOStore;
import org.lealone.aostore.btree.BTreeMap;
import org.lealone.storage.type.DataType;

public class AOMapBuilder extends StorageMap.BuilderBase {
    private final AOStore store;

    public AOMapBuilder(AOStore store) {
        this.store = store;
    }

    @Override
    public <K, V> StorageMap<K, V> openMap(String name, DataType keyType, DataType valueType) {
        BTreeMap.Builder<K, V> builder = new BTreeMap.Builder<K, V>().keyType(keyType).valueType(valueType);
        return store.openMap(name, builder);
    }

    @Override
    public String getMapName(int id) {
        return null; // TODO
    }
}
