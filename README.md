# Trade Optimization Pathfinder (TOP)

*An all in one Tariff Management Platform to streamline the process of calculating Tariffs and optimizing the trade routes between 2 or more countries*

## 🌍 Problem Statement
Design and implement a system called TARIFF (Trade Agreements Regulating Imports and Foreign Fees): its purpose is to define import tariffs (and/or additional fees) and to calculate them against a certain industry or product between different countries at any given times.

## 💡 Our Solution
**Trade Optimization Pathfinder (TOP)** is the perfect solution to tackle Trade Route Tariff Calculations and Management through our all in one optimizer platform that:
* Provides clear **Trade Route Tariff Calculations**.
* Enables admins to **manage Tariffs**.
* Gives **route visualization** a bird’s-eye view for smarter coordination.

## 🎯 Features

### Baseline Tariff Calculation Process
<img width="1089" height="1293" alt="image" src="https://github.com/user-attachments/assets/3a2636c5-0ebe-4432-859d-668e9f5abbe0" />
* Step 1: Enter source and destination countries to search for goods using HSCode or descriptions
<img width="1174" height="1240" alt="image" src="https://github.com/user-attachments/assets/5a4de1f6-6702-415f-a030-c35f7dbdfe7e" />
* Step 2: System returns the tariff(s) available and user selects from the dropdown and enters quantity of product
* Step 3: Select desired currency conversion and calculate the result

## Exchange Rate Conversion
Exchange rates are from ExchangeRateAPI, pulled every hour and stored in Redis, ensuring sped and scaleability without sacrificing accuracy in exchange rate conversions.

## Explainable Calculation
TOP breaks down calculation into step by step formula and displaying each step
This not only gives users confidence about the correctness of the final tariff calculation but also help beginners learn about different tariffs and how they are calculated

## Compliance Checklist
<img width="969" height="791" alt="image" src="https://github.com/user-attachments/assets/b454477a-000a-4f65-84b5-25b78d1360fb" />
Offers users a list of checklist items to look out for after they perform a calculation. This includes items like Compliance declarations and Certification and classification requirements.
Uses Pinecone and OpenAI for semantic search, with strict JSON output for frontend checklist visualisation.
Checklist items are sourced from official reports of government websites and offer a direct source link from our app for users to verify the authenticity of the checklist item. 

## Route Visualization
<img width="1134" height="380" alt="image" src="https://github.com/user-attachments/assets/edd0eb3e-d2e7-4df7-a15d-7bd8c49e0870" />
Gives 4 distinct routes: Each optimized for a certain aspect:

*Risk: Risk of piracy/incidents occuring
*Cost: Cheapest way to ship objects
*Carbon Emissions: Most environmentally friendly route
*Time: Fastest way to transport goods

Each route also gives a breakdown of all 4 factors for more in depth comparisons

### AI Chatbot
<img width="1141" height="456" alt="image" src="https://github.com/user-attachments/assets/8c4d19e7-ce33-419b-b960-98e8be3264d0" />
Our chatbot uses a RAG system, when a user asks a question, the system doesn’t just generate an answer from the language model alone. Instead, it first checks our Pinecone vector database that stores reliable tariff data.

Pinecone then returns the most relevant context back to GPT - for example, specific tariff definitions, regulatory notes, or documentation references.

Using these together, GPT generates a final answer that is grounded in real data, rather than guessing. This ensures the chatbot’s responses are accurate, reliable, and source-backed.

<img width="869" height="1221" alt="image" src="https://github.com/user-attachments/assets/c1b91c86-e918-4ec3-989a-4213dac912c9" />
Administrators can add new documents or update existing tariff information directly into Pinecone.

### Results Tab
This tab allows users to quickly build new routes, compare Tariffs and export results as a PDF

## Multi-Leg Route Builder
<img width="827" height="609" alt="image" src="https://github.com/user-attachments/assets/5022f312-e24d-486f-b701-8c9741d8bbbc" />

Users have the ability to combine multiple routes through multiple countries together for further cost optimization, to allow users to compare metrics for direct shipping versus multi-leg shipping routes as well.

## Tariff comparator
<img width="1077" height="403" alt="image" src="https://github.com/user-attachments/assets/2b5c1a3a-c13e-4b7c-aa15-c61be1e0bb38" />
Users are able to compare different tariffs between two different trade routes, and gives a breakdown on which is more optimal based on one of the 4 metrics:
*Risk: Risk of piracy/incidents occuring
*Cost: Cheapest way to ship objects
*Carbon Emissions: Most environmentally friendly route
*Time: Fastest way to transport goods
We also provide some additional analytics as shown:
<img width="761" height="97" alt="image" src="https://github.com/user-attachments/assets/23fe14a4-9315-4119-873e-dc0221e57612" />


### Security
<img width="904" height="630" alt="image" src="https://github.com/user-attachments/assets/3a191a7b-7f74-4539-8d99-14e1ac63aa24" />
Secured by AWS cognito, ensuring compliance with strict regulations and auditability as required by banks.
Designed with strict least privilege access principles, with 2 distinct user groups: Users and Admins.
## Users: 
Allowed to use the Tariff Calculation Features, Results tab and AI Chatbot
## Admins: 
Are allowed to use anything the Users have accessed to, as well as Tariff Management page

## Tariff Management
A page that only Admins are able to access
It has the following functionalities:
*Add Tariff/Delete Tariff: 
Due to frequently changing regulations, admins have the authority to update the system to stay compliant even outside of the regularly updated intervals.

*Edit Tariff: 
Any mistakes in tariff entries can be rectified without having to recreate the entire database and without developer intervention

*Adding Information to Chatbot:
Able to modify the compliance documents that is used as context for the AI chatbot



## 🛠️ Technical Implementation

* **Frontend:** React, TypeScript (map visualisation via Mapbox).
* **Backend:** Java (Spring Boot), Python.
* **Databases:** PostgreSQL, Neo4J, Pinecone, Redis.
* **Deployment:** Vercel (Frontend), AWS EC2 (Backend).


# To Run Locally

## 1. Project Setup Instructions

Welcome to our CS203 tariff project! Follow these steps to set up and run the application:

### Backend Setup

1. Open a terminal in the root project directory (`CS203-Project`).
2. Run the following command to install backend dependencies:
	```
	mvn install
	```

### Frontend Setup

1. Navigate to the `frontend` directory:
	```
	cd frontend
	```
2. Install frontend dependencies:
	```
	npm install
	```

### Chatbot Setup

1. Using a Powershell Terminal, navigate to the `Python RAG chatbot` directory:
    ```
	cd "Python RAG chatbot"
	```

2. Create the virtual environment, if it does not yet exist:
    ```
	python -m venv .venv
	```

3. In PowerShell (and NOT the Command Prompt), run this command. You should see `(.venv)` in the PowerShell prompt:
    ```
    .\.venv\Scripts\Activate.ps1
    ```

4. If in step 3, you encounter an UnauthorizedAccess SecurityError, it means PowerShell is blocking script execution for security purposes. Allow local scripts to be run by executing this command when running PowerShell as an administrator:
    ```
    Set-ExecutionPolicy -ExecutionPolicy RemoteSigned
    ```

5. Install the required dependencies into the virtual environment, like so:
    ```
    pip install -r requirements.txt
    ```

### Compliance Checklist Setup

1. Using a Powershell Terminal, navigate to the `Compliance_checklist` directory:
    ```
	cd Compliance_checklist
	```

2. Create the virtual environment, if it does not yet exist:
    ```
	python -m venv .venv
	```

3. In PowerShell (and NOT the Command Prompt), run this command. You should see `(.venv)` in the PowerShell prompt:
    ```
    .\.venv\Scripts\Activate.ps1
    ```

4. If in step 3, you encounter an UnauthorizedAccess SecurityError, it means PowerShell is blocking script execution for security purposes. Allow local scripts to be run by executing this command when running PowerShell as an administrator:
    ```
    Set-ExecutionPolicy -ExecutionPolicy RemoteSigned
    ```

5. Install the required dependencies into the virtual environment, like so:
    ```
    pip install -r requirements.txt
    ```

## 2. Running Tests

We use **Testcontainers** for integration testing with PostgreSQL. Docker is required.

### Quick Start (Windows)
```bash
run-tests.bat                    # Run all tests
run-tests.bat --coverage         # Run tests with coverage report
run-tests.bat --controller       # Run controller tests only
```

### Quick Start (Mac/Linux)
```bash
./run-tests.sh                   # Run all tests
./run-tests.sh --coverage        # Run tests with coverage report
./run-tests.sh --controller      # Run controller tests only
```

### Manual Tests
```bash
# Run all tests
mvn clean test

# Run specific test class
mvn test -Dtest=TariffManagementControllerIntegrationTest

# Generate coverage report
mvn jacoco:report
```

For detailed testing documentation, see [TEST_SETUP_GUIDE.md](TEST_SETUP_GUIDE.md).

## 3. CI/CD Pipeline

All tests run automatically on:
- Push to `main`, `develop`, or `backend` branches
- Pull requests to `main`

View results in the **Actions** tab on GitHub.

## 4. Running the Application

### Backend
From the root project directory, start the backend server:
```
mvn spring-boot:run
```

### Frontend
From the `frontend` directory, start the frontend development server:
```
npm run dev
```

### AI Chatbot
From the `Python RAG chatbot` directory, start the chatbot:
```
uvicorn src.api_server:app --reload --host 0.0.0.0 --port 8000
```

### Compliance Checklist
From the `Compliance_checklist` directory, start the server:
```
uvicorn src.api_server:app --reload --host 0.0.0.0 --port 8001
```

The backend will be available on [http://localhost:8080](http://localhost:8080), and the frontend will run on [http://localhost:3000](http://localhost:3000). The chatbot will be running on [http://localhost:8000](http://localhost:8000), and the compliance checklist server will be hosted at [http://localhost:8001](http://localhost:8001). The chatbot and compliance checklist can also be interacted with from the frontend UI.

### Deployed Servers
Alteratively, you can access the tariff calculation UI from the frontend. The frontend is deployed at [https://trade-optimisation-pathfinder.vercel.app/](https://trade-optimisation-pathfinder.vercel.app/), and the backend is deployed at [https://cs203tariffproject.duckdns.org](https://cs203tariffproject.duckdns.org).

Additionally, the chatbot is deployed at [https://cs203chatbot.duckdns.org](https://cs203chatbot.duckdns.org), while the compliance checklist is deployed at [https://cs203compliance.duckdns.org](https://cs203compliance.duckdns.org).

If you encounter a `NetworkError` while connected to the SMU Wi-Fi, do connect to a different Wi-Fi source and try again (the DNS server is blocked by the school Wi-Fi).

## 5. Running Backend Tests

To run backend tests, make sure Docker Desktop is running on your machine. Some tests require Docker containers for database or service emulation.

1. Start Docker Desktop (see [Docker Desktop install guide](https://docs.docker.com/desktop/)).
2. Verify Docker is running by typing the following command in your terminal:
	```
	docker ps
	```
	If Docker is running, you will see a list of active containers (or an empty list if none are running). If you get an error, Docker is not running.
2. In the root project directory, run:
	```
	mvn test
	```
If Docker is not running, tests may fail to start the required containers.
