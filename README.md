# CS203-Project

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

### Chatbot
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
