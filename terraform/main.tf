module "dynamodb" {
  source = "./modules/dynamodb"

  table_tags = var.common_tags
}