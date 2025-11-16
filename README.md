# Trade Optimization Pathfinder (TOP)

*An all-in-one Tariff Management Platform to streamline the process of calculating tariffs and optimizing trade routes between countries.*
---
[![Watch the video](https://img.youtube.com/vi/S8zH2_qEYFs/0.jpg)](https://www.youtube.com/watch?v=S8zH2_qEYFs)
---

## 🌍 Problem Statement
Design and implement a system called **TARIFF** (*Trade Agreements Regulating Imports and Foreign Fees*):  
Its purpose is to define import tariffs (and/or additional fees) and calculate them across industries or products between different countries at any given time.

---

## 💡 Our Solution
**Trade Optimization Pathfinder (TOP)** simplifies and unifies the Tariff Calculation and Management process by providing:
- 🧮 **Accurate trade route tariff calculations**
- 🧑‍💼 **Admin management tools** for tariffs
- 🌐 **Interactive route visualization** for smarter coordination

---

## 🎯 Features

### Baseline Tariff Calculation Process
<img width="300" height="355" alt="image" src="https://github.com/user-attachments/assets/3a2636c5-0ebe-4432-859d-668e9f5abbe0" />

**Step 1:** Enter source and destination countries to search for goods using HSCode or descriptions  
<img width="310" height="327.5" alt="image" src="https://github.com/user-attachments/assets/5a4de1f6-6702-415f-a030-c35f7dbdfe7e" />

**Step 2:** System returns available tariff(s). Select from dropdown and enter product quantity  
**Step 3:** Choose desired currency conversion and calculate the result

---

## 💱 Exchange Rate Conversion
Exchange rates are pulled hourly from **ExchangeRateAPI** and cached in **Redis**, ensuring both **speed** and **accuracy** without straining API limits.

---

## 🧩 Explainable Calculation
TOP transparently breaks down the formula and displays each step:
- Builds user trust in the final calculation  
- Helps learners understand tariff computation mechanics  

---

## ✅ Compliance Checklist
<img width="500" height="410" alt="image" src="https://github.com/user-attachments/assets/b454477a-000a-4f65-84b5-25b78d1360fb" />

Provides users with post-calculation compliance requirements such as declarations and certifications.  
Built using **Pinecone + OpenAI** for semantic search with JSON-structured results for the frontend.  
All items are sourced from official government reports with **direct source links** for verification.

---

## 🗺️ Route Visualization
<img width="650" height="220" alt="image" src="https://github.com/user-attachments/assets/edd0eb3e-d2e7-4df7-a15d-7bd8c49e0870" />

Displays four optimized route types:
- ⚠️ **Risk** – lowest likelihood of incidents  
- 💸 **Cost** – cheapest route  
- 🌿 **Carbon** – eco-friendly route  
- ⏱️ **Time** – fastest shipping option  

Each route includes a breakdown across all four factors for deeper comparison.

---

## 🤖 AI Chatbot
<img width="620" height="250" alt="image" src="https://github.com/user-attachments/assets/8c4d19e7-ce33-419b-b960-98e8be3264d0" />

Our chatbot uses a **Retrieval-Augmented Generation (RAG)** system:
1. User sends a query  
2. Pinecone retrieves relevant tariff data  
3. GPT generates a data-grounded, source-backed answer  

This ensures accuracy and reliability — not hallucination.  

<img width="450" height="630" alt="image" src="https://github.com/user-attachments/assets/c1b91c86-e918-4ec3-989a-4213dac912c9" />

Admins can upload or update tariff documents directly in Pinecone.

---

## 📊 Results Tab
Quickly build new routes, compare tariffs, and export results as a PDF report.

---

## 🧭 Multi-Leg Route Builder
<img width="460" height="340" alt="image" src="https://github.com/user-attachments/assets/5022f312-e24d-486f-b701-8c9741d8bbbc" />

Combine multiple legs across various countries to optimize cost and efficiency.  
Compare direct vs. multi-leg routes seamlessly.

---

## ⚖️ Tariff Comparator
<img width="600" height="225" alt="image" src="https://github.com/user-attachments/assets/2b5c1a3a-c13e-4b7c-aa15-c61be1e0bb38" />

Compare tariffs based on:
- Risk  
- Cost  
- Carbon emissions  
- Time  

With additional analytics: <br>
<img width="380" height="50" alt="image" src="https://github.com/user-attachments/assets/23fe14a4-9315-4119-873e-dc0221e57612" />

---

## 🔒 Security
<img width="450" height="315" alt="image" src="https://github.com/user-attachments/assets/3a191a7b-7f74-4539-8d99-14e1ac63aa24" />

Secured via **AWS Cognito** with strict least-privilege access control.

| User Type | Permissions |
|------------|--------------|
| **User** | Access tariff calculations, results tab, chatbot |
| **Admin** | All user features + Tariff Management and Chatbot Data Editing |

---

## ⚙️ Tariff Management (Admin)
Admin-only page for:
- ➕ **Add/Delete Tariff** – stay up-to-date with changing regulations  
- ✏️ **Edit Tariff** – correct mistakes without database rebuilds  
- 📚 **Update Chatbot Context** – manage compliance documents used in RAG system  

---

## 🛠️ Technical Implementation
- **Frontend:** React + TypeScript (Mapbox for visualization)  
- **Backend:** Java (Spring Boot), Python  
- **Databases:** PostgreSQL, Neo4J, Pinecone, Redis  
- **Deployment:** Vercel (Frontend), AWS EC2 (Backend)

---

## 🔁 CI/CD Pipeline Overview
<img width="360" height="280" alt="ci-cd" src="https://github.com/user-attachments/assets/b0477b26-a7e1-4999-8b7e-099de8696e77" />

Automated via **GitHub Actions**:
1. Runs full test suite  
2. Deploys backend to AWS EC2  
3. Deploys frontend to Vercel  

Ensures production stays synchronized with every `main` branch push.

---

## 🧮 Database & Pipelines
<img width="850" height="500" alt="er-diagram" src="https://github.com/user-attachments/assets/e7c5e289-4af3-4764-9896-103ec2761a38" />  
<img width="420" height="230" alt="pipeline" src="https://github.com/user-attachments/assets/3e320c54-2bd6-41b5-80fa-45ebf77b347b" />

Automated background pipelines:
- 🗓️ **Monthly:** Tariff data ingestion  
- ⏱️ **Hourly:** Exchange rate updates  

Powered by **AWS Lambda**, **Redis**, and **DynamoDB**.

---

## 🚀 Running the Project Locally

### Backend Setup
```bash
mvn install
mvn spring-boot:run
```
### Frontend Setup
```bash
cd frontend
npm install
npm run dev
```
### Chatbot Setup
```bash
cd "Python RAG chatbot"
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn src.api_server:app --reload --host 0.0.0.0 --port 8000
```
### Compliance Checklist Setup
```bash
cd Compliance_checklist
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn src.api_server:app --reload --host 0.0.0.0 --port 8001
```
🧪 Running Tests
## Windows

```bash
run-tests.bat
run-tests.bat --coverage
run-tests.bat --controller
```
## Mac/Linux
```bash
./run-tests.sh
./run-tests.sh --coverage
./run-tests.sh --controller
```
## Manual
```bash
mvn clean test
mvn test -Dtest=TariffManagementControllerIntegrationTest
mvn jacoco:report
```

## 🌐 Deployed Servers
Service	URL <br>
Frontend	https://trade-optimisation-pathfinder.vercel.app <br>
Backend	https://cs203tariffproject.duckdns.org <br>
Chatbot	https://cs203chatbot.duckdns.org <br>
Compliance Checklist	https://cs203compliance.duckdns.org <br>

Note: If you encounter a NetworkError while on SMU Wi-Fi, switch to another network (DNS blocking issue).

## 🧱 Backend Testing Requirements
Ensure Docker Desktop is running before executing tests:

```bash
docker ps
mvn test
```
If Docker is not running, integration tests will fail.
## Team
---
This project was built by
- [Khoo Kar Xing](https://github.com/itsnotkx)
- [Ethan Lim Jin](https://github.com/TheJinius)
- [Neo Jia Wen](https://github.com/N-Jia-Wen)
- [Seow Wee Siang](https://github.com/wonnom)
- [Alister Chong](https://github.com/OborosYX)
- [Muhammad Hayyun](https://github.com/muhdhayyun)

---
