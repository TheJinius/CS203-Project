# CS203-Project


## Project Setup Instructions

Welcome to our CS203 tariff project! Follow these steps to set up and run the application:

### 1. Backend Setup

1. Open a terminal in the root project directory (`CS203-Project`).
2. Run the following command to install backend dependencies:
	```
	mvn install
	```

### 2. Frontend Setup

1. Navigate to the `frontend` directory:
	```
	cd frontend
	```
2. Install frontend dependencies:
	```
	npm install
	```

## 3. Chatbot Setup

1. Navigate to the `Python RAG chatbot` directory:
    ```
	cd "Python RAG chatbot`
	```

2. Create the virtual environment, if it does not yet exist:
    ```
	python -m venv .venv
	```

3. In PowerShell (and NOT the Command Prompt), run this command. You should see `( venv) in the PowerShell prompt:
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

6. 

### 3. Running the Application

#### Backend
From the root project directory, start the backend server:
```
mvn spring-boot:run
```

#### Frontend
From the `frontend` directory, start the frontend development server:
```
npm run dev
```

#### Chatbot
From the `Python RAG chatbot` directory, start the chatbot:
```
uvicorn src.api_server:app --reload --host 0.0.0.0 --port 8000
```

The backend will be available on [http://localhost:8080](http://localhost:8080), and the frontend will run on [http://localhost:3000](http://localhost:3000). The chatbot can be interacted with from the frontend UI.

### 4. Running Backend Tests

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
