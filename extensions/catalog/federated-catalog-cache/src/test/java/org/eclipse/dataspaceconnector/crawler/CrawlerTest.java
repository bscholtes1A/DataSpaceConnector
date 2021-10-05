package org.eclipse.dataspaceconnector.crawler;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.*;

class CrawlerTest {
    private Crawler crawler;
    private CrawlerProtocolAdapter protocolAdapterMock;
    private ArrayBlockingQueue<UpdateResponse> queue;
    private List<CrawlerProtocolAdapter> adapters;
    private Monitor monitorMock;

    @BeforeEach
    void setUp() {
        protocolAdapterMock = strictMock(CrawlerProtocolAdapter.class);
        queue = new ArrayBlockingQueue<>(3);
        adapters = new ArrayList<>();
        adapters.add(protocolAdapterMock);
        monitorMock = niceMock(Monitor.class);
        crawler = new Crawler(adapters, monitorMock, queue, createRetryPolicy());
    }

    @Test
    @DisplayName("Should insert one item into queue when request succeeds")
    void shouldInsertInQueue_whenSucceeds() {
        expect(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).andReturn(CompletableFuture.supplyAsync(UpdateResponse::new));
        replay(protocolAdapterMock);

        crawler.run();

        assertThat(queue).hasSize(1);
        verify(protocolAdapterMock);
    }

    @Test
    @DisplayName("Should insert only those items into queue that have succeeded")
    void shouldInsertInQueue_onlySuccessfulRequests() {

        CrawlerProtocolAdapter secondAdapter = strictMock(CrawlerProtocolAdapter.class);
        adapters.add(secondAdapter);

        expect(protocolAdapterMock.sendRequest(isA(UpdateRequest.class))).andReturn(CompletableFuture.supplyAsync(UpdateResponse::new));
        replay(protocolAdapterMock);
        expect(secondAdapter.sendRequest(isA(UpdateRequest.class))).andReturn(CompletableFuture.failedFuture(new RuntimeException()));
        replay(secondAdapter);

        crawler.run();

        assertThat(queue).hasSize(1);
        verify(protocolAdapterMock);
        verify(secondAdapter);
    }

    private RetryPolicy<Object> createRetryPolicy() {
        return new RetryPolicy<>().withMaxRetries(1);
    }
}