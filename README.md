# Splice

**High-concurrency document ingestion engine for RAG pipelines**

[![Java](https://img.shields.io/badge/Java-21_(LTS)-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Concurrency](https://img.shields.io/badge/Virtual_Threads-Project_Loom-E05D44?style=for-the-badge&logo=java&logoColor=white)](https://openjdk.org/jeps/444)
[![Build](https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![PDF Engine](https://img.shields.io/badge/Apache_PDFBox-Stream_Engine-1976D2?style=for-the-badge&logo=apache&logoColor=white)](https://pdfbox.apache.org/)
[![Testing](https://img.shields.io/badge/JUnit-5-25A162?style=for-the-badge&logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![License](https://img.shields.io/badge/License-PolyForm-333333?style=for-the-badge)](LICENSE)

---

**Splice** optimizes infrastructure costs by intelligently routing document pages between local extraction (CPU) and external OCR services (GPU/API) based on content complexity.

## Key Features

* **Hybrid Extraction:** Automatically detects scanned pages vs. native text.
    * *Native Text:* Processed locally via PDFBox (Zero cost, <10ms latency).
    * *Visual/Scanned:* Tagged and routed for OCR/Vision API processing.
* **Virtual Thread Concurrency:** Built on Java 21 `StructuredTaskScope` to handle thousands of concurrent file I/O operations with minimal memory footprint.
* **Vector-Ready Output:** Produces normalized JSON compatible with major Vector Databases (Pinecone, Weaviate).

```mermaid
graph LR
    A[PDF Input] --> B{"Complexity Analysis"}
    B -- Native Text --> C["Local Parser (Java)"]
    B -- Scanned/Image --> D["OCR Service (API)"]
    C --> E[Normalized JSON]
    D --> E
````

## Building from Source

**Prerequisites:**

* Java 21 (LTS)
* Maven 3.9+

<!-- end list -->

```bash
git clone https://github.com/jonathan-ngamboe/splice.git
cd splice
mvn clean package
```

> The executable JAR will be generated in the `target/` directory.

## Usage

Splice runs as a CLI tool.

```bash
java -jar target/splice-1.0.jar --input ./data/documents --output ./results.json
```

### Configuration Options

| Flag | Description | Default |
| :--- | :--- | :--- |
| `-i`, `--input` | Path to source directory or file. | **Required** |
| `-o`, `--output` | Path to destination JSON file. | **Required** |
| `--threads` | Max concurrent virtual threads. | Auto-detect |
| `--threshold` | Image/Text ratio to trigger OCR routing. | `0.2` |

### Output Format

Splice produces a normalized JSON structure regardless of the processing path:

```json
[
  {
    "source": "contract_v2.pdf",
    "page": 1,
    "type": "TEXT",
    "content": "SECTION 1: DEFINITIONS..."
  },
  {
    "source": "contract_v2.pdf",
    "page": 2,
    "type": "IMAGE",
    "metadata": { "resolution": "300dpi", "size": "2MB" },
    "content_ref": "extracts/img_p2_hash.png"
  }
]
```

## Performance Benchmark

*Environment: [To be defined]*
*Dataset: [To be defined]*

| Metric | Splice (Java 21) |
| :--- | :--- |
| **Throughput (Pages/sec)** | *TBD* |
| **Memory Footprint** | *TBD* |
| **Processing Time (1GB)** | *TBD* |

## Roadmap

- [x] **Core Engine:** PDF parsing and content stream interception.
- [x] **Smart Detection:** Text vs. Image classification logic.
- [x] **Batch Processing:** Virtual Thread implementation for directory scanning.
- [ ] **CLI:** Picocli integration for argument parsing.
- [ ] **OCR Integration:** Connector for AWS Textract / OpenAI Vision.

## License

Distributed under the **PolyForm Noncommercial License 1.0.0**. See `LICENSE` file for more information.