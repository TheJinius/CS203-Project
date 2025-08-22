# DynamoDB: users
resource "aws_dynamodb_table" "users" {
  name         = "users"
  billing_mode = "PAY_PER_REQUEST"

  hash_key  = "user_id"
  range_key = "type"

  attribute {
    name = "user_id"
    type = "S"
  }

  attribute {
    name = "type"
    type = "S"
  }

  tags = var.table_tags
}

# DynamoDB: tariffs
resource "aws_dynamodb_table" "tariffs" {
  name         = "tariffs"
  billing_mode = "PAY_PER_REQUEST"

  hash_key  = "origin"
  range_key = "effectiveFrom"

  attribute {
    name = "origin"
    type = "S"
  }

  attribute {
    name = "effectiveFrom"
    type = "S"
  }

  tags = var.table_tags
}
