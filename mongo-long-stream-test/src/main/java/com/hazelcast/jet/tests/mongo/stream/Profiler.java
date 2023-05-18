/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.jet.tests.mongo.stream;

import java.time.Duration;
import java.time.Instant;

class Profiler {
    private Instant startAt;
    private String msg = "";


    public static Profiler start(String msgIn) {
        Profiler profiler = new Profiler();
        profiler.startInternal(msgIn);
        return profiler;
    }
    public void startInternal(String msgIn) {
        startAt = Instant.now();
        msg = msgIn;
    }

    public String stop() {
        Instant stop = Instant.now();
        Duration duration = Duration.between(startAt, stop);
        startAt = null;
        return String.format("\n%20s | %s", duration.toString(), msg);
    }
}
