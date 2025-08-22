variable "table_tags" {
  type        = map(string)
  description = "Tags to apply to the DynamoDB tables."
  default     = {
    "project":"cs203"
  }
}