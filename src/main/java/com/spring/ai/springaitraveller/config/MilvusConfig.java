package com.spring.ai.springaitraveller.config;


import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.index.CreateIndexParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;

@Configuration
public class MilvusConfig {

    @Value("${milvus.host}")
    private String host;

    @Value("${milvus.port}")
    private Integer port;

    @Value("${milvus.collection}")
    private String collectionName;

    @Value("${milvus.dimension}")
    private Integer dimension;

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build();

        MilvusServiceClient client = new MilvusServiceClient(connectParam);
        initializeCollection(client);
        return client;
    }

    private void initializeCollection(MilvusServiceClient client) {
        try {
            R<Boolean> response = client.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );

            if (response.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("Failed to check collection existence");
            }

            if (!response.getData()) {
                FieldType idField = FieldType.newBuilder()
                        .withName("id")
                        .withDescription("ID field")
                        .withDataType(DataType.VarChar)
                        .withMaxLength(100)
                        .withPrimaryKey(true)
                        .withAutoID(false)
                        .build();

                FieldType embeddingField = FieldType.newBuilder()
                        .withName("embedding")
                        .withDescription("Vector embeddings")
                        .withDataType(DataType.FloatVector)
                        .withDimension(dimension)
                        .build();

                CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withDescription("Travel data embeddings collection")
                        .withShardsNum(2)
                        .withFieldTypes(Arrays.asList(idField, embeddingField))
                        .build();

                R<RpcStatus> createResponse = client.createCollection(createCollectionParam);
                if (createResponse.getStatus() != R.Status.Success.getCode()) {
                    throw new RuntimeException("Failed to create collection: " + createResponse.getMessage());
                }

                String extraParam = "{\"nlist\":1024}";
                CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withFieldName("embedding")
                        .withIndexType(IndexType.IVF_FLAT)
                        .withMetricType(MetricType.L2)
                        .withExtraParam(extraParam)
                        .withSyncMode(Boolean.TRUE)
                        .build();

                R<RpcStatus> indexResponse = client.createIndex(indexParam);
                if (indexResponse.getStatus() != R.Status.Success.getCode()) {
                    throw new RuntimeException("Failed to create index: " + indexResponse.getMessage());
                }

                LoadCollectionParam loadParam = LoadCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withSyncLoad(true)
                        .withReplicaNumber(1)
                        .build();

                R<RpcStatus> loadResponse = client.loadCollection(loadParam);
                if (loadResponse.getStatus() != R.Status.Success.getCode()) {
                    throw new RuntimeException("Failed to load collection: " + loadResponse.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Milvus collection: " + e.getMessage(), e);
        }
    }
}