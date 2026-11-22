# Parallel inverted index search with LLM query expansion

A search server for a large set of English books. The inverted index is built in
parallel by our own thread pool and stored in our own thread-safe data structure.
Before searching, the query is expanded with synonyms by a local LLM (Ollama).
Results are available over a REST API and a WebSocket, with a command-line client
and a small web page.

## Requirements

- Docker with Docker Compose
- about 10 GB of free RAM (server up to 6 GB, Ollama about 2.5 GB)
- Python 3 (for the CLI client and the benchmark scripts)
- JDK 25 (for tests and run without Docker)

## 1. Dataset

The `data/` folder is in the repository, so nothing has to be downloaded. It has 5378 English
books (2.1 GB, UTF-8) from [PG-19](https://github.com/google-deepmind/pg19), a DeepMind dataset
of Project Gutenberg books published before 1919. A file is named by its book id, for example
`11.txt` is "Alice's Adventures in Wonderland". Titles of all books are in `metadata.csv` of PG-19.

The server indexes all `*.txt` files in `data/`, so you can also put your own English `.txt` files there.

Numbers about the dataset (words per book, index size):

```bash
python3 scripts/dataset_stats.py
```

## 2. Start

```bash
docker compose up --build -d
```

This starts three containers:

- `ollama` - the LLM server
- `ollama-pull` - downloads the model `llama3.2:3b` (about 2 GB) on the first start and exits
- `server` - our search server on port 8080, it starts after the model is ready

Check that everything works:

```bash
./cli/search --health
```

Stop: `docker compose down`.

## 3. Usage

The index is kept in memory, so build it after every start of the server:

```bash
./cli/search --index 8        # build the index with 8 threads
./cli/search --stats          # number of documents, build time, threads
./cli/search "car"            # search
./cli/search --help
```

The CLI uses `http://localhost:8080` by default, another address can be set with
`--server` or the `SEARCH_SERVER` variable.

Web UI: open `http://localhost:8080/` in a browser.

## 4. API

| Method | Path | Body | Answer |
|---|---|---|---|
| GET | `/api/health` | - | `{"status":"ok","ollama":true}` |
| POST | `/api/index` | `{"threads":8}` | `{"status":"ok","documents_indexed":20000,"time_ms":22748}` |
| POST | `/api/search` | `{"query":"car","mode":"keyword"}` | `{"results":[{"doc_id":"doc_10521.txt","score":1.0,"snippet":"..."}],"time_ms":812}` |
| GET | `/api/stats` | - | `{"documents":20000,"index_build_time_ms":22748,"threads_used":8}` |

Example:

```bash
curl -X POST localhost:8080/api/search -H 'Content-Type: application/json' -d '{"query":"car","mode":"keyword"}'
```

WebSocket `ws://localhost:8080/ws/search`: send `{"query":"car"}`, the server sends one message per stage (`query_expansion`, `keyword_search`, `done`).

Search returns the 10 best documents, ranked by TF-IDF (Term Frequency – Inverse Document Frequency). Words added by the LLM count half as much as the words from the query. 
If Ollama is not available, the search still works, just without expansion.

## 5. Tests

```bash
cd server
./mvnw test
```

## 6. Running the server without Docker

```bash
docker compose up -d ollama ollama-pull   # only Ollama in Docker
cd server
./mvnw spring-boot:run                    # reads ../data, uses -Xmx6g from pom.xml
```

## 7. Scalability research

Scripts are in `scripts/`, results (CSV, charts, test machine) in `docs/research/`.

```bash
python3 -m venv scripts/.venv
source scripts/.venv/bin/activate
pip install -r scripts/requirements.txt     # matplotlib for the charts
python scripts/bench_index.py               # build time for 1..16 threads
docker compose stop ollama
python scripts/bench_search.py no_ollama    # search throughput without the LLM
docker compose start ollama
python scripts/bench_search.py with_ollama  # with the LLM
python scripts/plot_results.py              # charts
```

Results on Intel i5-10600KF with 16GB RAM 3200MHz:

- index build: 105s on 1 thread, 22.7s on 8 threads (4.6 times faster). more than 8 threads is slower
- search without the LLM: up to about 230 requests per second
- search with the LLM: about 1.3 requests per second, as Ollama answers one request at a time (and was tested on CPU)

## 8. Project structure

```
server/            Spring Boot server
  concurrent/      ThreadPool
  index/           InvertedIndex, DocumentStore, Tokenizer, IndexingService
  search/          SearchService, Ranker (TF-IDF), SnippetBuilder
  ollama/          OllamaClient, QueryExpander
  api/             REST controllers, WebSocket handler
  resources/static/index.html   web UI
cli/search         command-line client
data/              dataset (5378 books from PG-19)
scripts/           dataset statistics, benchmark and chart scripts
docs/uml/          UML diagrams
docs/research/     benchmark results and charts
docker-compose.yml
```

Commit style [Conventional Commits](https://www.conventionalcommits.org/), see `CONTRIBRULES.md`.
