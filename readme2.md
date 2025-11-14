CI/CD Pipeline Overview
<img width="563" height="438" alt="Screenshot 2025-11-14 at 12 09 54 PM" src="https://github.com/user-attachments/assets/a492c92f-14df-4f63-883c-5b9a4a71a4a0" />
Github actions is used for our deployments. Whenever code is pushed to the main repository, github actions will automatically run our test suite to ensure that our code changes do not fundamentally affect the core business logic. After, it sequentially deploys each service into their respective EC2 instances, and lastly, the frontend is deployed. This way, we ensure that all live deployments are updated on code push.

Database design
<img width="1452" height="850" alt="cs203-er-diagram" src="https://github.com/user-attachments/assets/151e365b-8e10-4cb7-a42c-cb50c14b93be" />

Other Pipelines
<img width="648" height="344" alt="Screenshot 2025-11-14 at 12 19 50 PM" src="https://github.com/user-attachments/assets/1caf86be-2d0a-4c11-9b60-cfd3772e7e19" />
The data ingestion pipeline and the exchange rate pipelines run in the background without human intervention. These pipelines are rigged to run monthly and hourly respectively. The ingestion pipeline is hosted together with the calculation logic, whilst the exchange rate is hosted using AWS Lambda, Redis, and DynamoDB
