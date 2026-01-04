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

**Splice** leverages native hardware acceleration (via ONNX Runtime) for layout analysis. Ensure your system meets the following requirements:

* **Java:** JDK 21 (LTS) or higher.
* **Build Tool:** Maven 3.9+.
* **Native Dependencies (OS specific):**
  * **Windows:** [Visual C++ Redistributable 2019+](https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist) (Required for DLL execution).
  * **Linux:** `libgomp1` (OpenMP support, usually pre-installed on desktop distros).
    * *Debian/Ubuntu/Docker:* `apt-get install libgomp1`
  * *macOS:* No additional dependencies required (universal binaries included).

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
java -jar target/splice-1.0.jar --input ./data/documents --output ./results
```

### Configuration Options

| Flag | Description | Default |
| --- | --- | --- |
| `-i`, `--input` | Path to source directory or file. | **Required** |
| `-o`, `--output` | **Directory** where the JSON report and extracted images will be saved. | **Required** |
| `-r`, `--recursive` | Process subdirectories recursively if input is a directory. | `false` |
| `--threads` | Max concurrent virtual threads. | Auto-detect |
| `--threshold` | Image/Text ratio to trigger OCR routing. | `0.2` |

### Output Format

Splice produces a list of `DocumentElement` objects, preserving layout and structure:

```json
[
  {
    "id": "f4073ab5-0399...",
    "type": "TEXT",
    "location": {
      "pageNumber": 1,
      "bbox": { "x": 36.0, "y": 74.8, "width": 453.3, "height": 13.6 }
    },
    "content": {
      "contentType": "TEXT",
      "text": "Docker provides the ability to package and run an application..."
    }
  },
  {
    "id": "bd0e6c79-ca4e...",
    "type": "TABLE",
    "location": {
      "pageNumber": 1,
      "bbox": { "x": 36.0, "y": 273.3, "width": 411.0, "height": 8.6 }
    },
    "content": {
      "contentType": "TABLE",
      "csvData": "INSTALLATION,GENERAL COMMAND\nDocker Desktop,[https://docs.docker.com/desktop](https://docs.docker.com/desktop)"
    }
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
- [x] **CLI:** Picocli integration for argument parsing.
- [ ] **OCR Integration:** Connector for AWS Textract / OpenAI Vision.

## License

Distributed under the **PolyForm Noncommercial License 1.0.0**. See `LICENSE` file for more information.