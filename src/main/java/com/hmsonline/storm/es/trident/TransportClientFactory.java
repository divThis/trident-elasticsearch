package com.hmsonline.storm.es.trident;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportClientFactory implements ClientFactory {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(TransportClientFactory.class);
    public static final int DEFAULT_PORT = 9300;
    private static Map<String, TransportClient> CLIENTS = new HashMap<String, TransportClient>();
    private static Object MUTEX = new Object();

    @Override
    @SuppressWarnings("rawtypes")
    public Client makeClient(Map conf) {
        Settings.Builder settingsBuilder = Settings.builder();
        settingsBuilder.put("cluster.name", (String) conf.get(CLUSTER_NAME));
        
        synchronized (MUTEX) {
            String clusterHosts = (String) conf.get(CLUSTER_HOSTS);
            TransportClient client = CLIENTS.get(clusterHosts);
            if (client == null) {
                client = TransportClient.builder().settings(settingsBuilder).build();

                LOG.info("Creating TransportClient with addresses: '{}'", clusterHosts);

                // expecting "host:port,host2:port2,host3"
                if (!StringUtils.isEmpty(clusterHosts)) {
                    String[] hostPorts = StringUtils.split(clusterHosts, ",");
                    for (String hostPortStr : hostPorts) {
                        String[] hostPort = StringUtils.split(hostPortStr, ":");

                        InetAddress address;

                        try {
                            address = InetAddress.getByName(hostPort[0]);
                        } catch (UnknownHostException e) {
                            throw new IllegalStateException("Unknown host: '" + hostPort[0], e);
                        }

                        if (hostPort.length == 2) {
                            client.addTransportAddress(new InetSocketTransportAddress(address, Integer
                                    .parseInt(hostPort[1])));
                        } else if (hostPort.length == 1) {
                            client.addTransportAddress(new InetSocketTransportAddress(address, DEFAULT_PORT));
                        }
                    }

                } else {
                    throw new IllegalStateException("Settings for '" + CLUSTER_HOSTS
                            + "' can not be empty when using the Transport Client");
                }
                CLIENTS.put(clusterHosts, client);
            }
            return client;
        }        
    }
}
