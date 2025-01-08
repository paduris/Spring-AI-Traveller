package com.spring.ai.springaitraveller.service;

import com.spring.ai.springaitraveller.model.SearchResponse;
import com.spring.ai.springaitraveller.model.TravelData;
import groovy.util.logging.Slf4j;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TravelDataService {

    private static final Logger logger = LoggerFactory.getLogger(TravelDataService.class);
    private static final int TOP_K_RESULTS = 3;

    private final MilvusServiceClient milvusClient;
    private final ChatClient chatClient;
    private final EmbeddingClient embeddingClient;
    private final Map<String, TravelData> dataStore = new HashMap<>();

    @Value("${milvus.collection}")
    private String collectionName;

    public TravelDataService(MilvusServiceClient milvusClient,
                             ChatClient chatClient,
                             EmbeddingClient embeddingClient) {
        this.milvusClient = milvusClient;
        this.chatClient = chatClient;
        this.embeddingClient = embeddingClient;
    }

    public void ingestData(List<TravelData> travelDataList) {
        try {
            logger.info("Starting ingestion of {} travel destinations", travelDataList.size());

            // Store data in local map for quick retrieval
            travelDataList.forEach(data -> dataStore.put(data.getId(), data));

            // Generate embeddings and convert to float values
            List<List<Float>> embeddings = travelDataList.stream()
                    .map(data -> {
                        List<Double> embedding = embeddingClient.embed(data.getContentForEmbedding());
                        return embedding.stream()
                                .map(Double::floatValue)
                                .collect(Collectors.toList());
                    })
                    .collect(Collectors.toList());

            // Extract IDs for insertion
            List<String> ids = travelDataList.stream()
                    .map(TravelData::getId)
                    .collect(Collectors.toList());

            // Prepare fields for insertion
            List<InsertParam.Field> fields = Arrays.asList(
                    new InsertParam.Field("id", ids),
                    new InsertParam.Field("embedding", embeddings)
            );

            // Create insert parameters
            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();

            R<MutationResult> insertResponse = milvusClient.insert(insertParam);
            if (insertResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("Failed to insert data into Milvus: " + insertResponse.getMessage());
            }

            // Load collection for searching
            LoadCollectionParam loadParam = LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withSyncLoad(true)
                    .build();

            R<RpcStatus> loadResponse = milvusClient.loadCollection(loadParam);
            if (loadResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("Failed to load collection: " + loadResponse.getMessage());
            }

            logger.info("Successfully ingested {} travel destinations", travelDataList.size());

        } catch (Exception e) {
            logger.error("Error during data ingestion", e);
            throw new RuntimeException("Error during data ingestion: " + e.getMessage(), e);
        }
    }



    public SearchResponse searchTravel(String query) {
        try {
            logger.info("Processing search query: {}", query);

            // Generate embedding for the search query and convert to float values
            List<Double> doubleEmbedding = embeddingClient.embed(query);
            List<Float> queryEmbedding = doubleEmbedding.stream()
                    .map(Double::floatValue)
                    .collect(Collectors.toList());

            // Prepare search vectors for Milvus
            List<List<Float>> searchVectors = Collections.singletonList(queryEmbedding);

            // Create search parameters
            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withMetricType(MetricType.L2)
                    .withOutFields(Collections.singletonList("id"))
                    .withTopK(TOP_K_RESULTS)
                    .withVectors(searchVectors)
                    .withVectorFieldName("embedding")
                    .withParams("{\"nprobe\":10}")  // Search parameter for IVF_FLAT index
                    .build();

            // Execute the search operation
            R<SearchResults> searchResponse = milvusClient.search(searchParam);
            if (searchResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("Vector search operation failed: " + searchResponse.getMessage());
            }

            // Extract results using the wrapper
            SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResponse.getData().getResults());
            //List<String> matchingIds = wrapper.getFieldData("id", Integer.class);
            List<?> matchingIds = wrapper.getFieldData("id", 0);
            // Retrieve the full travel data from our local cache
            List<TravelData> relevantDestinations = matchingIds.stream()
                    .map(dataStore::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Build context from relevant destinations for AI processing
            String context = relevantDestinations.stream()
                    .map(TravelData::getContentForEmbedding)
                    .collect(Collectors.joining("\n\n"));

            // Generate AI recommendation based on search results
            String promptText = String.format("""
            Based on the following travel destinations and the user's query: '%s',
            provide a helpful response suggesting the most relevant destination(s).
            Include specific details from the context to support your recommendations.
            
            Context:
            %s
            """, query, context);

            String aiResponse = chatClient.call(new Prompt(promptText))
                    .getResult()
                    .getOutput()
                    .getContent();

            logger.info("Successfully processed search query with {} relevant destinations found",
                    relevantDestinations.size());

            return SearchResponse.builder()
                    .query(query)
                    .aiResponse(aiResponse)
                    .relevantDestinations(relevantDestinations)
                    .build();

        } catch (Exception e) {
            logger.error("Error during travel search operation", e);
            throw new RuntimeException("Travel search operation failed: " + e.getMessage(), e);
        }
    }
}
