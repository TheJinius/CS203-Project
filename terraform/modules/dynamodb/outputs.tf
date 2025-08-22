output "users_table_name" {
  value = aws_dynamodb_table.users.name
}

output "tariffs_table_name" {
  value = aws_dynamodb_table.tariffs.name
}