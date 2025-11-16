# IMPORTANT: ON RUNNING THE GRAPHDB SERVICE LOCALLY
To run the graphdb service locally, the following software is required:
- [Neo4j desktop](https://neo4j.com/download/neo4j-desktop/?edition=desktop&flavour=winstall64&release=2.0.5&offline=false)

---
# Additional information:
For ease of use, there is a deployed cloud instance of the database hosted on Neo4j. The provided .env file already points to the cloud database, hence no action is needed. However, it is on a free trial, and will expire on 21st November 2025. The instructions below guide the user on how to run the database locally.
---

# Steps to run the database locally:
1. Install Neo4j desktop. Follow the installation instructions.
2. Create a new database instance. Note down the Database user and the password. Replace the values of the `NEO4J_USER` and `NEO4J_PASSWORD` environment variables in `graphdb/.env` are respectively updated to these values.
3. Enable the Graph Data Science (GDS) and APOC plugins by clicking on menu (3 dots) -> Plugins -> Install Graph Data Science and APOC plugins
4. Delete the pre-existing generated`neo4j` database, if any. Then, stop the database instance, and load the .dump file by clicking "Load database from file" and selecting the neo4j.dump file provided at `graphdb/neo4j.dump`.
5. Start the instance, if it did not automatically start running in step 4. It will be running on localhost, port 7687. You should also see the connection URI (e.g. [neo4j://127.0.0.1:7687](neo4j://127.0.0.1:7687) - check the exact URI you are provided). Delete the ```NEO4J_URI``` variable in the provided .env file at `graphdb/.env` and replace it with this value.

6. Start the main function:
```
cd graphdb
python -m venv .venv
.venv\scripts\activate
pip install -r requirements.txt
py main.py
```
The app will run on [http://0.0.0.0:8002](http://0.0.0.0:8002).
---


