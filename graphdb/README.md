# IMPORTANT: ON RUNNING THE GRAPHDB SERVICE LOCALLY
To run the graphdb service locally, the following software is required:
- [Neo4j desktop](https://neo4j.com/download/neo4j-desktop/?edition=desktop&flavour=winstall64&release=2.0.5&offline=false)

---
# Additional information:
For ease of use, there is a deployed cloud instance of the database hosted on Neo4j. The provided .env file already points to the cloud database, hence no action is needed. However, it is on a free trial, and will expire on 21st November 2025. The instructions below guide the user on how to run the database locally.
---

# Steps to run the database:
1. Install Neo4j desktop. Follow the installation instructions.
2. Create a new database instance. Note down the Database user and the Password.
3. Enable the Graph Data Science(GDS) and APOC plugins by clicking on menu(3 dots) -> Plugins -> Install Graph Data Science and APOC plugins
4. Load the .dump file by clicking "Load database from file" and selecting the neo4j.dump file provided in the repository.
5. Start the instance. It will be running on localhost, port 7687. If running locally, delete the ```NEO4J_URI``` variable in the provided .env file. Do make sure that the ```NEO4J_USER``` and ```NEO4J_PASSWORD``` variables are updated too.
6. Start the main function:
```
cd /graphdb
python -m venv .venv
.venv\scripts\activate
pip install -r requirements.txt
py main.py
```
The app will run on port 8002.
---


