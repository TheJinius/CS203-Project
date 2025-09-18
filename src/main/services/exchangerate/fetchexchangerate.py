import requests
import os
import boto3
import redis
from datetime import datetime, timezone
from decimal import Decimal
import logging
import json

EXCHANGE_RATE_URL = os.getenv("EXCHANGE_RATE_URL")
EXCHANGE_RATE_API_KEY = os.getenv("EXCHANGE_RATE_API_KEY")
EXCHANGE_RATE_BASE_CURRENCY = os.getenv("EXCHANGE_RATE_BASE_CURRENCY")
DYNAMODB_TABLE = os.getenv("DYNAMODB_TABLE")
REDIS_HOST = os.getenv("REDIS_HOST")
REDIS_PORT = os.getenv("REDIS_PORT")
REDIS_TTL_SECONDS = int(os.getenv("REDIS_TTL_SECONDS", "3600"))
REDIS_PASSWORD = os.getenv('REDIS_PASSWORD')
REDIS_USERNAME = os.getenv('REDIS_USERNAME')

logger = logging.getLogger()
logger.setLevel(logging.INFO)

dynamodb = boto3.resource("dynamodb")
table = dynamodb.Table(DYNAMODB_TABLE)  # Fix: changed from table = dynamodb("exchange-rate-audit")

def handler(event, context):
    
    try:
        req = f"{EXCHANGE_RATE_URL}/{EXCHANGE_RATE_API_KEY}/latest/{EXCHANGE_RATE_BASE_CURRENCY}"
        response = requests.get(req)
        data = response.json()
        rates = data["conversion_rates"]
    except Exception as e:
        logger.exception("failed to fetch exchange rates: %s", e)
        raise
        
    try:
        store_redis(rates)
    except Exception as e:
        logger.exception("failed to store in redis: %s", e)
        raise
    
    try:
        store_dynamodb(rates)
    except Exception as e:
        logger.exception("failed to store in dynamodb: %s", e)
        raise
    
    logger.info("successfully stored in redis and dynamodb")
    return {
        "statusCode":200,
        "body": json.dumps({"message": "OK", "stored_at": datetime.now(timezone.utc).isoformat()})
    }
        
def store_redis(rates):
    client = None
    try:
        client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, socket_timeout=5, username= REDIS_USERNAME, password = REDIS_PASSWORD)
        key = f"exchange_rates:{EXCHANGE_RATE_BASE_CURRENCY}"
        payload = json.dumps(rates)
        logger.info("Setting Redis key %s (TTL %s)", key, REDIS_TTL_SECONDS)
        client.set(key, payload, ex=REDIS_TTL_SECONDS)
    except redis.RedisError as e:
        logger.error("Redis operation failed: %s", e)
        raise
    except Exception as e:
        logger.error("An error occured with storing in redis: %s", e)
        raise
    finally:
        if client:
            client.close()
    
def store_dynamodb(rates):
    #store response in dynamodb
    now_iso = datetime.now(timezone.utc).isoformat()
    item = {
        "id":now_iso,
        "base":EXCHANGE_RATE_BASE_CURRENCY,
        "timestamp":now_iso,
        "rates":convert_floats(rates)
    }
    
    logger.info("storing item in dynamodb")
    table.put_item(Item = item)

def convert_floats(obj):
    if isinstance(obj, float):
        return Decimal(str(obj))
    if isinstance(obj, dict):
        return {k: convert_floats(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [convert_floats(v) for v in obj]