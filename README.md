# Spring-AI-Traveller

The application implements:

Vector similarity search using Milvus
RAG (Retrieval-Augmented Generation) using OpenAI embeddings and chat completions
Data ingestion with vector storage
Semantic search capabilities

The search endpoint returns both relevant destinations and an AI-generated response that considers the context of the matched destinations.

Ingest sample data:

curl -X POST http://localhost:8080/api/travel/ingest \
-H "Content-Type: application/json" \
-d '[
{
"id": "1",
"destination": "Paris, France",
"description": "Experience the romance of the City of Light. Visit iconic landmarks like the Eiffel Tower, Louvre Museum, and Notre-Dame Cathedral. Enjoy world-class cuisine, art galleries, and charming cafes along cobblestone streets.",
"type": "City Break",
"bestTimeToVisit": "April-June, September-October",
"avgCostPerDay": 150,
"popularAttractions": ["Eiffel Tower", "Louvre Museum", "Notre-Dame Cathedral", "Champs-Élysées", "Montmartre"]
}
]'


Search for travel recommendations:
**curl "http://localhost:8080/api/travel/search?query=I%20want%20to%20visit%20a%20romantic%20city%20with%20great%20food%20and%20art"**