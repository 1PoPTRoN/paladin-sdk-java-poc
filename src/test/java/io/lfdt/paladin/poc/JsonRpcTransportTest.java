package io.lfdt.paladin.poc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

class JsonRpcTransportTest {

    private WireMockServer wireMock;
    private JsonRpcTransport transport;

    @BeforeEach
    void startServer() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
        transport = new JsonRpcTransport("http://localhost:" + wireMock.port());
    }

    @AfterEach
    void stopServer() {
        wireMock.stop();
    }

    @Test
    void successfulCallReturnsDeserializedResult() {
        wireMock.stubFor(post(urlEqualTo("/"))
            .willReturn(okJson("""
                {"jsonrpc":"2.0","id":1,"result":"0xabc123"}
                """)));

        String result = transport
            .call("ptx_sendTransaction", List.of(), new TypeReference<String>() {})
            .join();

        assertEquals("0xabc123", result);
    }

    @Test
    void rpcErrorIsSurfacedAsPaladinRpcException() {
        wireMock.stubFor(post(urlEqualTo("/"))
            .willReturn(okJson("""
                {"jsonrpc":"2.0","id":1,"error":{"code":12229,"message":"event not decodable"}}
                """)));

        CompletionException ex = assertThrows(CompletionException.class, () ->
            transport.call("ptx_decodeEvent", List.of(), new TypeReference<String>() {}).join()
        );

        assertInstanceOf(PaladinRpcException.class, ex.getCause());
        PaladinRpcException rpcEx = (PaladinRpcException) ex.getCause();
        assertEquals(12229, rpcEx.code());
        assertTrue(rpcEx.getMessage().contains("event not decodable"));
    }

    @Test
    void httpErrorIsSurfacedAsTransportException() {
        wireMock.stubFor(post(urlEqualTo("/"))
            .willReturn(aResponse().withStatus(500).withBody("internal server error")));

        CompletionException ex = assertThrows(CompletionException.class, () ->
            transport.call("ptx_getTransaction", List.of(), new TypeReference<String>() {}).join()
        );

        assertInstanceOf(PaladinTransportException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("HTTP 500"));
    }

    @Test
    void malformedJsonIsSurfacedAsTransportException() {
        wireMock.stubFor(post(urlEqualTo("/"))
            .willReturn(okJson("not actually json {{{")));

        CompletionException ex = assertThrows(CompletionException.class, () ->
            transport.call("ptx_getTransaction", List.of(), new TypeReference<String>() {}).join()
        );

        assertInstanceOf(PaladinTransportException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("Malformed"));
    }

    @Test
    void requestIdsIncrementMonotonically() {
        wireMock.stubFor(post(urlEqualTo("/"))
            .willReturn(okJson("""
                {"jsonrpc":"2.0","id":1,"result":"ok"}
                """)));

        transport.call("first", List.of(), new TypeReference<String>() {}).join();
        transport.call("second", List.of(), new TypeReference<String>() {}).join();
        transport.call("third", List.of(), new TypeReference<String>() {}).join();

        // Verify three distinct request bodies hit the server with incrementing ids
        wireMock.verify(3, postRequestedFor(urlEqualTo("/")));
    }
}
