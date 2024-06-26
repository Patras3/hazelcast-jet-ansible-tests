/*
 * Copyright (c) 2008-2024, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.tests.stateful;

import java.io.Serializable;

public class TransactionEvent implements Serializable {

    // 0 = start, 1 = end
    private final int type;
    private final long transactionId;
    private final long timestamp;

    public TransactionEvent(int type, long transactionId, long timestamp) {
        this.type = type;
        this.transactionId = transactionId;
        this.timestamp = timestamp;
    }

    public int type() {
        return type;
    }

    public long transactionId() {
        return transactionId;
    }

    public long timestamp() {
        return timestamp;
    }
}
