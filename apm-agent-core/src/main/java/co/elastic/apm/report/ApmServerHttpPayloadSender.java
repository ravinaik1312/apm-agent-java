package co.elastic.apm.report;

import co.elastic.apm.impl.Transaction;
import co.elastic.apm.impl.TransactionPayload;
import co.elastic.apm.report.serialize.PayloadSerializer;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okio.BufferedSink;

import java.io.IOException;

public class ApmServerHttpPayloadSender implements PayloadSender {
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");
    private final OkHttpClient httpClient;
    private final ReporterConfiguration reporterConfiguration;
    private final PayloadRequestBody body;

    public ApmServerHttpPayloadSender(OkHttpClient httpClient, PayloadSerializer payloadSerializer,
                                      ReporterConfiguration reporterConfiguration) {
        this.httpClient = httpClient;
        this.reporterConfiguration = reporterConfiguration;
        this.body = new PayloadRequestBody(payloadSerializer);
    }

    @Override
    public void sendPayload(final TransactionPayload payload) {
        // this is ok as its only executed single threaded
        body.payload = payload;
        okhttp3.Request request = new okhttp3.Request.Builder()
            .url(reporterConfiguration.getServerUrl() + "/v1/transactions")
            .post(body)
            .build();

        try {
            httpClient.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class PayloadRequestBody extends RequestBody {
        private final PayloadSerializer payloadSerializer;
        TransactionPayload payload;

        public PayloadRequestBody(PayloadSerializer payloadSerializer) {
            this.payloadSerializer = payloadSerializer;
        }

        @Override
        public MediaType contentType() {
            return MEDIA_TYPE_JSON;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            payloadSerializer.serializePayload(sink, payload);
            sink.close();
            for (Transaction transaction : payload.getTransactions()) {
                transaction.recycle();
            }
        }
    }
}