variable "aws_region" {
  type        = string
  description = "AWS region to deploy into."
  default = "ap-southeast-1"
}

# Optional tags for all resources
variable "common_tags" {
  type        = map(string)
  description = "Tags applied to all resources."
  default     = {
    "project":"cs203"
  }
}

variable "environment" {
  description = "Environment name (dev, stg, prod)"
  type = string
  default = "dev"
}

