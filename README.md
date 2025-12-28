# PDF to Image Converter API

High-performance Spring Boot REST API for converting PDF files to images with automatic PDF repair.

---

## Features

- ✅ Convert PDFs to JPG/PNG (50-600 DPI)
- ✅ 3-tier PDF repair (QPDF → Ghostscript → DPI fallback)
- ✅ Multi-threaded batch processing
- ✅ ZIP download of all pages
- ✅ RESTful API with job tracking
- ✅ Auto-cleanup after 1 hour

---

## Quick Start

### Option 1: Local Development (Java)

**Prerequisites:** Java 17+, Maven 3.6+, QPDF, Ghostscript

```bash
# Build
mvn clean package

# Run
java -jar target/pdf-converter-api.jar

# Access at http://localhost:8080
```

**Install Repair Tools:**
```bash
# Windows (Chocolatey)
choco install qpdf ghostscript

# Ubuntu/Debian
sudo apt-get install qpdf ghostscript

# macOS (Homebrew)
brew install qpdf ghostscript
```

---

### Option 2: Docker (Recommended 🐳)

**Prerequisites:** Docker only

```bash
# Clone and run
git clone <repo-url>
cd pdf-converter-java
docker-compose up

# Access at http://localhost:8080
```

**Why Docker?**
- ✅ QPDF & Ghostscript pre-installed
- ✅ Works on Windows/Linux/macOS
- ✅ Zero configuration needed
- ✅ Production-ready

---

## API Usage

### Convert PDF
```bash
curl -F "pdf=@sample.pdf" -F "dpi=300" -F "format=jpg" \
  http://localhost:8080/api/convert
```

**Response:**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "success",
  "metadata": {
    "totalPages": 10,
    "successfulPages": 10,
    "failedPages": 0,
    "timeTakenSeconds": 5.2,
    "repairMethod": "qpdf"
  },
  "downloadUrl": "/api/output/550e8400-e29b-41d4-a716-446655440000"
}
```

### Download Result
```bash
curl -O http://localhost:8080/api/output/{jobId}
# Downloads ZIP with all images + metadata.json
```

### Health Check
```bash
curl http://localhost:8080/health
```

---

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/convert` | POST | Convert PDF (params: `pdf`, `dpi`, `format`) |
| `/api/output/{jobId}` | GET | Download converted images as ZIP |
| `/api/help` | GET | API documentation |
| `/health` | GET | Health check |

---

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server
server.port=8080

# File Limits
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

# Job Cleanup
app.job.expiry-hours=1

# PDF Repair (paths auto-detected if installed system-wide)
app.repair.enabled=true
app.repair.qpdf.path=qpdf
app.repair.ghostscript.path=gs
```

---

## How PDF Repair Works

**3-Tier Fallback Strategy:**

1. **PDFBox Direct** → Fast, handles 95% of PDFs
2. **QPDF Repair** → Fixes corrupted structures (+10s)
3. **Ghostscript Fallback** → Comprehensive repair (+45s)
4. **72 DPI Fallback** → Last resort (quality compromise)

**Only activates when pages fail** - no overhead for clean PDFs.

---

## Development

**Build:**
```bash
mvn clean package
```

**Run locally:**
```bash
mvn spring-boot:run
```

**Docker build:**
```bash
docker-compose up --build
```

---

## Troubleshooting

**"Failed to convert PDF"**  
→ Install QPDF/Ghostscript or use Docker

**"File too large"**  
→ Increase limits in `application.properties`

**"Port 8080 in use"**  
→ Change `server.port` in config

**"Job not found"**  
→ Job expired (default 1 hour)

---

## Performance

**660-Page PDF:**
- Direct conversion: ~12s (98% success)
- With QPDF: ~20s (100% success)
- With Ghostscript: ~50s (100% success)

**Optimization:**
- Use 150 DPI for previews
- Use JPG for smaller files
- Docker uses 8 threads automatically

---

## Project Structure

```
pdf-converter-java/
├── src/main/java/com/pdfconverter/
│   ├── api/               # REST controllers & services
│   ├── core/              # PDF conversion logic
│   └── config/            # Spring configuration
├── src/main/resources/
│   └── application.properties
├── Dockerfile             # Docker image definition
├── docker-compose.yml     # Docker orchestration
├── pom.xml
└── README.md
```

---

## License

MIT License

---

**Made with Spring Boot & Apache PDFBox**
