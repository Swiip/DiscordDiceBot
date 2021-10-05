package de.janno.discord;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

public class Metrics {

    public final static String METRIC_PREFIX = "de.janno.bot.";
    public final static String ACTION_TAG = "action";
    public final static String CONFIG_TAG = "config";

    public static void init() {
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        io.micrometer.core.instrument.Metrics.addRegistry(prometheusRegistry);
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/prometheus", httpExchange -> {
                String response = prometheusRegistry.scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            new Thread(server::start).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        new JvmMemoryMetrics().bindTo(globalRegistry);
        new JvmGcMetrics().bindTo(globalRegistry);
        new ProcessorMetrics().bindTo(globalRegistry);
        new UptimeMetrics().bindTo(globalRegistry);
        new JvmThreadMetrics().bindTo(globalRegistry);
        globalRegistry.config().meterFilter(MeterFilter.ignoreTags("uri")); //reduce output of netty

    }

    public static void incrementMetricCounter(String commandName, String action, List<String> config) {
        if (config != null && !config.isEmpty()) {
            globalRegistry.counter(METRIC_PREFIX + commandName, Tags.of(ACTION_TAG, action).and(CONFIG_TAG, config.toString())).increment();
        }
        globalRegistry.counter(METRIC_PREFIX + commandName, Tags.of(ACTION_TAG, action)).increment();
        globalRegistry.counter(METRIC_PREFIX + action).increment();

    }
}
