# PDF to Image Converter API

A high-performance Spring Boot REST API for converting PDF files to high-quality images with advanced PDF repair capabilities.

---

## 📋 Table of Contents

- [Features](#-features)
- [Prerequisites](#-prerequisites)
- [Quick Start](#-quick-start)
- [API Endpoints](#-api-endpoints)
- [Configuration](#-configuration)
- [PDF Repair Tools](#-pdf-repair-tools-optional)
- [Usage Examples](#-usage-examples)
- [Docker Support](#-docker-support)
- [Development](#-development)
- [Troubleshooting](#-troubleshooting)
- [Performance](#-performance)
- [License](#-license)

---

## ✨ Features

✅ **High-Quality Conversion** - Convert PDFs to JPG or PNG with configurable DPI (50-600)  
✅ **PDF Repair** - 3-tier repair strategy for corrupted/problematic PDFs  
✅ **Job Tracking** - UUID-based job tracking with automatic cleanup  
✅ **Batch Processing** - Convert multi-page PDFs efficiently  
✅ **RESTful API** - Simple, intuitive REST endpoints  
✅ **ZIP Downloads** - Download all pages as a single ZIP file  
✅ **CORS Enabled** - Ready for frontend integration  
✅ **File Management** - Automatic cleanup after 1 hour (configurable)  
✅ **Metadata** - Detailed conversion statistics and file information  
✅ **Health Checks** - Monitor API status and active jobs  

---

## 📦 Prerequisites

### Required
- **Java 17+** - [Download](https://adoptium.net/)
- **Maven 3.6+** - [Download](https://maven.apache.org/download.cgi)

### Optional (for PDF Repair)
- **QPDF** - Fast PDF repair ([Installation Guide](SETUP.md#windows-installation))
- **Ghostscript** - Comprehensive PDF repair ([Installation Guide](SETUP.md#linux-installation))

---

## 🚀 Quick Start

### 1. Clone and Build
```bash
git clone <repository-url>
cd pdf-converter-java
mvn clean package
```

### 2. Run the API
```bash
java -jar target/pdf-converter-api.jar
```
**Or** with Maven:
```bash
mvn spring-boot:run
```

### 3. Verify
API will be available at: **`http://localhost:8080`**

Test health endpoint:
```bash
curl http://localhost:8080/health
```

📖 **For detailed setup including PDF repair tools, see [SETUP.md](SETUP.md)**

---

## 🔌 API Endpoints

### 1️⃣ Convert PDF to Images

**POST** `/api/convert`

Upload and convert a PDF file to images.

**Parameters:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `pdf` | File | ✅ | PDF file to convert |
| `dpi` | Integer | ❌ | DPI quality (50-600, default: 150) |
| `format` | String | ❌ | Output format: `jpg` or `png` (default: jpg) |

**Example Request:**
```bash
curl -X POST http://localhost:8080/api/convert \
  -F "pdf=@document.pdf" \
  -F "dpi=300" \
  -F "format=png"
```

**Success Response (200 OK):**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "success",
  "metadata": {
    "totalPages": 10,
    "successfulPages": 10,
    "failedPages": 0,
    "timeTakenSeconds": 5.2,
    "dpi": 300,
    "format": "png",
    "repairMethod": "none",
    "files": ["page-001.png", "page-002.png", ...]
  },
  "downloadUrl": "/api/output/550e8400-e29b-41d4-a716-446655440000"
}
```

---

### 2️⃣ Download Converted Files

**GET** `/api/output/{jobId}`

Download all converted images as a ZIP file.

**Example:**
```bash
curl -O http://localhost:8080/api/output/550e8400-e29b-41d4-a716-446655440000
```

**ZIP Contents:**
- `page-001.{format}`, `page-002.{format}`, ...
- `metadata.json` - Conversion details

---

### 3️⃣ API Help

**GET** `/api/help`

Get detailed API documentation.

```bash
curl http://localhost:8080/api/help
```

---

### 4️⃣ Health Check

**GET** `/health`

Check API status and active jobs.

```bash
curl http://localhost:8080/health
```

**Response:**
```json
{
  "status": "healthy",
  "activeJobs": 3,
  "api": "running",
  "repairTools": {
    "qpdf": "available",
    "ghostscript": "available"
  }
}
```

---

## ⚙️ Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8080

# File Upload Limits
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

# Storage Paths
app.upload.dir=uploads
app.output.dir=outputs

# Job Management
app.job.expiry-hours=1

# PDF Repair (requires external tools)
app.repair.enabled=true
app.repair.qpdf.path=qpdf
app.repair.ghostscript.path=gs
app.repair.timeout-seconds=300
```

---

## 🛠️ PDF Repair Tools (Optional)

Install PDF repair tools for **100% page recovery** with problematic PDFs.

### Quick Install

**Windows (Chocolatey):**
```powershell
choco install qpdf ghostscript
```

**Ubuntu/Debian:**
```bash
sudo apt-get install qpdf ghostscript
```

**macOS (Homebrew):**
```bash
brew install qpdf ghostscript
```

📖 **See [SETUP.md](SETUP.md) for detailed installation and configuration**

### Repair Strategy
1. **Direct conversion** - Fast (0 overhead)
2. **QPDF repair** - If pages fail (+6-10s)
3. **Ghostscript fallback** - If QPDF fails (+30-45s)

✅ **Only repairs when needed** - No performance penalty for clean PDFs

---

## 💡 Usage Examples

### cURL Examples

**Basic Conversion:**
```bash
curl -X POST http://localhost:8080/api/convert \
  -F "pdf=@sample.pdf"
```

**High-Quality PNG:**
```bash
curl -X POST http://localhost:8080/api/convert \
  -F "pdf=@document.pdf" \
  -F "dpi=600" \
  -F "format=png"
```

**Download Result:**
```bash
curl -O http://localhost:8080/api/output/{jobId}
```

---

### JavaScript/Fetch Example

```javascript
// Upload and convert
const formData = new FormData();
formData.append('pdf', pdfFile); // File from <input type="file">
formData.append('dpi', '300');
formData.append('format', 'png');

const response = await fetch('http://localhost:8080/api/convert', {
  method: 'POST',
  body: formData
});

const result = await response.json();

if (result.status === 'success') {
  console.log(`Converted ${result.metadata.totalPages} pages`);
  console.log(`Download: ${result.downloadUrl}`);
  
  // Trigger download
  window.location.href = `http://localhost:8080${result.downloadUrl}`;
} else {
  console.error('Conversion failed:', result.errors);
}
```

---

### Python Example

```python
import requests

# Convert PDF
with open('document.pdf', 'rb') as pdf_file:
    files = {'pdf': pdf_file}
    data = {'dpi': '300', 'format': 'png'}
    
    response = requests.post(
        'http://localhost:8080/api/convert',
        files=files,
        data=data
    )
    
    result = response.json()
    job_id = result['jobId']
    print(f"Job ID: {job_id}")

# Download result
download_response = requests.get(
    f'http://localhost:8080/api/output/{job_id}',
    stream=True
)

with open('output.zip', 'wb') as f:
    for chunk in download_response.iter_content(chunk_size=8192):
        f.write(chunk)
```

---

## 🐳 Docker Support

### Build Docker Image
```bash
docker build -t pdf-converter-api .
```

### Run Container
```bash
docker run -p 8080:8080 pdf-converter-api
```

### Docker Compose
```yaml
version: '3.8'
services:
  pdf-converter:
    build: .
    ports:
      - "8080:8080"
    volumes:
      - ./uploads:/app/uploads
      - ./outputs:/app/outputs
    environment:
      - SERVER_PORT=8080
      - APP_REPAIR_ENABLED=true
```

---

## 🔧 Development

### Project Structure
```
pdf-converter-java/
├── src/main/java/com/pdfconverter/
│   ├── controller/      # REST controllers
│   ├── service/         # Business logic
│   ├── config/          # Configuration classes
│   └── model/           # Response models
├── src/main/resources/
│   └── application.properties
├── pom.xml
└── README.md
```

### Build Commands

**Clean build:**
```bash
mvn clean package
```

**Skip tests:**
```bash
mvn clean package -DskipTests
```

**Run locally:**
```bash
mvn spring-boot:run
```

**Run JAR:**
```bash
java -jar target/pdf-converter-api.jar
```

### Output Location
- **JAR file:** `target/pdf-converter-api.jar`
- **Uploads:** `uploads/{jobId}/`
- **Outputs:** `outputs/{jobId}/`

---

## 🐛 Troubleshooting

### Common Issues

#### ❌ "Failed to convert PDF"
**Cause:** Corrupted or password-protected PDF  
**Solution:** Install PDF repair tools ([SETUP.md](SETUP.md))

#### ❌ "File too large"
**Cause:** PDF exceeds 50MB limit  
**Solution:** Increase limit in `application.properties`:
```properties
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
```

#### ❌ "Job not found"
**Cause:** Job expired (default: 1 hour)  
**Solution:** Download files sooner or increase expiry:
```properties
app.job.expiry-hours=24
```

#### ❌ "Port 8080 already in use"
**Cause:** Another service using port 8080  
**Solution:** Change port in `application.properties`:
```properties
server.port=9090
```

### Error Codes

| Status Code | Description |
|------------|-------------|
| **200** | Success |
| **400** | Invalid parameters or file format |
| **404** | Job not found or expired |
| **413** | File too large |
| **500** | Internal server error (conversion failed) |

---

## 📊 Performance

### Benchmarks
**660-Page PDF:**
- Without repair tools: ~12s (98% success)
- With QPDF: ~20s (100% success)
- With Ghostscript: ~50s (100% success)

### Optimization Tips
1. Use lower DPI for previews (150 DPI)
2. Use JPG for smaller file sizes
3. Install QPDF for faster PDF repair
4. Increase JVM heap size for large PDFs:
   ```bash
   java -Xmx2g -jar pdf-converter-api.jar
   ```

---

## 📄 License

This project is licensed under the MIT License.

---

## 🤝 Contributing

Contributions are welcome! Please open an issue or submit a pull request.

---

## 📞 Support

For detailed setup instructions, see [SETUP.md](SETUP.md)

For issues and questions, please open a GitHub issue.

---

**Made with ❤️ using Spring Boot & Apache PDFBox**
