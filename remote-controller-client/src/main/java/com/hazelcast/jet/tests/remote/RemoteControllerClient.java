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

package com.hazelcast.jet.tests.remote;

import com.google.common.collect.Iterables;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.server.JetBootstrap;
import com.hazelcast.logging.ILogger;
import com.hazelcast.remotecontroller.Lang;
import com.hazelcast.remotecontroller.RemoteController;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.hazelcast.jet.impl.util.Util.uncheckRun;
import static com.hazelcast.jet.tests.common.Util.parseArguments;
import static java.lang.Integer.parseInt;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * This class is used to connect to the HazelcastRemoteController
 * instances running on the cluster machines and restart
 * Hazelcast Jet instances one by one cycling through instances.
 * <p>
 * A python script is invoked on the remote machine which
 * stops/starts a system service `hazelcast-jet-isolated`
 */
public final class RemoteControllerClient {

    private static final int DEFAULT_PORT = 9701;
    private static final int VERIFICATION_DURATION_GAP = 15;

    private static int logCounter;
    private static ILogger logger;

    private RemoteControllerClient() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("hazelcast.logging.type", "log4j");
        parseArguments(args);
        String jetHome = System.getProperty("jetHome");
        int initialSleep = parseInt(System.getProperty("initialSleepMinutes", "5"));
        int sleepBetweenRestart = parseInt(System.getProperty("sleepBetweenRestartMinutes", "5"));
        boolean shuffle = Boolean.parseBoolean(System.getProperty("shuffle", "true"));
        int durationInMinutes = parseInt(System.getProperty("durationInMinutes", "30")) - VERIFICATION_DURATION_GAP;
        System.out.println("RemoteController will run for " + durationInMinutes);

        long duration = MINUTES.toMillis(durationInMinutes);

        JetInstance jet = JetBootstrap.getInstance();
        HazelcastInstance instance = jet.getHazelcastInstance();
        logger = instance.getLoggingService().getLogger(RemoteControllerClient.class);

        List<Member> members = new ArrayList<>(instance.getCluster().getMembers());
        if (shuffle) {
            Collections.shuffle(members);
        }

        int memberCount = members.size();
        long begin = System.currentTimeMillis();
        sleepMinutes(initialSleep);
        int[] counter = new int[]{0};
        Iterables.cycle(members).forEach(m -> {
            try {
                stop(m, jetHome);
                sleepMinutes(sleepBetweenRestart);
                start(m);
                sleepMinutes(sleepBetweenRestart);

                counter[0]++;
                if (counter[0] % memberCount == 0) {
//                    shutdownCluster(m, jetHome, members);
//                    sleepMinutes(1);
//                    startCluster(members);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            if (System.currentTimeMillis() - begin > duration) {
                System.out.println("Exiting Remote Controller Client");
                System.exit(0);
            }
        });
    }

    private static void startCluster(List<Member> members) throws Exception {
        logger.info("Start cluster");
        members.forEach(m -> uncheckRun(() -> start(m)));
    }

    private static void shutdownCluster(Member member, String jetHome, List<Member> members) throws Exception {
        logger.info("Shutdown cluster");
        String host = member.getAddress().getHost();
        int port = member.getAddress().getPort();
        call(member, jetHome + "/bin/cluster.sh -a " + host + " -p " + port + " -o shutdown");
        members.forEach(m -> uncheckRun(() -> rollLogs(m, jetHome)));
    }

    private static void stop(Member member, String jetHome) throws Exception {
        logger.info("Stopping member[" + member + "]");
        call(member, "sudo initctl stop hazelcast-jet-isolated");
        rollLogs(member, jetHome);
    }

    private static void rollLogs(Member member, String jetHome) throws Exception {
        call(member, "mv " + jetHome + "/logs/hazelcast-jet.log " +
                jetHome + "/logs/hazelcast-jet-" + logCounter + ".log");
        call(member, "mv " + jetHome + "/logs/hazelcast-jet.gc.log " +
                jetHome + "/logs/hazelcast-jet.gc-" + logCounter + ".log");
        logCounter++;
    }

    private static void start(Member member) throws Exception {
        logger.info("Starting member[" + member + "]");
        call(member, "sudo initctl start hazelcast-jet-isolated");
    }

    private static void call(Member member, String command) throws Exception {
        TTransport transport = new TSocket(member.getAddress().getHost(), DEFAULT_PORT);
        transport.open();
        TProtocol protocol = new TBinaryProtocol(transport);
        RemoteController.Client client = new RemoteController.Client(protocol);
        String script = "import subprocess\nprocess = subprocess.call(['" + command + "'], shell=True)";
        client.executeOnController(null, script, Lang.PYTHON);
        transport.close();
    }

    private static void sleepMinutes(int minutes) throws InterruptedException {
        MINUTES.sleep(minutes);
    }
}
